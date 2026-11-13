import json, hashlib, sys
from bateria import api,login,new,turn,ROOT
def record(id, condition, details):
    with (ROOT/'api-extra.jsonl').open('a') as f: f.write(json.dumps(dict(id=id,status='PASS' if condition else 'FAIL',details=details),ensure_ascii=False)+'\n')
def run(phase):
    a=login('audit-alpha')
    b=login('audit-beta')
    c=(ROOT/'chat-persistencia.txt').read_text()
    path='/nexo/conversations/'+c
    if phase in ('antes','despues'):
        status,body=api(path,token=a)
        digest=hashlib.sha256(json.dumps(body,sort_keys=True).encode()).hexdigest()
        mark=ROOT/'persistencia.sha256'
        if phase=='antes': mark.write_text(digest)
        record('DB-'+phase,status==200 and len(body)>20 and digest==mark.read_text(),dict(http=status,mensajes=len(body),sha256=digest))
        if phase=='despues':
            status,body=turn(a,c,'¿Cuál es el nombre de mi proyecto ficticio de esta conversación?')
            answer=''.join(e['text'] for e in body if e['type']=='delta') if isinstance(body,list) else ''
            record('MEM-reinicio',status==200 and 'zafiro-81' in answer.lower(),dict(http=status,respuesta=answer))
        return
    if phase=='offline':
        sc,s=api('/nexo/status',token=a); code,data=turn(a,c,'Di HOLA')
        code2,after=api(path,token=a)
        record('LLM-caida',sc==200 and not s['connected'] and code==200 and any(e.get('code')=='MODEL_UNAVAILABLE' for e in data) and not any(e['type']=='delta' for e in data),dict(status=s,http=code,eventos=data,mensajes=len(after),http_historial=code2))
        return
    def verify(id,call,condition):
        code,data=call();record(id,condition(code,data),dict(http=code,respuesta=data))
    code,profile=api('/users/me',token=b)
    verify('SEC-sim-ajena',lambda:api('/nexo/simulate',{'source':'live','minute':0,'target':profile['id'],'roles':['ADMIN'],'required':['profile:read']},a),lambda c,d:c==403)
    verify('SEC-parche-rol',lambda:api('/nexo/simulate',{'source':'live','minute':0,'patchRole':'USER','patchPermissions':['profile:read'],'required':['profile:read']},a),lambda c,d:c==403)
    verify('SEC-doc-privado',lambda:api('/nexo/documents/operacion',token=a),lambda c,d:c==404)
    verify('SEC-delete-ajeno',lambda:api(path,token=b,method='DELETE'),lambda c,d:c==404)
    verify('SEC-lista-aislada',lambda:api('/nexo/conversations',token=b),lambda s,d:s==200 and all(x['id']!=c for x in d))
    verify('TOOLS-catalogo',lambda:api('/nexo/tools',token=a),lambda s,d:s==200 and len(d)==5)
    for tool,prompt,expected in [
      ('explain_error','Explica NullPointerException al invocar length sobre una referencia nula.','null'),
      ('interpret_tests','Hay 10 pruebas, 8 pasan y 2 fallan. ¿Qué porcentaje pasó?','80'),
      ('report','Redacta informe con un título y tres puntos sobre 2 pruebas aprobadas y 1 fallida.','prueb'),
      ('search_docs','Cita las fuentes para explicar la arquitectura del proyecto ExamenErik.','arquitectura'),
      ('simulate_self','Explica la simulación de ascender mi cuenta a ADMIN.','riesgo')
    ]:
        s,d=turn(a,new(a),prompt,tool)
        answer=''.join(e['text'] for e in d if e['type']=='delta') if isinstance(d,list) else ''
        meta=next((e['metadata'] for e in d if e['type']=='done'),{}) if isinstance(d,list) else {}
        extra=(bool(meta.get('references')) if tool=='search_docs' else bool(meta.get('simulation')) if tool=='simulate_self' else True)
        record(tool,s==200 and meta.get('tool')==tool and expected in answer.lower() and extra,dict(http=s,respuesta=answer,metadata=meta))
run(sys.argv[1])
