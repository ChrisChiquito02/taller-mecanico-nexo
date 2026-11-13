import json
from pathlib import Path
from urllib.request import Request,urlopen
cases=[
 ('Escribe exactamente tres líneas y al menos cinco líneas. ¿Es posible?',('incompat','imposible','no se puede','no es posible')),
 ('Write exactly three sentences and at most one sentence. Can you satisfy both?',('conflict','cannot','impossible','not possible')),
 ('Escribe exactamente dos líneas y al menos una línea sobre árboles.',()),
 ('Responde en español y exclusivamente en inglés. ¿Puedes satisfacer ambas?',('incompat','imposible','no puedo','no es posible')),
]
rows=[]
for prompt,expected in cases:
 for attempt in range(3):
    data={'model':'qwen2.5-coder:7b','stream':False,'options':{'temperature':0.2,'num_predict':180},
          'messages':[{'role':'system','content':'Prioriza la petición más reciente. Si detectas condiciones incompatibles, identifícalas y pide que el usuario elija.'},
                      {'role':'user','content':prompt}]}
    req=Request('http://localhost:11434/api/chat',json.dumps(data).encode(),{'Content-Type':'application/json'})
    with urlopen(req,timeout=60) as r: result=json.load(r)
    answer=result['message']['content']
    ok=any(e in answer.lower() for e in expected) if expected else len([l for l in answer.splitlines() if l.strip()])==2
    rows.append(dict(prompt=prompt,attempt=attempt+1,passed=ok,answer=answer,reason=result.get('done_reason'),tokens=result.get('eval_count')))
Path(__file__).with_name('modelo-directo.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2))
