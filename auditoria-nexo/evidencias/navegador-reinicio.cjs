const { chromium }=require('../../frontend/node_modules/playwright');
const fs=require('node:fs');
(async()=>{
 const mode=process.argv[2]; const base='http://localhost:8090';
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try {
 const page=await browser.newPage();
 await page.goto(base+'/#login');
 await page.locator('#login [name=email]').fill('audit-admin@example.test');
 await page.locator('#login [name=password]').fill('AuditOnly-19!Test');
 await page.locator('#login button').click();
 await page.locator('#view-account').waitFor();
 await page.locator('a[href="#nexo"]').click();
 const status=page.locator('#nexo-model');
 await page.waitForFunction(()=>/Conectado|No disponible/.test(document.querySelector('#nexo-model').textContent));
 let result={id:'UI-'+mode,modelo:await status.textContent()};
 if(mode==='reinicio'){
  const items=page.locator('#nexo-chats button');
  await items.first().waitFor();
  let found=false;
  for(let i=0;i<await items.count();i++){
   const reply=page.waitForResponse(r=>r.request().method()==='GET'&&/\/api\/nexo\/conversations\/[a-f0-9-]+$/.test(r.url()));
   await items.nth(i).click();const r=await reply;
   const messages=await r.json();
   if(messages.some(x=>x.content.includes('CIRUELA-761'))){found=true;await page.waitForFunction(()=>document.querySelector('#nexo-messages').textContent.includes('CIRUELA-761'));break;}
  }
  result.status=found?'PASS':'FAIL';result.adjuntoAnterior=found;
 }else{
  await page.locator('#nexo-tool').selectOption('general');await page.locator('#nexo-text').fill('Di HOLA');
  await page.locator('#nexo-send').click();
  await page.waitForFunction(()=>document.querySelector('#nexo-progress').textContent.includes('Ollama no completó'));
  result.errorVisible=await page.locator('#nexo-progress').textContent();result.status=result.modelo.includes('No disponible')?'PASS':'FAIL';
  await page.screenshot({path:__dirname+'/nexo-modelo-no-disponible.png'});
 }
 fs.writeFileSync(__dirname+'/navegador-'+mode+'.json',JSON.stringify(result,null,2));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
