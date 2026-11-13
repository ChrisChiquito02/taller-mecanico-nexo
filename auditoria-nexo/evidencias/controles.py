import json, hashlib, time, urllib.request
from bateria import api,login,new,turn,ROOT,BASE
def save(i,ok,**data):
    with (ROOT/'controles.jsonl').open('a') as f: f.write(json.dumps(dict(id=i,status='PASS' if ok else 'FAIL',**data),ensure_ascii=False)+'\n')
a=login('audit-alpha'); b=login('audit-beta')
_,profile=api('/users/me',token=b)
for i,request in [
 ('SEC-sim-ajena',{'source':'live','minute':0,'target':profile['id'],'roles':['ADMIN'],'required':['profile:read']}),
 ('SEC-parche-rol',{'source':'live','minute':0,'patchRole':'USER','patchPermissions':['profile:read'],'required':['profile:read']})
]:
    code,data=api('/nexo/simulate',request,a);save(i,code==403,http=code,body=data)
code,data=api('/nexo/documents/operacion',token=a);save('SEC-doc-privado',code==404,http=code,body=data)
code,data=api('/nexo/simulate',{'source':'live','minute':0,'roles':['ADMIN'],'required':['profile:read']},a)
save('SIM-lectura-propia',code==200 and data.get('mode')=='simulation_only',http=code,body=data)
c=new(a)
for i,name,sample in [('FILE-tipo','muestra.pdf','Contenido'),('FILE-tamano','muestra.txt','texto '*1001)]:
    code,data=turn(a,c,'Analiza el adjunto',attachment={'name':name,'text':sample});save(i,code==400 and data.get('code')=='INVALID_FILE',http=code,body=data)
code,data=api('/nexo/conversations/'+c,token=a,method='DELETE')
code2,d2=api('/nexo/conversations/'+c,token=a)
save('DELETE-normal',code in (200,204) and code2==404,http=code,lectura=code2)
api('/auth/forgot-password',{'email':'audit-alpha@example.test'})
for n in range(10):
    try:
        with urllib.request.urlopen('http://127.0.0.1:48025/api/v1/messages') as r: inbox=json.load(r)
        found=any(x.get('Address')=='audit-alpha@example.test' for m in inbox.get('messages',[]) for x in m.get('To',[]))
        if found: break
    except Exception: found=False
    time.sleep(.5)
save('SMTP-MAILPIT',found,mensajes=inbox.get('total',0))
