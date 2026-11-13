/** Actual Chrome + Java/MySQL + local Ollama. Only the dedicated synthetic dev stack. */
import { chromium } from '@playwright/test';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import assert from 'node:assert/strict';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root=fileURLToPath(new URL('../../',import.meta.url));
const env=Object.fromEntries((await readFile(path.join(root,'agente-nexo/.local/dev.env'),'utf8')).trim().split('\n').map(x=>{const i=x.indexOf('=');return [x.slice(0,i),x.slice(i+1)];}));
const dir=path.join(root,'documentacion/nexo-ai/evidencia',`live-${new Date().toISOString().replaceAll(':','-')}`);await mkdir(dir,{recursive:true});
const report={started_at:new Date().toISOString(),synthetic_only:true,provider:'Ollama real',model:'qwen2.5-coder:7b',turns:[],checks:[],errors:[],passed:false};
const browser=await chromium.launch({channel:'chrome',headless:true});
const context=await browser.newContext({viewport:{width:1440,height:1100}});const page=await context.newPage();
page.on('pageerror',e=>report.errors.push(e.message));
function iamDigest(){
 const id=execFileSync('docker',['compose','--env-file',path.join(root,'agente-nexo/.local/dev.env'),'-f',path.join(root,'deploy/nexo-local.yml'),'ps','-q','mysql'],{encoding:'utf8'}).trim();
 const sql='SELECT HEX(id),session_version,status,password_hash FROM app_user ORDER BY id;SELECT HEX(user_id),HEX(role_id) FROM user_role ORDER BY user_id,role_id;SELECT HEX(role_id),HEX(permission_id) FROM role_permission ORDER BY role_id,permission_id;SELECT HEX(id),token_hash,expires_at,consumed_at FROM account_token ORDER BY id;';
 const raw=execFileSync('docker',['exec',id,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot -N -B nexo_local -e "$1"','sh',sql],{maxBuffer:1000000});
 return createHash('sha256').update(raw).digest('hex');
}
async function ask(text,tool='general'){
 await page.locator('#nexo-tool').selectOption(tool);await page.locator('#nexo-text').fill(text);
 const events=[];const started=Date.now();
 const response=page.waitForResponse(r=>r.url().endsWith('/messages')&&r.request().method()==='POST',{timeout:240000});
 await page.locator('#nexo-send').click();const r=await response;const body=await r.text();
 for(const line of body.trim().split('\n'))events.push(JSON.parse(line));
 assert.ok(events.some(e=>e.type==='done'),body.slice(-600));
 const textOut=events.filter(e=>e.type==='delta').map(e=>e.text).join('');assert.ok(textOut.length>20);
 const item={prompt:text,tool,latency_ms:Date.now()-started,deltas:events.filter(e=>e.type==='delta').length,answer:textOut,metadata:events.find(e=>e.type==='done').metadata};report.turns.push(item);
 await page.locator('#nexo-send').waitFor({state:'visible'});await page.waitForFunction(()=>!document.querySelector('#nexo-send').disabled);
 return item;
}
try{
 await page.goto('http://localhost:8090/#login');await page.locator('#login input[name=email]').fill('demo-admin@example.test');await page.locator('#login input[name=password]').fill(env.DEMO_PASSWORD);await page.locator('#login button').click();await page.locator('#view-account').waitFor({state:'visible'});
 await page.locator('nav a[href="#nexo"]').click();await page.waitForFunction(()=>document.querySelector('#nexo-model').textContent.includes('Conectado'),{timeout:15000});report.checks.push('Ollama disponible a través del backend autenticado');
 const before=iamDigest();
 await ask('La palabra de nuestro ejercicio es albahaca. Explícame en dos frases qué es una base de datos relacional.');
 const memory=await ask('¿Cuál es la palabra del ejercicio que te dije en mi mensaje anterior? Responde con esa palabra.');assert.match(memory.answer,/albahaca/i);report.checks.push('Memoria real de dos turnos');
 const rag=await ask('Explica los roles y la autenticación de ExamenErik. Cita documentos autorizados.','auto');assert.ok(rag.metadata.references.some(r=>r.id==='identidad'));assert.equal(rag.metadata.tool,'search_docs');report.checks.push('Plan de herramienta generado por modelo y RAG con fuentes');
 await page.locator('#nexo-simulate').click();await page.waitForFunction(()=>document.querySelector('#nexo-report').textContent.includes('60/100'));report.checks.push('Simulación sintética 60 y alternativa 0');
 await page.locator('#nexo-source').selectOption('live');await page.locator('#nexo-role').selectOption('USER');await page.locator('#nexo-minute').fill('0');
 await page.locator('#nexo-simulate').click();await page.waitForFunction(()=>document.querySelector('#nexo-report').textContent.includes('Lectura autorizada'));
 assert.equal(iamDigest(),before);report.checks.push('Snapshot IAM idéntico antes/después: cuentas, roles, permisos y tokens');report.iam_unchanged=true;
 const aiResponse=page.waitForResponse(r=>r.url().endsWith('/messages'),{timeout:240000});await page.locator('#nexo-explain').click();const explanation=await (await aiResponse).text();assert.ok(explanation.includes('"type":"done"'));report.simulation_explanation=explanation.trim().split('\n').map(x=>JSON.parse(x));report.checks.push('Explicación de simulación con inferencia real');
 await page.waitForFunction(()=>!document.querySelector('#nexo-send').disabled);
 await page.screenshot({path:path.join(dir,'nexo-desktop.png'),fullPage:true});
 const download=page.waitForEvent('download');await page.locator('#nexo-export').click();await (await download).saveAs(path.join(dir,'informe-descargado.json'));report.checks.push('Exportación de conversación e informe');
 await page.reload();await page.locator('#view-nexo').waitFor({state:'visible'});await page.locator('#nexo-chats button').first().click();await page.waitForFunction(()=>document.querySelectorAll('.nexo-bubble').length>=6);report.checks.push('Historial restaurado tras refresh real');
 await page.setViewportSize({width:390,height:844});await page.screenshot({path:path.join(dir,'nexo-mobile.png'),fullPage:true});
 assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth && document.querySelector('.nexo-layout').scrollWidth<=Math.ceil(document.querySelector('.nexo-layout').getBoundingClientRect().width)+1));report.checks.push('Vista móvil sin desbordamiento horizontal');
 assert.deepEqual(report.errors,[]);report.passed=true;
}finally{report.finished_at=new Date().toISOString();await writeFile(path.join(dir,'resultado.json'),JSON.stringify(report,null,2));await browser.close();console.log(JSON.stringify({directory:dir,passed:report.passed,turns:report.turns.length,checks:report.checks}));}
