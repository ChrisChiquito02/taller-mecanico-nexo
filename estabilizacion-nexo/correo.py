import json,time,uuid,re
from pathlib import Path
from urllib.request import urlopen
from bateria import api
email=f'final-recovery-{uuid.uuid4().hex[:10]}@example.test'
initial='AuditSyntheticBefore-19!'
updated='AuditSyntheticAfter-19!'
assert api('/auth/register',dict(email=email,password=initial,displayName='Recuperación sintética'))[0]==202
assert api('/auth/forgot-password',{'email':email})[0]==202
for _ in range(30):
 with urlopen('http://127.0.0.1:48025/api/v1/messages') as r: inbox=json.load(r)
 msg=next((m for m in inbox['messages'] if any(t['Address']==email for t in m['To'])),None)
 if msg: break
 time.sleep(.5)
assert msg
with urlopen('http://127.0.0.1:48025/api/v1/message/'+msg['ID']) as r: body=json.load(r)
token=re.search(r'#token=([A-Za-z0-9_-]{43})',body['Text']).group(1)
reset=api('/auth/reset-password',dict(token=token,password=updated))[0]
reuse=api('/auth/reset-password',dict(token=token,password=updated))[0]
old=api('/auth/login',dict(email=email,password=initial))[0]
new=api('/auth/login',dict(email=email,password=updated))[0]
assert reset==204 and reuse>=400 and old==401 and new==200,(reset,reuse,old,new)
Path(__file__).with_name('correo.json').write_text(json.dumps(dict(smtp='Mailpit',reset=reset,reuse=reuse,old=old,new=new)))
