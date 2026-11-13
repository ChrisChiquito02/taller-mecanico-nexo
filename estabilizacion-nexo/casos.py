import json
from pathlib import Path
from bateria import login,new,turn,api
root=Path(__file__).parent
a=login('final-cases',True)
c=new(a)
def text(e): return ''.join(x['text'] for x in e if x['type']=='delta')
def meta(e): return next((x['metadata'] for x in e if x['type']=='done'),{})
cases=[
 ('bounds-es','Escribe exactamente tres líneas y al menos cinco líneas. ¿Es posible?','general','clarification','incompatibles'),
 ('bounds-en','Write exactly 3 sentences and at most 1 sentence.','general','clarification','conflict'),
 ('compatible','Responde con exactamente dos palabras y al menos una palabra. Saluda.','general',None,''),
 ('report','Prepara un informe de resultados.\nLa prueba Alfa pasó.\nLa prueba Beta falló por timeout.\nLa prueba Gamma pasó.','report','report','prueba Beta'),
 ('report-mixed','Redacta informe con un título y tres puntos sobre 2 pruebas aprobadas y 1 fallida.','report','report','2 pruebas aprobadas'),
 ('sim','Explica qué tokens se afectarían al simular el rol ADMIN en mi cuenta.','simulate_self','simulation','El total de sesiones y de JWT de acceso es desconocido.')]
rows=[]
for id,prompt,tool,origin,expect in cases:
 code,events=turn(a,c,prompt,tool)
 result=text(events)
 m=meta(events)
 ok=code==200 and bool(m) and m.get('origin')==origin and expect.lower() in result.lower()
 rows.append(dict(id=id,passed=ok,answer=result,events=[e for e in events if e['type']=='error'],metadata=m))
(root/'casos.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2))
