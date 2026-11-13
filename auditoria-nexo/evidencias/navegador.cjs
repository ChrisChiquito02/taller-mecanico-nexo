const { chromium } = require('../../frontend/node_modules/playwright');
const { createHash } = require('node:crypto');
const fs = require('node:fs');
const base = 'http://localhost:8090';
const output = __dirname;
const sleep = ms => new Promise(r=>setTimeout(r,ms));
(async()=>{
 const browser = await chromium.launch({channel:'chrome',headless:true});
 const page = await browser.newPage({acceptDownloads:true});
 const records=[];
 const push=(row)=>{records.push(row);fs.writeFileSync(output+'/navegador.json',JSON.stringify(records,null,2));};
 const check=async(id,name,fn)=>{try{const details=await fn();push({id,name,status:'PASS',details});}catch(e){push({id,name,status:'FAIL',error:String(e)});}};
 const requireIt=(x,msg)=>{if(!x)throw Error(msg);};
 page.on('pageerror',e=>push({event:'pageerror',message:e.message}));
 page.on('console',e=>{if(['error','warning'].includes(e.type()))push({event:'console',type:e.type(),message:e.text()});});
 page.on('requestfailed',r=>push({event:'requestfailed',path:new URL(r.url()).pathname,error:r.failure()}));
 page.on('response',r=>{if(r.url().includes('/api/'))push({event:'http',method:r.request().method(),path:new URL(r.url()).pathname,status:r.status()});});
 const el=id=>page.locator('#'+id);
 const waitText=async(id,pattern)=>page.waitForFunction(([id,pattern])=>new RegExp(pattern,'i').test(document.getElementById(id).textContent),[id,pattern],{timeout:220000});
 const model=async({prompt,tool='auto',file,explain=false,expected}={})=>{
  if(file) await el('nexo-file').setInputFiles(file);
  if(!explain){await el('nexo-tool').selectOption(tool);await el('nexo-text').fill(prompt);}
  const pending=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/messages'),{timeout:220000});
  await el(explain?'nexo-explain':'nexo-send').click();
  await el('nexo-stop').waitFor({state:'visible',timeout:2000});
  const response=await pending;const raw=await response.text();
  const events=raw.trim().split('\n').map(JSON.parse);
  const answer=events.filter(e=>e.type==='delta').map(e=>e.text).join('');
  const visible=await page.locator('.nexo-bubble.assistant .nexo-body').last().textContent();
  const metadata=events.find(e=>e.type==='done')?.metadata;
  const evidence={answer,visible,sha256:createHash('sha256').update(answer).digest('hex'),metadata,events:[...new Set(events.map(e=>e.type))],http:response.status(),errors:events.filter(e=>e.type==='error')};
  push({event:'turn',tool,evidence});
  requireIt(metadata?.generated && !evidence.errors.length,'Falta evento done generado');
  requireIt(visible.trim().length>0,'Sin respuesta visible');
  if(expected) requireIt(expected.test(answer),'Respuesta no satisface el criterio');
  return evidence.sha256;
 };
 try {
 push({browser:browser.version()});
 await check('UI01','Raíz y protección de enlace directo',async()=>{
  requireIt((await page.goto(base+'/')).status()===200,'Raíz no disponible');
  await page.goto(base+'/#nexo');await el('view-login').waitFor({state:'visible'});
  requireIt(!(await el('view-nexo').isVisible()),'Chat accesible sin iniciar sesión');
 });
 await check('UI02','Login con cuenta sintética y navegación',async()=>{
  await page.locator('#login [name=email]').fill('audit-admin@example.test');
  await page.locator('#login [name=password]').fill('AuditOnly-19!Test');
  await page.locator('#login button').click();await el('view-account').waitFor();
  await page.locator('a[href="#nexo"]').click();await el('view-nexo').waitFor();
 });
 await check('UI03','Comprobar modelo',async()=>{await el('nexo-check').click();await waitText('nexo-model','Conectado.*qwen2.5-coder:7b');return await el('nexo-model').textContent();});
 await check('UI04','Tres sugerencias rellenan el mensaje',async()=>{
  for(const b of await page.locator('[data-nexo-prompt]').all()){await b.click();requireIt(await el('nexo-text').inputValue()===await b.getAttribute('data-nexo-prompt'),'Sugerencia incorrecta');}
 });
 await check('UI05','Auto, estado de espera, inferencia y documentación',()=>model({prompt:'Explica la arquitectura de ExamenErik y cita al menos una fuente [id].',expected:/arquitectura|backend/i}));
 await check('UI06','Abrir y cerrar fuente',async()=>{await page.locator('.nexo-references button').first().click({timeout:3000});await el('nexo-document').waitFor();requireIt((await el('nexo-document-text').textContent()).length>100,'Documento vacío');await el('nexo-document-close').click();await el('nexo-document').waitFor({state:'hidden'});});
 await check('UI07','Analizar archivo de texto adjunto',()=>model({prompt:'Lee el archivo adjunto y devuelve su código de inventario.',tool:'general',file:{name:'inventario.txt',mimeType:'text/plain',buffer:Buffer.from('Código de inventario: CIRUELA-761')},expected:/ciruela-761/i}));
 await check('UI08','Historial tras recarga',async()=>{await page.reload();if(!(await el('view-nexo').isVisible()))await page.locator('a[href="#nexo"]').click();await page.locator('#nexo-chats button').first().click();await waitText('nexo-messages','CIRUELA-761');});
 await check('UI09','Nueva conversación y vista vacía',async()=>{await el('nexo-new').click();requireIt(await page.locator('.nexo-bubble').count()===0,'Vista no vacía');});
 await check('UI10','Tres escenarios sintéticos etiquetados y con resultado',async()=>{
  const results={};
  for(const scenario of ['ascenso','consulta','compartido']){
   await el('nexo-scenario').selectOption(scenario);
   const reply=page.waitForResponse(r=>r.url().endsWith('/nexo/simulate'));
   await el('nexo-simulate').click();const r=await reply;
   requireIt(r.ok(),'Simulación rechazada: '+r.status());
   const data=await r.json();await waitText('nexo-report','Escenario sintético.*ningún cambio aplicado');
   results[scenario]=data.requested.risk.score;
  }return results;
 });
 await check('UI11','Lectura IAM, modo alterno y controles de administrador',async()=>{
  await el('nexo-source').selectOption('live');await el('nexo-live-options').waitFor();
  await el('nexo-target').waitFor();await el('nexo-patch').waitFor();
  await el('nexo-role').selectOption('USER');
  const reply=page.waitForResponse(r=>r.url().endsWith('/nexo/simulate'));
  await el('nexo-simulate').click();const r=await reply;requireIt(r.ok(),'Lectura rechazada '+r.status());
  await waitText('nexo-report','Lectura autorizada');
  return (await r.json()).requested.risk;
 });
 await check('UI12','Explicar simulación con IA',()=>model({explain:true,expected:/riesgo|puntaje|alternativa/i}));
 await check('UI13','Exportar JSON real',async()=>{
  const download=page.waitForEvent('download');await el('nexo-export').click();const file=await download;await file.saveAs(output+'/exportacion.json');const data=JSON.parse(fs.readFileSync(output+'/exportacion.json'));requireIt(data.simulation&&data.messages.length>0,'Exportación incompleta');return {messages:data.messages.length,simulation:data.simulation.source};
 });
 await check('UI14','Vista móvil sin desbordamiento general',async()=>{await page.setViewportSize({width:375,height:812});const dimensions=await page.evaluate(()=>({w:innerWidth,scroll:document.documentElement.scrollWidth}));await page.screenshot({path:output+'/nexo-movil.png',fullPage:true});requireIt(dimensions.w>=dimensions.scroll,JSON.stringify(dimensions));return dimensions;});
 await check('UI15','Detener generación y comprobar historial',async()=>{
  await el('nexo-new').click();await el('nexo-tool').selectOption('general');
  await el('nexo-text').fill('Escribe una lista de cien palabras con definiciones.');
  const pending=page.waitForResponse(r=>r.url().endsWith('/messages'));
  await el('nexo-send').click();const response=await pending;
  const pathname=new URL(response.url()).pathname.replace(/\/messages$/,'');
  await el('nexo-stop').click();await waitText('nexo-progress','Generación detenida');
  const client=await page.context().request.post(base+'/api/auth/login',{headers:{Origin:base},data:{email:'audit-admin@example.test',password:'AuditOnly-19!Test'}});
  const token=(await client.json()).accessToken;
  await sleep(8000);
  const saved=await page.context().request.get(base+pathname,{headers:{Authorization:'Bearer '+token}});
  const msgs=await saved.json();push({event:'cancel-check',count:msgs.length,status:saved.status()});
  requireIt(msgs.length===0,'El turno detenido quedó persistido');
 });
 await check('UI16','Eliminar únicamente chat sintético',async()=>{
  page.once('dialog',d=>d.accept());const pending=page.waitForResponse(r=>r.request().method()==='DELETE');
  await el('nexo-delete').click();const r=await pending;requireIt(r.ok(),'DELETE '+r.status());
 });
 await check('UI17','Logout y bloqueo de NEXO',async()=>{await page.locator('a[href="#account"]').click();await el('logout').click();await el('view-login').waitFor();await page.goto(base+'/#nexo');await el('view-login').waitFor();});
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
