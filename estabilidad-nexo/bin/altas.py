import sys,json,uuid
from pathlib import Path
from carga import call
def run():
    port=int(sys.argv[1])
    email='sre-'+uuid.uuid4().hex[:10]+'@example.test'
    account={'email':email,'password':'SyntheticProbe-39!'}
    code,_,_=call(8090,'/auth/register',dict(account,displayName='Cuenta de prueba'))
    assert code==202
    code,session,_=call(8090,'/auth/login',account)
    assert code==200
    token=session['accessToken']
    rows=[]
    for p in [8090,port,8090,port]:
        for path,body in [('/health',None),('/nexo/conversations',{}),('/nexo/conversations',None)]:
            code,data,seconds=call(p,path,body,token)
            rows.append(dict(port=p,path=path,method='GET' if body is None else 'POST',http=code,code=data.get('code') if isinstance(data,dict) else None,ms=round(seconds*1000,2)))
    print(json.dumps(rows,indent=2))
run()
