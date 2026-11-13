// Prueba local de despliegue separado: dos orígenes HTTPS, API y MySQL sintéticos.
import { createServer } from 'node:https';
import http from 'node:http';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium, expect } from '@playwright/test';
import setup from './e2e/environment.js';
const root = fileURLToPath(new URL('../../', import.meta.url));
const scratch = mkdtempSync(path.join(tmpdir(), 'examenerik-tls-'));
const servers = []; let cleanup, browser;
const listen = async handler => {
  const server = createServer({key:readFileSync(path.join(scratch,'key.pem')),cert:readFileSync(path.join(scratch,'cert.pem'))}, handler);
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve)); servers.push(server);return server.address().port;
};
try {
  execFileSync('openssl',['req','-x509','-newkey','rsa:2048','-nodes','-days','1','-subj','/CN=examenerik.test','-addext','subjectAltName=DNS:app.examenerik.test,DNS:api.examenerik.test','-keyout',path.join(scratch,'key.pem'),'-out',path.join(scratch,'cert.pem')],{stdio:'ignore'});
  const apiPort = await listen((req,res)=>{
    const upstream=http.request(process.env.E2E_BASE_URL+req.url,{method:req.method,headers:{...req.headers,host:new URL(process.env.E2E_BASE_URL).host}},response=>{
      res.writeHead(response.statusCode,response.headers);response.pipe(res);
    });upstream.on('error',()=>{res.writeHead(502);res.end();});req.pipe(upstream);
  });
  let headers = '';
  const frontPort = await listen((req,res)=>{
    const file = {'/':'index.html','/index.html':'index.html','/reset-password.html':'reset-password.html','/app.js':'app.js','/styles.css':'styles.css'}[req.url.split('?')[0]];
    if(!file){res.writeHead(404);res.end();return;}
    res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');
    res.setHeader('Content-Security-Policy',headers);
    res.end(readFileSync(path.join(root,'frontend/dist',file)));
  });
  const origin=`https://app.examenerik.test:${frontPort}`; const apiOrigin=`https://api.examenerik.test:${apiPort}`;
  process.env.E2E_ADDITIONAL_ORIGIN=origin;process.env.E2E_COOKIE_SECURE='true';
  execFileSync('npm',['--prefix',path.join(root,'frontend'),'run','build'],{env:{...process.env,APP_API_BASE_PATH:apiOrigin+'/api'},stdio:'ignore'});
  headers=readFileSync(path.join(root,'frontend/dist/_headers'),'utf8').split('Content-Security-Policy: ')[1].split('\n')[0];
  cleanup=await setup();
  browser=await chromium.launch({channel:'chrome',args:['--host-resolver-rules=MAP app.examenerik.test 127.0.0.1, MAP api.examenerik.test 127.0.0.1','--no-proxy-server']});
  const context=await browser.newContext({ignoreHTTPSErrors:true});const page=await context.newPage();
  const responses=[];page.on('response',r=>{if(r.url().startsWith(apiOrigin))responses.push({path:new URL(r.url()).pathname,status:r.status()});});
  await page.goto(origin+'/#login');
  await page.locator('#login').getByLabel('Correo',{exact:true}).fill('qa-admin@gmail.com');
  await page.locator('#login').getByLabel('Contraseña',{exact:true}).fill(process.env.E2E_ADMIN_PASSWORD);
  await page.locator('#login').getByRole('button',{name:'Entrar',exact:true}).click();
  await expect(page.locator('#view-account')).toBeVisible();
  const cookies=await context.cookies(apiOrigin+'/api/auth/refresh');
  const refresh=cookies.find(c=>c.name==='refresh_token');
  expect(refresh).toBeTruthy();expect(refresh.httpOnly).toBe(true);expect(refresh.secure).toBe(true);expect(refresh.sameSite).toBe('Strict');
  expect(refresh.domain).toBe('api.examenerik.test');expect(refresh.path).toBe('/api/auth');
  await page.reload();await expect(page.locator('#view-account')).toBeVisible();
  expect(responses.some(r=>r.path==='/api/auth/refresh'&&r.status===200)).toBe(true);
  const forbidden=await page.evaluate(async api=>{const r=await fetch(api+'/api/auth/refresh',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:'{}'});return r.status;},apiOrigin);
  expect(forbidden).toBe(200);
  expect(await page.evaluate(()=>document.cookie.includes('refresh_token'))).toBe(false);
  await page.screenshot({path:path.join(root,'documentacion/actividad/evidencia/origenes-https.png'),fullPage:true});
  const proof={local_synthetic_only:true,tls:'Self-signed certificate accepted ONLY in this test browser', frontend_origin:origin,api_origin:apiOrigin,
    cookie:{httpOnly:refresh.httpOnly,secure:refresh.secure,sameSite:refresh.sameSite,domain:refresh.domain,path:refresh.path},
    reload_refresh_success:true,responses,passed:true};
  writeFileSync(path.join(root,'documentacion/actividad/evidencia/origenes-https.json'),JSON.stringify(proof,null,2));
  console.log('PASS: login, cookie HttpOnly/Secure/Strict y refresh al recargar desde otro origen HTTPS del mismo sitio.');
} finally {
  await browser?.close();for(const server of servers)await new Promise(resolve=>server.close(resolve));
  if(cleanup)await cleanup();rmSync(scratch,{recursive:true,force:true});
  // Build neutro sin direcciones efímeras del laboratorio.
  execFileSync('npm',['--prefix',path.join(root,'frontend'),'run','build'],{env:{...process.env,APP_API_BASE_PATH:'/api'},stdio:'ignore'});
}
