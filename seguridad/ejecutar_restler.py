#!/usr/bin/env python3
"""Crea y destruye SOLO contenedores propios. Nunca acepta URL externa ni credenciales reales."""
import argparse
import signal
import base64
from datetime import datetime, timezone
import hashlib
import json
import os
import re
from collections import Counter
from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import time
import urllib.request
import urllib.error
import uuid

ROOT = Path(__file__).resolve().parents[1]

def docker(*args):
    return subprocess.check_output(["docker", *args], text=True, stderr=subprocess.PIPE).strip()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--restler-drop", type=Path, required=True)
    parser.add_argument("--source-commit", required=True)
    parser.add_argument("--minutes", type=int, default=1)
    args = parser.parse_args()
    if not 1 <= args.minutes <= 10: raise SystemExit("Duración permitida: 1–10 minutos")
    drop = args.restler_drop.resolve()
    binary = drop / "restler/Restler.dll"
    if not binary.is_file(): raise SystemExit("Compila RESTler primero")
    name = "examenerik-fuzz-" + uuid.uuid4().hex[:8]
    private = ROOT / "seguridad/private" / name; private.mkdir(parents=True, mode=0o700)
    public = ROOT / "documentacion/actividad/evidencia" / name; public.mkdir(parents=True)
    resources = []; network = False; image = False
    report = {"started_utc": datetime.now(timezone.utc).isoformat(), "run_id": name,
              "restler_source_commit": args.source_commit,
              "restler_binary_sha256": hashlib.sha256(binary.read_bytes()).hexdigest(),
              "scope": "MySQL 8.4 + API Java21 + Mailpit, red Docker interna, datos sintéticos, loopback",
              "spec_source_sha256": hashlib.sha256((ROOT/'docs/integracion/openapi.json').read_bytes()).hexdigest(),
              "commands_exit_codes": {}, "completed": False}
    def envfile(filename, values):
        file = private / filename
        file.write_text("".join(f"{k}={v}\n" for k,v in values.items())); file.chmod(0o600)
        return str(file)
    def request(base, path, data=None, token=None, origin=None, method=None, extra=None):
        headers = {"Content-Type":"application/json"}
        if token: headers["Authorization"] = "Bearer " + token
        if origin: headers["Origin"] = origin
        headers.update(extra or {})
        req = urllib.request.Request(base+path, data=None if data is None else json.dumps(data).encode(), headers=headers, method=method)
        try: return urllib.request.urlopen(req, timeout=10)
        except urllib.error.HTTPError as e: return e
    def restler(mode, arguments):
        log = private / (mode + '.txt')
        with log.open('w') as stream:
            process = subprocess.Popen(['dotnet', str(binary), mode, *arguments], cwd=private, stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
            try:
                code = process.wait(timeout=180)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGTERM)
                try: process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL); process.wait()
                raise RuntimeError(f'RESTler {mode} excedió 180 segundos; se detuvo su grupo de procesos')
            result = subprocess.CompletedProcess(process.args, code)
        report['commands_exit_codes'][mode] = result.returncode
        if result.returncode: raise RuntimeError(f"RESTler {mode} falló; revisar registro privado")
    try:
        docker('network','create','--internal',name); network=True
        docker('network','create',name+'-entry')
        pw = secrets.token_urlsafe(24); adminpw=secrets.token_urlsafe(24)
        resources.append(docker('run','-d','--network',name,'--network-alias','mysql','--tmpfs','/var/lib/mysql:rw,size=768m', '--env-file',envfile('mysql.env',{
            'MYSQL_DATABASE':'identity_synthetic','MYSQL_USER':'synthetic','MYSQL_PASSWORD':pw,'MYSQL_ROOT_PASSWORD':secrets.token_urlsafe(24)}),'mysql:8.4'))
        for _ in range(90):
            try: docker('exec',resources[0],'mysqladmin','ping','-h','127.0.0.1','--silent'); break
            except subprocess.CalledProcessError: time.sleep(1)
        else: raise RuntimeError('MySQL sintético no disponible')
        resources.append(docker('run','-d','--network',name,'--network-alias','mailpit','axllent/mailpit:v1.27'))
        subprocess.run(['docker','build','-q','-t',name,str(ROOT/'sistema-m01-m02')],check=True,stdout=subprocess.DEVNULL)
        image=True
        resources.append(docker('run','-d','--network',name,'-p','127.0.0.1::8080','--read-only','--tmpfs','/tmp','--env-file',envfile('app.env',{
            'SPRING_PROFILES_ACTIVE':'local','DB_URL':'jdbc:mysql://mysql:3306/identity_synthetic?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC',
            'DB_USERNAME':'synthetic','DB_PASSWORD':pw,'JWT_SIGNING_KEY':base64.b64encode(secrets.token_bytes(48)).decode(),
            'APP_ALLOWED_ORIGINS':'http://localhost:5173','APP_COOKIE_SECURE':'false','APP_RECOVERY_MODE':'development',
            'APP_RECOVERY_BASE_URL':'http://localhost:5173/reset-password.html','ALLOWED_EMAIL_DOMAINS':'example.test',
            'SMTP_HOST':'mailpit','SMTP_PORT':'1025','SMTP_AUTH':'false','SMTP_STARTTLS':'false','SMTP_FROM':'noreply@example.test',
            'APP_BOOTSTRAP_ENABLED':'true','APP_BOOTSTRAP_EMAIL':'admin@example.test','APP_BOOTSTRAP_PASSWORD':adminpw,
            'AUTH_REQUESTS_PER_MINUTE':'10000','MAX_FAILED_ATTEMPTS':'100','NEXO_PROVIDER':'disabled','NEXO_REQUESTS_PER_MINUTE':'60'}), name))
        docker('network','connect',name+'-entry',resources[-1])
        for _ in range(10):
            try:
                port=docker('port',resources[-1],'8080/tcp').split(':')[-1]; break
            except subprocess.CalledProcessError: time.sleep(1)
        else: raise RuntimeError('Puerto loopback no publicado')
        base='http://127.0.0.1:'+port
        admin=None
        for _ in range(90):
            try:
                resp=request(base,'/api/auth/login',{'email':'admin@example.test','password':adminpw})
                if resp.status==200: admin=json.load(resp)['accessToken']; break
            except (OSError, ValueError): pass
            time.sleep(1)
        if not admin: raise RuntimeError('API sintética no disponible')
        # Esta cuenta es prescindible: las mutaciones jamás afectan al administrador del laboratorio.
        request(base,'/api/auth/register',{'email':'target@example.test','password':pw,'displayName':'Synthetic target'})
        targets=json.load(request(base,'/api/admin/users',token=admin))
        target=next(u['id'] for u in targets if u['email']=='target@example.test')
        report['preflight']={}
        report['preflight']['profile_admin']=request(base,'/api/users/me',token=admin).status
        report['preflight']['refresh_bad_origin']=request(base,'/api/auth/refresh',{},origin='https://evil.example.test').status
        report['preflight']['refresh_missing_origin']=request(base,'/api/auth/refresh',{}).status
        login=request(base,'/api/auth/login',{'email':'target@example.test','password':pw})
        cookie=login.headers.get('Set-Cookie').split(';')[0]
        refreshed=request(base,'/api/auth/refresh',{},origin='http://localhost:5173',extra={'Cookie':cookie})
        report['preflight']['refresh_allowed_origin']=refreshed.status
        report['preflight']['preflight_allowed_origin']=request(base,'/api/users/me',origin='http://localhost:5173',method='OPTIONS',extra={'Access-Control-Request-Method':'GET','Access-Control-Request-Headers':'authorization'}).status
        spec=json.loads((ROOT/'docs/integracion/openapi.json').read_text())
        spec['openapi']='3.0.3'; spec['servers']=[{'url':base}]
        # Orden explícito register -> login. JWT válido se aporta mediante autenticación de RESTler.
        spec['x-restler-global-annotations']=[{'producer_endpoint':'/api/auth/register','producer_method':'POST','consumer_endpoint':'/api/auth/login','consumer_method':'POST'},
            {'producer_endpoint':'/api/nexo/conversations','producer_method':'POST','producer_resource_name':'id','consumer_endpoint':'/api/nexo/conversations/{conversationId}','consumer_method':'GET','consumer_param':'conversationId'},
            {'producer_endpoint':'/api/nexo/conversations','producer_method':'POST','producer_resource_name':'id','consumer_endpoint':'/api/nexo/conversations/{conversationId}/messages','consumer_method':'POST','consumer_param':'conversationId'}]
        report['model_mode']='disabled: 503 MODEL_UNAVAILABLE is expected; no inference fuzzed'
        report['spec_paths']=len(spec['paths'])
        # El diccionario fija un usuario sintético consumible, y evita invalidar el token del fuzzer.
        dictionary={'restler_fuzzable_string':['fuzzstring','',"' OR 1=1 --",'<script>alert(1)</script>'],
            'restler_fuzzable_int':['0','-1','2147483647'],
            'restler_custom_payload':{'email':['flow@example.test'],'password':[pw], 'displayName':['Synthetic fuzz'], 'id':[target]},
            'restler_custom_payload_header':{'Origin':['http://localhost:5173']}}
        (private/'spec.json').write_text(json.dumps(spec,indent=2))
        (private/'dictionary.json').write_text(json.dumps(dictionary,indent=2))
        config={'SwaggerSpecFilePath':[str(private/'spec.json')], 'CustomDictionaryFilePath':str(private/'dictionary.json'),
                'ResolveBodyDependencies':True,'ResolveQueryDependencies':True,'ResolveHeaderDependencies':True,
                'AllowGetProducers':False,'DataFuzzing':True,'UseBodyExamples':True}
        (private/'compiler.json').write_text(json.dumps(config,indent=2))
        restler('compile',[str(private/'compiler.json')])
        # Location evita interpolar comandos o imprimir credenciales. Los logs completos quedan privados.
        (private/'token.txt').write_text("{'app':{}}\nAuthorization: Bearer "+admin+"\n")
        (private/'token.txt').chmod(0o600)
        settings={'target_ip':'127.0.0.1','target_port':int(port),'use_ssl':False,
            'max_combinations':1,'max_request_execution_time':10,'max_sequence_length':3,'fuzzing_mode':'bfs-cheap',
            'authentication':{'token':{'location':str(private/'token.txt'),'token_refresh_interval':30}}}
        (private/'settings.json').write_text(json.dumps(settings,indent=2))
        common=['--grammar_file',str(private/'Compile/grammar.py'),'--dictionary_file',str(private/'Compile/dict.json'), '--settings',str(private/'settings.json'),'--no_ssl','--no_results_analyzer']
        restler('test',common+['--time_budget',str(1/60)])
        restler('fuzz',common+['--time_budget',str(args.minutes/60)])
        for file in private.rglob('testing_summary.json'):
            dest=public/(file.parts[len(private.parts)]+'-testing_summary.json');shutil.copy2(file,dest)
        # Cobertura contiene requests; solo exportar el resultado agregado y dejar cuerpos/tokens privados.
        for file in private.rglob('runSummary.json'):
            dest=public/(file.parts[len(private.parts)]+'-runSummary.json');shutil.copy2(file,dest)
        for file in private.rglob('bug_buckets.txt'):
            report.setdefault('bug_bucket_summaries',[]).append({'phase':file.parts[len(private.parts)],'text':file.read_text()[:4000]})
        report['grammar_sha256']=hashlib.sha256((private/'Compile/grammar.py').read_bytes()).hexdigest()
        report['dependency_files']=[str(p.relative_to(private)) for p in (private/'Compile').glob('*dependenc*')]
        report['analyzer_errors'] = []
        for phase in ['Test', 'Fuzz']:
            err = private/phase/'ResultsAnalyzerStdErr.txt'
            text = err.read_text() if err.exists() else ''
            if 'exception' in text.lower() or 'failed' in text.lower():
                report['analyzer_errors'].append(phase)
            counts=Counter(); sent=0
            for log in (private/phase).rglob('network.*.txt'):
                for line in log.read_text(errors='replace').splitlines():
                    if 'Sending: ' in line: sent+=1
                    match=re.search(r"Received: [\"']HTTP/1[.][01] ([0-9]{3})", line)
                    if match: counts[match[1]] += 1
            (public/(phase+'-http-counts.json')).write_text(json.dumps({'source':'Independent count of raw HTTP status lines; includes checkers and rerenders', 'sending_lines':sent,'received_status_lines':dict(counts)},indent=2))
        report['analysis_complete'] = False
        report['analyzer_disabled'] = True
        report['analysis_note'] = 'ResultsAnalyzer deshabilitado explícitamente tras fallo/timeout en macOS. Resúmenes del motor y conteo HTTP independiente disponibles; revisión incompleta.'
        report['completed']=True
    except Exception as exc:
        report['error']=f'{type(exc).__name__}: {exc}'
        raise
    finally:
        cleanup=[]
        for container in reversed(resources):
            try: docker('rm','-f','-v',container);cleanup.append(True)
            except Exception: cleanup.append(False)
        if network:
            for network_name in [name, name+'-entry']:
                try: docker('network','rm',network_name)
                except Exception: cleanup.append(False)
        if image:
            try: docker('image','rm',name)
            except Exception: cleanup.append(False)
        report['cleanup_success']=all(cleanup)
        report['finished_utc']=datetime.now(timezone.utc).isoformat()
        (public/'resumen.json').write_text(json.dumps(report,indent=2,ensure_ascii=False))
        # Archivos privados contienen SOLO secretos efímeros; no adjuntarlos a exposición ni repositorio.
        for file in private.rglob('*'):
            if file.is_file(): file.chmod(0o600)
        print(public)

if __name__=='__main__': main()
