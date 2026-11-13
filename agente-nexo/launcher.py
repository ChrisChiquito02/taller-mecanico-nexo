#!/usr/bin/env python3
"""Starts only ExamenErik's dedicated development stack; no production credentials."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import secrets
import shutil
import socket
import subprocess
import sys
import time
import urllib.request
import webbrowser

ROOT = Path(__file__).resolve().parents[1]
PRIVATE = ROOT / 'agente-nexo' / '.local'
COMPOSE = ROOT / 'deploy' / 'nexo-local.yml'
URL = 'http://localhost:8090'

def ready(path='/api/health'):
    try:
        with urllib.request.urlopen(URL+path, timeout=2) as r: return r.status == 200
    except Exception: return False

def run(*args):
    with (PRIVATE/'launcher.log').open('ab') as log:
        result = subprocess.run(args, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode: raise RuntimeError('Falló una dependencia. Revisa agente-nexo/.local/launcher.log; no compartas este registro sin revisarlo.')

def compose(*args): return ('docker','compose','--env-file',str(PRIVATE/'dev.env'),'-f',str(COMPOSE),*args)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--stop',action='store_true');parser.add_argument('--no-open',action='store_true');args=parser.parse_args()
    if not COMPOSE.is_file(): raise RuntimeError('Abre el lanzador dentro del proyecto completo ExamenErik. El ZIP antiguo solo contiene la demo.')
    for name in ['docker','node','npm']:
        if not shutil.which(name): raise RuntimeError(f'Falta {name}. Instala las dependencias descritas en la guía.')
    PRIVATE.mkdir(mode=0o700,exist_ok=True);PRIVATE.chmod(0o700)
    lock=(PRIVATE/'launcher.lock').open('w')
    try: fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
    except BlockingIOError: raise RuntimeError('Ya hay otro lanzador preparando NEXO. Espera a que termine.')
    run('docker','info','--format','{{.ServerVersion}}')
    if args.stop:
        if (PRIVATE/'dev.env').exists(): run(*compose('down'))
        print('Detenido ExamenErik NEXO local. Su base se conserva; los otros servicios siguen intactos.')
        return
    envfile=PRIVATE/'dev.env'
    if not envfile.exists():
        password=secrets.token_urlsafe(24)
        values={'DB_PASSWORD':secrets.token_hex(24),'MYSQL_ROOT_PASSWORD':secrets.token_hex(24),'JWT_SIGNING_KEY':secrets.token_urlsafe(48).replace('-','A').replace('_','B'),'DEMO_PASSWORD':password}
        with envfile.open('x') as f: f.write(''.join(f'{k}={v}\n' for k,v in values.items()))
        envfile.chmod(0o600)
        (PRIVATE/'Acceso local.txt').write_text(f'Entorno de desarrollo sintético. No compartir.\nCorreo: demo-admin@example.test\nContraseña: {password}\n')
        (PRIVATE/'Acceso local.txt').chmod(0o600)
    own=subprocess.check_output(compose('ps','-q','backend'),text=True).strip()
    with socket.socket() as s:
        occupied=s.connect_ex(('127.0.0.1',8090))==0
    if occupied and not own: raise RuntimeError('El puerto 8090 está ocupado por otro servicio. No se detuvo ni reemplazó.')
    source_files=[ROOT/'frontend/package-lock.json',ROOT/'frontend/scripts/build.mjs',ROOT/'sistema-m01-m02/pom.xml']
    for folder in ['frontend/src','frontend/public','sistema-m01-m02/src/main/java','sistema-m01-m02/src/main/resources/nexo','sistema-m01-m02/src/main/resources/db/migration']:
        source_files.extend(p for p in (ROOT/folder).rglob('*') if p.is_file())
    source_files.extend([ROOT/'sistema-m01-m02/src/main/resources/application.yml',COMPOSE])
    digest=hashlib.sha256(b''.join(str(p.relative_to(ROOT)).encode()+p.read_bytes() for p in sorted(source_files))).hexdigest()
    marker=PRIVATE/'build.sha256'
    image_exists=subprocess.run(['docker','image','inspect','examenerik-nexo-local'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0
    if not image_exists or not marker.exists() or marker.read_text()!=digest:
        print('Preparando la interfaz y el backend Java 21. Puede tardar unos minutos.',flush=True)
        run('npm','--prefix','frontend','ci')
        run('npm','--prefix','frontend','run','build:integrated')
        run('docker','run','--rm','-v',str(ROOT/'sistema-m01-m02')+':/workspace','-v',str(Path.home()/'.m2')+':/root/.m2','-w','/workspace','eclipse-temurin:21-jdk','./mvnw','-B','-ntp','package','-DskipTests')
        run('docker','build','-t','examenerik-nexo-local',str(ROOT/'sistema-m01-m02'))
        marker.write_text(digest)
    print('Iniciando únicamente los servicios locales de NEXO…',flush=True)
    run(*compose('up','-d','--wait','mysql','mailpit'))
    def scalar(sql):
        return subprocess.check_output(compose('exec','-T','mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot -N -B -e "$1"','sh',sql),text=True,stderr=subprocess.PIPE).strip()
    exists=scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='nexo_local' AND table_name='app_user'")=='1'
    count=int(scalar('SELECT COUNT(*) FROM nexo_local.app_user')) if exists else 0
    os.environ['NEXO_BOOTSTRAP_ENABLED']='true' if count==0 else 'false'
    run(*compose('up','-d','backend'))
    for _ in range(90):
        if ready(): break
        time.sleep(1)
    else: raise RuntimeError('El backend no respondió. Consulta el estado con docker compose y la guía local.')
    # Ollama is shared: never stop or replace a model service owned by the user.
    try:
        with urllib.request.urlopen('http://127.0.0.1:11434/api/tags',timeout=3) as r: models=json.load(r)
        if not any(m.get('name')=='qwen2.5-coder:7b' for m in models.get('models',[])): print('Falta el modelo: ejecuta ollama pull qwen2.5-coder:7b para instalarlo.')
    except Exception: print('La página está lista, pero Ollama no responde. Abre Ollama para habilitar la IA.')
    print(f'ExamenErik + NEXO-Δ AI: {URL}/#nexo\nAcceso de demostración: agente-nexo/.local/Acceso local.txt\nPuedes cerrar Terminal: Docker mantiene los servicios. Para detenerlos abre Detener NEXO.command.',flush=True)
    if not args.no_open: webbrowser.open(URL+'/#nexo')

if __name__=='__main__':
    try: main()
    except (RuntimeError,subprocess.CalledProcessError) as e:
        print('No se pudo abrir NEXO:',str(e),file=sys.stderr);sys.exit(1)
