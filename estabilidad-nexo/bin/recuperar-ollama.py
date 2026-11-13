"""Verifies loss and recovery of the audit relay, leaving the shared Ollama running."""
import json, os, signal, subprocess, sys, time
from pathlib import Path
from bateria import api, login, new, turn
d=Path(__file__).parent.parent/'evidencias'
a=login('sre-offline',True)
chat=new(a)
os.kill(int(sys.argv[1]),signal.SIGTERM)
for _ in range(20):
    if not api('/nexo/status',token=a)[1]['connected']: break
    time.sleep(.25)
status,events=turn(a,chat,'Dime hola')
_,saved=api('/nexo/conversations/'+chat,token=a)
failure=events[-1]
assert status==200 and failure.get('code')=='MODEL_UNAVAILABLE' and not saved
with (d/'ollama-recuperado.log').open('w') as log:
    relay=subprocess.Popen([sys.executable,'verificacion-nexo/traza.py'],stdout=log,stderr=subprocess.STDOUT)
    try:
        for _ in range(20):
            if api('/nexo/status',token=a)[1]['connected']: break
            time.sleep(.25)
        status,events=turn(a,chat,'Calcula 17 por 23.')
        answer=''.join(e['text'] for e in events if e['type']=='delta')
        assert status==200 and events[-1]['type']=='done' and '391' in answer
        (d/'ollama-recuperacion.json').write_text(json.dumps(dict(error=failure.get('code'),saved_offline=len(saved),recovered=answer),ensure_ascii=False))
    finally:
        relay.terminate()
        relay.wait(timeout=5)
