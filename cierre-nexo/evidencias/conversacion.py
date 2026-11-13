import json
from pathlib import Path
from bateria import login,new,turn,api
a=login('cierre-long',True)
b=login('cierre-long-b',True)
chat=new(a)
for i in range(40):
 question='Mi proyecto ficticio se llama CEDRO-374. Confirma recepción.' if i==0 else (
     '¿Cómo se llama mi proyecto ficticio?' if i==39 else f'Calcula {i} + 2. Devuelve el resultado.')
 code,events=turn(a,chat,question)
 assert code==200 and events[-1]['type']=='done',(i,code,events[-1])
last=''.join(e['text'] for e in events if e['type']=='delta')
status,messages=api('/nexo/conversations/'+chat,token=a)
other=api('/nexo/conversations/'+chat,token=b)[0]
cap,error=turn(a,chat,'Prueba del límite de historial')
result=dict(turns=len(messages)//2,messages=len(messages),recall=last,foreign_http=other,cap_http=cap,cap_code=error.get('code'))
Path(__file__).with_name('conversacion.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
assert status==200 and len(messages)==80 and 'CEDRO-374' in last.upper() and other==404 and cap==429 and error['code']=='CHAT_FULL',result
