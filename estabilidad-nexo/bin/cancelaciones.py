import concurrent.futures,json,time,uuid
from pathlib import Path
from urllib import request
from carga import account,call
root=Path(__file__).parent.parent/'evidencias'
accounts=[account(),account()]
chats=[call(8090,'/nexo/conversations',{},t)[1]['id'] for t in accounts]
def start(i):
    tid=str(uuid.uuid4())
    port=8090+i
    path=f'/api/nexo/conversations/{chats[i]}'
    headers={'Authorization':'Bearer '+accounts[i],'Content-Type':'application/json'}
    data={'text':'Enumera 400 elementos con explicaciones detalladas.','tool':'general','turnId':tid}
    req=request.Request(f'http://localhost:{port}'+path+'/messages',json.dumps(data).encode(),headers)
    stream=request.urlopen(req,timeout=30)
    assert json.loads(stream.readline())['type']=='status'
    return stream,path,tid,headers
def cancel(args):
    i,(stream,path,tid,headers)=args
    started=time.monotonic()
    req=request.Request(f'http://localhost:{8091-i}'+path+'/turns/'+tid,headers=headers,method='DELETE')
    with request.urlopen(req,timeout=30) as response: code=response.status
    with stream: events=[json.loads(x) for x in stream]
    _,saved,_=call(8090,path.removeprefix('/api'),token=accounts[i])
    return dict(index=i,http=code,error=events[-1].get('code'),saved=len(saved),seconds=round(time.monotonic()-started,3))
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    jobs=list(pool.map(start,range(2)))
    result=list(pool.map(cancel,enumerate(jobs)))
(root/'cancelaciones.json').write_text(json.dumps(result,indent=2))
assert all(r['http']==204 and r['error']=='TURN_CANCELLED' and r['saved']==0 for r in result)
