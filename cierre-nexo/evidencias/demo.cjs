const { chromium }=require('../../frontend/node_modules/playwright');
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
(async()=>{
 const env=fs.readFileSync(path.join(__dirname,'../demo/.demo.env'),'utf8');
 const password=env.match(/^DEMO_PASSWORD=(.+)$/m)[1];
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try {
  const page=await browser.newPage();
  await page.goto('http://localhost:8090/#login');
  await page.locator('#login [name=email]').fill('demo-admin@example.test');
  await page.locator('#login [name=password]').fill(password);
  await page.locator('#login button').click();
  await page.locator('#view-account').waitFor();
  await page.locator('a[href="#nexo"]').click();
  await page.waitForFunction(()=>document.querySelector('#nexo-model').textContent.includes('Conectado'));
  await page.locator('#nexo-tool').selectOption('general');
  await page.locator('#nexo-text').fill('Multiplica 23 por 17. Devuelve el resultado.');
  const reply=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/messages'));
  await page.locator('#nexo-send').click();
  const result=await reply;
  const events=(await result.text()).trim().split('\n').map(JSON.parse);
  const answer=events.filter(e=>e.type==='delta').map(e=>e.text).join('');
  assert.equal(events.at(-1).type,'done');
  assert.match(answer,/391/);
  await page.waitForFunction(()=>!document.querySelector('#nexo-send').disabled);
  await page.reload();
  await page.locator('#nexo-chats button').first().click();
  await page.waitForFunction(()=>document.querySelector('#nexo-messages').textContent.includes('391'));
  console.log(JSON.stringify({model:await page.locator('#nexo-model').textContent(),answer,persisted:true}));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
