import { execFileSync } from 'node:child_process';
import { randomBytes, randomUUID } from 'node:crypto';
import { mkdtempSync, writeFileSync, existsSync, rmSync, mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { createServer } from 'node:net';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../../../', import.meta.url));
const docker = (...args) => execFileSync('docker', args, { encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }).trim();
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
async function port() {
  const server = createServer(); await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const chosen = server.address().port; await new Promise(resolve => server.close(resolve)); return chosen;
}
export default async function setup() {
  if (!existsSync(path.join(root, 'sistema-m01-m02/target/backend-0.1.0-SNAPSHOT.jar'))) throw new Error('Construye el JAR antes de E2E');
  const name = `examenerik-e2e-${randomUUID().slice(0, 8)}`;
  const directory = mkdtempSync(path.join(tmpdir(), name));
  const containers = []; let network = false, image = false;
  const cleanup = () => {
    for (const id of [...containers].reverse()) { try { docker('rm', '-f', '-v', id); } catch {} }
    if (network) { try { docker('network', 'rm', name); } catch {} }
    if (image) { try { docker('image', 'rm', name); } catch {} }
    rmSync(directory, { recursive: true, force: true });
  };
  const envfile = (filename, values) => { const file = path.join(directory, filename); writeFileSync(file, Object.entries(values).map(([key, value]) => `${key}=${value}\n`).join(''), { mode: 0o600 }); return file; };
  try {
    docker('network', 'create', name); network = true;
    const password = randomBytes(24).toString('hex');
    containers.push(docker('run', '-d', '--network', name, '--network-alias', 'mysql', '--tmpfs', '/var/lib/mysql:rw,size=768m', '--env-file', envfile('mysql.env', {
      MYSQL_DATABASE: 'identity_e2e', MYSQL_USER: 'identity_e2e', MYSQL_PASSWORD: password, MYSQL_ROOT_PASSWORD: randomBytes(24).toString('hex')
    }), 'mysql:8.4'));

    let databaseReady = false;
    for (let i = 0; i < 90; i++) {
      try { docker('exec', containers[0], 'mysqladmin', 'ping', '-h', '127.0.0.1', '--silent'); databaseReady = true; break; } catch {}
      await delay(1000);
    }
    if (!databaseReady) throw new Error('MySQL E2E no arrancó');
    const mail = docker('run', '-d', '--network', name, '--network-alias', 'mailpit', '-p', '127.0.0.1::8025', 'axllent/mailpit:v1.27'); containers.push(mail);
    const mailPort = docker('port', mail, '8025/tcp').split(':').at(-1);
    const base = `http://127.0.0.1:${await port()}`;
    const adminPassword = randomBytes(24).toString('base64url');
    // Solo se empaqueta el JAR ya probado. No se omiten pruebas en una segunda compilación.
    docker('build', '-q', '-t', name, path.join(root, 'sistema-m01-m02')); image = true;
    const application = docker('run', '-d', '--network', name, '-p', `127.0.0.1:${new URL(base).port}:8080`, '--read-only', '--tmpfs', '/tmp', '--env-file', envfile('app.env', {
      SPRING_PROFILES_ACTIVE: 'local', DB_URL: 'jdbc:mysql://mysql:3306/identity_e2e?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false', DB_USERNAME: 'identity_e2e', DB_PASSWORD: password,
      JWT_SIGNING_KEY: randomBytes(48).toString('base64'), APP_ALLOWED_ORIGINS: [base, process.env.E2E_ADDITIONAL_ORIGIN].filter(Boolean).join(','), APP_COOKIE_SECURE: process.env.E2E_COOKIE_SECURE || 'false', APP_RECOVERY_MODE: 'development', APP_RECOVERY_BASE_URL: `${base}/reset-password.html`,
      SMTP_HOST: 'mailpit', SMTP_PORT: '1025', SMTP_AUTH: 'false', SMTP_STARTTLS: 'false', SMTP_FROM: 'noreply@example.test',
      APP_BOOTSTRAP_ENABLED: 'true', APP_BOOTSTRAP_EMAIL: 'qa-admin@gmail.com', APP_BOOTSTRAP_PASSWORD: adminPassword,
      AUTH_REQUESTS_PER_MINUTE: '10000', MAX_FAILED_ATTEMPTS: '3'
    }), name); containers.push(application);
    let ready = false;
    for (let i = 0; i < 90; i++) {
      try {
        const health = await fetch(`${base}/api/health`, { signal: AbortSignal.timeout(2000) });
        if (health.ok) {
          const auth = await fetch(`${base}/api/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: 'qa-admin@gmail.com', password: adminPassword }), signal: AbortSignal.timeout(3000) });
          if (auth.ok) { ready = true; break; }
        }
      } catch {}
      await delay(1000);
    }
    if (!ready) throw new Error('El entorno E2E aislado no quedó preparado (sin registrar secretos)');
    process.env.E2E_BASE_URL = base; process.env.E2E_MAIL_URL = `http://127.0.0.1:${mailPort}`;
    process.env.E2E_ADMIN_PASSWORD = adminPassword;
    // Solo metadatos sin credenciales ni cuerpos HTTP.
    mkdirSync(path.join(root, 'frontend/test-results'), { recursive: true });
    writeFileSync(path.join(root, 'frontend/test-results/environment.json'), JSON.stringify({ isolated: true, mysql: '8.4', smtp: 'Mailpit', containerUser: docker('inspect', '--format', '{{.Config.User}}', application) }, null, 2));
    return cleanup;
  } catch (error) { cleanup(); throw error; }
}
