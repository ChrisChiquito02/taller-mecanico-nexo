from pathlib import Path
import argparse
import subprocess,tempfile,secrets,base64,json,time,urllib.request
parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path);args=parser.parse_args()
r=Path(__file__).resolve().parents[1];name='examenerik-tls-'+secrets.token_hex(4);containers=[];network=False;volume=False
proof={'synthetic':True,'image':'examenerik-actividad-api','memory_limit_mb':512}
def docker(*a):return subprocess.check_output(['docker',*a],text=True,stderr=subprocess.PIPE).strip()
try:
 with tempfile.TemporaryDirectory(prefix='examenerik-tls-') as td:
  d=Path(td);tls=d/'tls';subprocess.run(['python3',str(r/'deploy/mysql/generar-tls.py'),'db.examenerik.test',str(tls)],check=True,stdout=subprocess.DEVNULL)
  docker('network','create',name);network=True;docker('volume','create',name);volume=True
  docker('run','--rm','-v',name+':/tls','-v',str(tls/'server')+':/source:ro','--entrypoint','sh','mysql:8.4','-c','cp /source/* /tls/ && chown -R mysql:mysql /tls && chmod 600 /tls/server-key.pem')
  pw=secrets.token_urlsafe(24)
  def env(file,values):
   p=d/file;p.write_text(''.join(f'{k}={v}\n' for k,v in values.items()));p.chmod(0o600);return str(p)
  db=docker('run','-d','--network',name,'--network-alias','db.examenerik.test','--tmpfs','/var/lib/mysql:rw,size=768m','-v',name+':/etc/mysql/tls:ro','-v',str(r/'deploy/mysql/tls.cnf')+':/etc/mysql/conf.d/tls.cnf:ro','--env-file',env('db.env',{'MYSQL_DATABASE':'identity','MYSQL_USER':'identity_app','MYSQL_PASSWORD':pw,'MYSQL_ROOT_PASSWORD':secrets.token_urlsafe(24)}),'mysql:8.4');containers.append(db)
  for _ in range(60):
   try:docker('exec',db,'mysqladmin','ping','--silent');break
   except subprocess.CalledProcessError:time.sleep(1)
  else:raise RuntimeError('MySQL no arrancó')
  api=docker('run','-d','--network',name,'--memory','512m','-p','127.0.0.1::8080','--read-only','--tmpfs','/tmp','--env-file',env('api.env',{
   'DB_URL':'jdbc:mysql://db.examenerik.test:3306/identity?sslMode=VERIFY_IDENTITY&serverTimezone=UTC','DB_USERNAME':'identity_app','DB_PASSWORD':pw,
   'DB_CA_CERT_BASE64':base64.b64encode((tls/'server/ca.pem').read_bytes()).decode(),'JWT_SIGNING_KEY':base64.b64encode(secrets.token_bytes(48)).decode(),
   'APP_ALLOWED_ORIGINS':'https://app.examenerik.test','APP_RECOVERY_BASE_URL':'https://app.examenerik.test/reset-password.html',
   'SMTP_HOST':'smtp.invalid','SMTP_FROM':'noreply@example.test','SMTP_AUTH':'true','SMTP_STARTTLS':'true','APP_COOKIE_SECURE':'true'}),'examenerik-actividad-api');containers.append(api)
  port=docker('port',api,'8080/tcp').split(':')[-1]
  for _ in range(90):
   try:
    with urllib.request.urlopen('http://127.0.0.1:'+port+'/api/health',timeout=2) as response:
     if response.status==200:proof['health']=json.load(response);break
   except Exception:pass
   time.sleep(1)
  else:raise RuntimeError('API TLS no arrancó')
  proof['mysql_tls']=docker('exec','-e','MYSQL_PWD='+pw,db,'mysql','--batch','--skip-column-names','-h','db.examenerik.test','-u','identity_app','--ssl-mode=VERIFY_IDENTITY','--ssl-ca=/etc/mysql/tls/ca.pem','-e',"SELECT VERSION(); SHOW STATUS LIKE 'Ssl_cipher'; SELECT version,success FROM identity.flyway_schema_history ORDER BY installed_rank;")
  bad=subprocess.run(['docker','exec','-e','MYSQL_PWD='+pw,db,'mysql','-h','127.0.0.1','-u','identity_app','--ssl-mode=VERIFY_IDENTITY','--ssl-ca=/etc/mysql/tls/ca.pem','-e','SELECT 1'],capture_output=True,text=True)
  assert bad.returncode!=0
  proof['wrong_hostname_rejected']=True;proof['wrong_hostname_error']=bad.stderr.strip()
  badca=subprocess.run(['docker','exec','-e','MYSQL_PWD='+pw,db,'mysql','-h','db.examenerik.test','-u','identity_app','--ssl-mode=VERIFY_IDENTITY','--ssl-ca=/etc/ssl/certs/ca-certificates.crt','-e','SELECT 1'],capture_output=True,text=True)
  assert badca.returncode!=0;proof['untrusted_ca_rejected']=True
  plain=subprocess.run(['docker','exec','-e','MYSQL_PWD='+pw,db,'mysql','-h','db.examenerik.test','-u','identity_app','--ssl-mode=DISABLED','-e','SELECT 1'],capture_output=True,text=True)
  assert plain.returncode!=0;proof['plaintext_rejected']=True
  try:urllib.request.urlopen('http://127.0.0.1:'+port+'/',timeout=2);raise RuntimeError('API-only sirvió frontend inesperadamente')
  except urllib.error.HTTPError as e:proof['frontend_root_status']=e.code
  proof['smtp_tested']=False;proof['passed']=True
finally:
 for c in reversed(containers):
  try:docker('rm','-f','-v',c)
  except Exception:pass
 if volume:docker('volume','rm',name)
 if network:docker('network','rm',name)
 (args.output or r/'documentacion/actividad/evidencia/mysql-tls.json').write_text(json.dumps(proof,indent=2))
print('PASS: API Java21 en 512MB, CA propia, VERIFY_IDENTITY, Flyway aplicado, rechazo de hostname/CA ajenos y conexión sin TLS.')
