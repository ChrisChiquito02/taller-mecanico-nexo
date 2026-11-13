import json,os,subprocess,time
from pathlib import Path
from carga import account,call
d=Path(__file__).parent.parent/'evidencias'
token=account()
def probe(method,port=8092):
    try:
        if method=='warm':
            return call(port,'/health')[0]
        return call(port,'/nexo/conversations',{} if method=='POST' else None,token)[0]
    except OSError as e:
        return type(e).__name__
for i in range(20):
    if probe('warm')==200: break
    time.sleep(.5)
pid=subprocess.check_output(['lsof','-nP','-t','-iTCP:8092','-sTCP:LISTEN'],text=True).strip()
subprocess.run(['jcmd',pid,'JFR.start','name=warm','settings=profile','jdk.JavaExceptionThrow#enabled=true'],check=True)
before=probe('GET')
assert before==200
# The test mutates only the disposable JAR at 8092.
old=next(Path('/tmp').glob('examenerik-audit-build.*/target/backend-0.1.0-SNAPSHOT.jar'))
with (d/'replica-descartable.jar').open('wb') as dest:
    dest.write(old.read_bytes())
results=[dict(port=8092,get=probe('GET'),post=probe('POST')) for _ in range(5)]
subprocess.run(['jcmd',pid,'JFR.dump','name=warm','filename='+str(d.resolve()/'templada.jfr')],check=True)
(d/'templada.json').write_text(json.dumps(dict(before=before,results=results),indent=2))
