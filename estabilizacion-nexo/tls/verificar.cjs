const { chromium } = require('../../frontend/node_modules/playwright');
const fs=require('node:fs');
const assert=require('node:assert/strict');
const qaEmail=process.env.QA_ADMIN_EMAIL;
const qaPassword=process.env.QA_ADMIN_PASSWORD;
if(!qaEmail||!qaPassword) throw new Error('Define QA_ADMIN_EMAIL y QA_ADMIN_PASSWORD antes de ejecutar la verificación');
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try{
  const page=await browser.newPage({ignoreHTTPSErrors:true});
  const base='https://localhost:8443';
  await page.goto(base+'/#login');
  await page.locator('#login [name=email]').fill(qaEmail);
  await page.locator('#login [name=password]').fill(qaPassword);
  await page.locator('#login button').click();
  await page.locator('#view-account').waitFor();
  const cookies=await page.context().cookies();
  const refresh=cookies.find(c=>c.name==='refresh_token');
  assert.ok(refresh?.secure && refresh?.httpOnly && refresh?.sameSite==='Strict');
  assert.equal(await page.evaluate(()=>document.cookie.includes('refresh_token')),false);
  await page.reload();
  await page.locator('#view-account').waitFor();
  fs.writeFileSync(__dirname+'/chrome.json',JSON.stringify({
    secure:refresh.secure,httpOnly:refresh.httpOnly,sameSite:refresh.sameSite,
    path:refresh.path,reload:true,certificado:'autofirmado en prueba local'
  }));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
