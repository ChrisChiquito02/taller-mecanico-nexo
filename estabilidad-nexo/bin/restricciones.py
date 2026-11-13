import json,re
from pathlib import Path
from bateria import api,login,new,turn
p=Path(__file__).parent
a=login('sre-limits',True)
cases=[
 ('words','Responde con exactamente dos palabras y al menos una palabra. Saluda.',lambda s:len(re.findall(r'\w+',s))==2),
 ('lines','Write exactly two lines and at most three lines. Name two colors.',lambda s:len([line for line in s.splitlines() if line.strip()])==2),
 ('mixed','Escribe exactamente dos líneas y como máximo cuatro palabras. Nombra dos planetas.',lambda s:len(s.splitlines())==2 and len(re.findall(r'\w+',s))<=4),
 ('single','Responde con exactamente 2 palabras. Nombra un animal y un color.',lambda s:len(re.findall(r'\w+',s))==2),
 ('incompatible','Escribe al menos 3 líneas y exactamente 4 palabras y como máximo 1 línea. Saluda.',None),
 ('impossible_content','Write exactly zero words. Explain how to make tea.',lambda s:len(re.findall(r'\w+',s))==0),
]
out=[]
for name,prompt,check in cases:
 for _ in range(3):
  chat=new(a)
  code,events=turn(a,chat,prompt,'auto')
  text=''.join(e['text'] for e in events if e['type']=='delta')
  done=next((e['metadata'] for e in events if e['type']=='done'),None)
  failure=next((e.get('code') for e in events if e['type']=='error'),None)
  _,saved=api('/nexo/conversations/'+chat,token=a)
  accepted=bool(done) and (done.get('origin')=='clarification' if check is None else done.get('outputConstraintsVerified') is True and check(text))
  rejected=failure=='OUTPUT_CONSTRAINT_UNMET' and not text and not done and not saved
  out.append(dict(case=name,passed=(accepted and len(saved)==2) or rejected,http=code,text=text,error=failure,saved=len(saved),metadata=done))
(p/'restricciones.json').write_text(json.dumps(out,ensure_ascii=False,indent=2))
assert all(r['passed'] for r in out)
