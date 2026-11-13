import json
from collections import Counter
from pathlib import Path
from carga import account,call
token=account()
rows=[]
for i in range(62):
    port=8090+i%2
    code,data,elapsed=call(port,'/nexo/conversations',{},token)
    rows.append(dict(port=port,http=code,code=data.get('code'),ms=round(elapsed*1000,2)))
summary=Counter(str(r['code'] or r['http']) for r in rows)
Path(__file__).parent.parent.joinpath('evidencias/cuota.json').write_text(json.dumps(dict(summary=summary,rows=rows),indent=2))
assert summary=={'200':30,'CHAT_LIMIT':30,'NEXO_RATE_LIMIT':2},summary
