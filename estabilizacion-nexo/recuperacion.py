import json, os, signal, sys, time, uuid
from pathlib import Path
from urllib import request
from bateria import login,new,turn,api,BASE
p=Path(__file__).parent
mode=sys.argv[1]
a=login('final-crash',True)
if mode=='crash':
    chat=new(a)
    code,seed=turn(a,chat,'Mi código de seguimiento es TIGRE-593. Confirma recepción.')
    assert code==200 and seed[-1]['type']=='done'
    data={'text':'Enumera quinientos animales con una descripción de diez líneas cada uno.', 'tool':'general','turnId':str(uuid.uuid4())}
    req=request.Request(BASE+f'/nexo/conversations/{chat}/messages',json.dumps(data).encode(),
        {'Content-Type':'application/json','Authorization':'Bearer '+a})
    with request.urlopen(req,timeout=30) as response:
        assert json.loads(response.readline())['type']=='status'
        os.kill(int(sys.argv[2]),signal.SIGKILL)
    (p/'crash-chat.txt').write_text(chat)
else:
    chat=(p/'crash-chat.txt').read_text()
    status,before=api('/nexo/conversations/'+chat,token=a)
    assert status==200 and len(before)==2 and 'TIGRE-593' in before[0]['content']
    start=time.monotonic()
    for _ in range(18):
        status,events=turn(a,chat,'¿Cuál es mi código de seguimiento?')
        if status==200: break
        assert status==409 and events.get('code')=='BUSY',events
        time.sleep(1)
    answer=''.join(e['text'] for e in events if e['type']=='delta')
    assert events[-1]['type']=='done' and 'TIGRE-593' in answer
    (p/'recuperacion.json').write_text(json.dumps(dict(messages_before_retry=len(before),seconds_after_restart=round(time.monotonic()-start,2),answer=answer),ensure_ascii=False))
