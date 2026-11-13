#!/usr/bin/env python3
"""Comprueba el JAR en Java 21 y MySQL desechable. No lee .env ni usa datos existentes."""
import base64
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / 'target/backend-0.1.0-SNAPSHOT.jar'

def docker(*args):
    return subprocess.check_output(['docker', *args], text=True, stderr=subprocess.PIPE).strip()

def main():
    if not JAR.exists():
        raise SystemExit('Primero ejecuta ./mvnw verify')
    name = 'examenerik-smoke-' + uuid.uuid4().hex[:10]
    containers = []
    created_network = False
    try:
        docker('network', 'create', name)
        created_network = True
        with tempfile.TemporaryDirectory(prefix=name) as directory:
            temporary = Path(directory)
            snapshot = temporary / "app.jar"
            shutil.copyfile(JAR, snapshot)
            password = secrets.token_urlsafe(24)
            def envfile(filename, values):
                path = temporary / filename
                path.write_text(''.join(f'{k}={v}\n' for k, v in values.items()))
                path.chmod(0o600)
                return str(path)
            database = docker('run', '-d', '--network', name, '--network-alias', 'mysql',
                '--tmpfs', '/var/lib/mysql:rw,size=768m', '--env-file', envfile('mysql.env', {
                    'MYSQL_DATABASE': 'identity_smoke', 'MYSQL_USER': 'identity_smoke',
                    'MYSQL_PASSWORD': password, 'MYSQL_ROOT_PASSWORD': secrets.token_urlsafe(24)
                }), 'mysql:8.4')
            containers.append(database)
            for _ in range(90):
                result = subprocess.run(['docker', 'exec', database, 'mysqladmin', 'ping', '-h', '127.0.0.1', '--silent'], capture_output=True)
                if result.returncode == 0:
                    break
                time.sleep(1)
            else:
                raise RuntimeError('MySQL aislado no arrancó')
            admin_password = secrets.token_urlsafe(24)
            app = docker('run', '-d', '--network', name, '-p', '127.0.0.1::8080',
                '-v', f'{snapshot}:/app/app.jar:ro', '--env-file', envfile('app.env', {
                    'SPRING_PROFILES_ACTIVE': 'local',
                    'DB_URL': 'jdbc:mysql://mysql:3306/identity_smoke?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false',
                    'DB_USERNAME': 'identity_smoke', 'DB_PASSWORD': password,
                    'JWT_SIGNING_KEY': base64.b64encode(secrets.token_bytes(48)).decode(),
                    'APP_ALLOWED_ORIGINS': 'http://localhost:5173', 'APP_COOKIE_SECURE': 'false',
                    'APP_RECOVERY_BASE_URL': 'http://localhost:5173/reset-password',
                    'SMTP_HOST': 'localhost', 'SMTP_AUTH': 'false', 'SMTP_STARTTLS': 'false',
                    'SMTP_FROM': 'noreply@example.test',
                    'APP_BOOTSTRAP_ENABLED': 'true', 'APP_BOOTSTRAP_EMAIL': 'smoke-admin@gmail.com',
                    'APP_BOOTSTRAP_PASSWORD': admin_password
                }), 'eclipse-temurin:21-jdk', 'java', '-jar', '/app/app.jar')
            containers.append(app)
            port = docker('port', app, '8080/tcp').rsplit(':', 1)[1]
            origin = f'http://127.0.0.1:{port}'
            opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
            def request(path, data=None, method=None, token=None):
                headers = {'Content-Type': 'application/json', 'Origin': 'http://localhost:5173'}
                if token:
                    headers['Authorization'] = f'Bearer {token}'
                req = urllib.request.Request(origin + path, data=None if data is None else json.dumps(data).encode(), headers=headers, method=method)
                with opener.open(req, timeout=5) as response:
                    raw = response.read()
                    return response.status, raw
            for _ in range(90):
                try:
                    if request('/api/health')[0] == 200:
                        # ApplicationRunner finaliza después de que el puerto HTTP está abierto.
                        try:
                            status, body = request('/api/auth/login', {'email': 'smoke-admin@gmail.com', 'password': admin_password})
                            if status == 200:
                                break
                        except urllib.error.HTTPError:
                            pass
                except (OSError, urllib.error.URLError):
                    pass
                time.sleep(1)
            else:
                raise RuntimeError('JAR aislado no quedó preparado')
            token = json.loads(body)['accessToken']
            assert request('/api/admin/users', token=token)[0] == 200
            assert request('/')[0] == 200
            email = 'smoke-user@gmail.com'
            user_password = secrets.token_urlsafe(24)
            status, body = request('/api/auth/register', {'email': email, 'password': user_password, 'displayName': 'Smoke user'})
            assert status == 202
            _, credentials = request('/api/auth/login', {'email': email, 'password': user_password})
            _, profile = request('/api/users/me', token=json.loads(credentials)['accessToken'])
            user_id = json.loads(profile)['id']
            assert request(f'/api/admin/users/{user_id}/roles', {'roles': ['ADMIN']}, 'PUT', token)[0] == 200
            status, body = request('/api/auth/login', {'email': email, 'password': user_password})
            assert status == 200
            user_token = json.loads(body)['accessToken']
            assert request('/api/users/me', token=user_token)[0] == 200
            assert request('/api/admin/users', token=user_token)[0] == 200
            assert request('/api/auth/refresh', {})[0] == 200
            assert request('/api/auth/logout', {})[0] == 204
            try:
                request('/api/users/me', token=user_token)
                raise AssertionError('La sesión no fue revocada')
            except urllib.error.HTTPError as error:
                assert error.code == 401
            docker('restart', app)
            # Bootstrap no debe volver a ejecutarse sobre usuarios existentes.
            exit_code = subprocess.check_output(['docker', 'wait', app], text=True, timeout=60).strip()
            assert exit_code == '1', 'Bootstrap repetido no fue rechazado'
            print('PASS: bootstrap repetido rechazado; JAR Java 21, MySQL vacío, Flyway, bootstrap ADMIN, interfaz HTTP, registro, login, RBAC, refresh y logout.')
    finally:
        for container in reversed(containers):
            subprocess.run(['docker', 'rm', '-f', '-v', container], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if created_network:
            subprocess.run(['docker', 'network', 'rm', name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

if __name__ == '__main__':
    main()
