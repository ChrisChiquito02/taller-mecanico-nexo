import concurrent.futures
import json
import statistics
import time
import uuid
from collections import Counter
from urllib import request, error
from pathlib import Path

OUT = Path(__file__).parent
def call(port, path, body=None, token=None, timeout=50):
    headers={'Origin':'http://localhost:8090','Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    req=request.Request(f'http://localhost:{port}/api'+path, None if body is None else json.dumps(body).encode(), headers)
    started=time.monotonic()
    try:
        with request.urlopen(req,timeout=timeout) as r:
            raw=r.read().decode()
            data=[json.loads(line) for line in raw.splitlines()] if 'ndjson' in r.headers.get('Content-Type','') else json.loads(raw or 'null')
            return r.status,data,time.monotonic()-started
    except error.HTTPError as e:
        return e.code,json.loads(e.read()),time.monotonic()-started

def account():
    data={'email':f'carga-{uuid.uuid4().hex[:10]}@example.test','password':'LoadTestSynthetic-39!'}
    assert call(8090,'/auth/register',dict(data,displayName='Cuenta sintética de carga'))[0]==202
    return call(8090,'/auth/login',data)[1]['accessToken']

def main():
    tokens=[account() for _ in range(6)]
    chat_ids=[call(8090,'/nexo/conversations',{},t)[1]['id'] for t in tokens]
    def worker(i, deadline):
        port=8090
        result=[]
        while time.monotonic()<deadline and len(result)<36:
            code,data,t=call(port,f'/nexo/conversations/{chat_ids[i]}/messages',{'text':'Calcula 17 por 23. Solo el número.', 'tool':'general'},tokens[i])
            if code==200:
                text=''.join(e['text'] for e in data if e['type']=='delta')
                state='DONE' if data[-1]['type']=='done' and '391' in text else data[-1].get('code',data[-1]['type'])
            else:
                state=data.get('code',str(code))
            result.append((i,port,code,state,round(t,3)))
            port=8091 if port==8090 else 8090
            time.sleep(1.5)
        return result
    start=time.monotonic()
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        runs=list(pool.map(lambda i:worker(i,start+65),range(6)))
    rows=[x for r in runs for x in r]
    successful=[r[-1] for r in rows if r[3]=='DONE']
    readback=[len(call(8090,f'/nexo/conversations/{c}',token=t)[1]) for c,t in zip(chat_ids,tokens)]
    states=Counter(r[3] for r in rows)
    cap=account()
    burst=[call(8090 if i%2==0 else 8091,'/nexo/conversations',{},cap) for i in range(62)]
    # Chat cap at 30 and request cap at 60 must be shared between both replicas.
    quota=Counter(d.get('code',str(c)) for c,d,_ in burst)
    stats=dict(seconds=round(time.monotonic()-start,2),accounts=len(tokens),states=states,
        median=statistics.median(successful),p95=statistics.quantiles(successful,n=20,method='inclusive')[18],
        saved=readback,quota=quota,rows=rows)
    (OUT/'carga.json').write_text(json.dumps(stats,indent=2))
    assert sum(readback)==2*len(successful)
    assert all(state in {'DONE','MODEL_BUSY'} for state in states),states
    assert len(successful)>=12 and quota=={'200':30,'CHAT_LIMIT':30,'NEXO_RATE_LIMIT':2},quota
    print({k:v for k,v in stats.items() if k!='rows'})

if __name__=='__main__': main()
