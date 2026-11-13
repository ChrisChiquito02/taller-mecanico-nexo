import json, re
from pathlib import Path
p=Path(__file__).parent
rows=[json.loads(l) for l in (p/'ollama.log').read_text().splitlines() if l.startswith('{')]
browser=json.loads((p/'navegador.json').read_text())
for r in browser:
 if r.get('event')=='turn':
    b=r['evidence']
    matches=[x for x in rows if not x['plan'] and x['answer_sha256']==b['sha256']]
    print(b['sha256'], 'coincidencias:', len(matches), 'texto DOM idéntico:', b['answer']==b['visible'],'longitud DOM:',len(b['visible']))
    if matches: print('Modelo:',matches[-1]['model'],'tokens:',matches[-1]['eval_count'],'fin:',matches[-1]['done_reason'])
    refs={x['id'] for x in (b.get('metadata') or {}).get('references',[])}
    cites=set(re.findall(r'\[([a-z0-9_-]+)\]',b['answer']))
    print('Referencias permitidas:',sorted(refs),'citas fuera del conjunto:',sorted(cites-refs))
