const { chromium }=require('../frontend/node_modules/playwright');
const fs=require('node:fs');
(async()=>{
 const base='http://localhost:8091';
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try {
  const page=await browser.newPage();
  await page.goto(base+'/#login');
  await page.locator('#login [name=email]').fill('audit-admin@example.test');
  await page.locator('#login [name=password]').fill('AuditOnly-19!Test');
  await page.locator('#login button').click();
  await page.locator('#view-account').waitFor();
  await page.locator('a[href="#nexo"]').click();
  await page.locator('#nexo-tool').selectOption('general');
  await page.locator('#nexo-text').fill('Escribe una lista de 50 animales con una descripción de tres frases por animal. Comienza con el elefante.');
  const response=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/messages'));
  await page.locator('#nexo-send').click();
  const reply=await response;
  const events=(await reply.text()).trim().split('\n').map(JSON.parse);
  const progress=await page.locator('#nexo-progress').textContent();
  const visible=await page.locator('.nexo-bubble.assistant .nexo-body').last().textContent();
  const result={types:[...new Set(events.map(e=>e.type))],progress,visible,chat:new URL(reply.url()).pathname.split('/').at(-2)};
  fs.writeFileSync(__dirname+'/truncamiento.json',JSON.stringify(result,null,2));
  await page.screenshot({path:__dirname+'/nexo-truncado.png'});
  if(!result.types.includes('truncated')||result.types.includes('done')||!visible.includes('Respuesta truncada')) process.exitCode=1;
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
