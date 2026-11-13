import assert from 'node:assert/strict';
import { chromium } from '@playwright/test';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const root=fileURLToPath(new URL('../../',import.meta.url));
const env=Object.fromEntries((await readFile(path.join(root,'agente-nexo/.local/dev.env'),'utf8')).trim().split('\n').map(x=>{const i=x.indexOf('=');return [x.slice(0,i),x.slice(i+1)];}));
const out=path.join(root,'documentacion/nexo-ai/evidencia/visual-viewport');await mkdir(out,{recursive:true});
const browser=await chromium.launch({channel:'chrome',headless:true});
try {
 const page=await browser.newPage({viewport:{width:1440,height:1000}});
 await page.goto('http://localhost:8090/#login');await page.locator('#login input[name=email]').fill('demo-admin@example.test');await page.locator('#login input[name=password]').fill(env.DEMO_PASSWORD);await page.locator('#login button').click();await page.locator('#view-account').waitFor();await page.locator('nav a[href="#nexo"]').click();await page.locator('#nexo-chats button').first().waitFor();
 await page.screenshot({path:path.join(out,'escritorio.png')});
 await page.locator('#nexo-chats button').first().click();await page.locator('.nexo-bubble').first().waitFor();
 await page.setViewportSize({width:390,height:844});await page.evaluate(()=>scrollTo(0,0));await page.screenshot({path:path.join(out,'movil-inicio.png')});
 await page.locator('.nexo-bubble').first().evaluate(el=>el.scrollIntoView({block:'start'}));await page.screenshot({path:path.join(out,'movil-conversacion.png')});
 await page.locator('.nexo-lab').evaluate(el=>el.scrollIntoView({block:'start'}));await page.screenshot({path:path.join(out,'movil-laboratorio.png')});
 const check=await page.evaluate(()=>({viewport:innerWidth,document:document.documentElement.scrollWidth,elements:[...document.querySelectorAll('.nexo-layout,.nexo-sidebar,.nexo-workspace,#nexo-chats,.nexo-bubble,.nexo-compose-tools')].slice(0,8).map(e=>({name:e.className||e.id,width:e.getBoundingClientRect().width,scroll:e.scrollWidth}))}));
 assert.ok(check.elements.filter(e=>e.name!=='nexo-chats').every(e=>e.width<=check.viewport && e.scroll<=Math.ceil(e.width)+1), 'Panel interno recortado');
 await writeFile(path.join(out,'capturas.json'),JSON.stringify({real_browser:true,full_page:false,...check,note:'Capturas por viewport: la captura fullPage de Chrome anterior repetía franjas superiores; no se usa como evidencia visual móvil final.'},null,2));
 console.log(JSON.stringify(check));
} finally {await browser.close();}
