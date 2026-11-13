import { test, expect } from '@playwright/test';
import { randomBytes, randomUUID } from 'node:crypto';
import { writeFileSync } from 'node:fs';
const base = () => process.env.E2E_BASE_URL;
const account = () => ({ email: `qa-${randomUUID()}@gmail.com`, password: randomBytes(18).toString('base64url'), displayName: 'Usuario de prueba' });
async function register(request, user) { const result = await request.post(`${base()}/api/auth/register`, { data: user }); expect(result.status()).toBe(202); const login = await request.post(`${base()}/api/auth/login`, { data: { email: user.email, password: user.password } }); expect(login.status()).toBe(200); const access = await login.json(); const profile = await request.get(`${base()}/api/users/me`, { headers: { Authorization: `Bearer ${access.accessToken}` } }); expect(profile.status()).toBe(200); return profile.json(); }
async function login(page, user) {
  await page.goto(`${base()}/#login`);
  const form = page.locator('#login'); await form.getByLabel('Correo', { exact: true }).fill(user.email); await form.getByLabel('Contraseña', { exact: true }).fill(user.password); await form.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page.locator('#view-account')).toBeVisible();
}
test('registro valida contraseñas y navega a perfil real sin interpretar HTML', async ({ page }) => {
  const user = { ...account(), displayName: '<img src=x onerror=alert(1)>' };
  await page.goto(`${base()}/#register`); const form = page.locator('#register');
  await form.getByLabel('Nombre', { exact: true }).fill(user.displayName); await form.getByLabel('Correo', { exact: true }).fill(user.email);
  await form.getByLabel('Contraseña · mínimo 12 caracteres', { exact: true }).fill(user.password); await form.getByLabel('Confirmar contraseña', { exact: true }).fill('Other-password-123!'); await form.getByRole('button').click();
  await expect(page.locator('#view-register')).toBeVisible();
  await form.getByLabel('Confirmar contraseña', { exact: true }).fill(user.password); await form.getByRole('button').click(); await expect(page.locator('#message')).toContainText('Solicitud de registro procesada');
  await login(page, user); await expect(page.locator('#profile')).toContainText(user.displayName); await expect(page.locator('#profile img')).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Administración', exact: true })).toBeHidden();
  const sensitiveStorage = await page.evaluate(() => [...Object.keys(localStorage), ...Object.keys(sessionStorage)]); expect(sensitiveStorage).toEqual([]);
  await page.getByRole('button', { name: 'Cerrar todas mis sesiones' }).click(); await expect(page.locator('#message')).toContainText('Sesiones cerradas');
});
test('cuenta bloqueada rechaza contraseña correcta y no muestra datos internos', async ({ page, request }) => {
  const user = account(); await register(request, user); await page.goto(`${base()}/#login`); const form = page.locator('#login');
  for (let i = 0; i < 3; i++) {
    await form.getByLabel('Correo', { exact: true }).fill(user.email); await form.getByLabel('Contraseña', { exact: true }).fill('Incorrect-password-123!'); await form.getByRole('button').click();
    await expect(page.locator('#message')).toContainText('Credenciales o sesión inválidas'); await expect(form.getByRole('button')).toBeEnabled();
  }
  await form.getByLabel('Contraseña', { exact: true }).fill(user.password); await form.getByRole('button').click();
  await expect(page.locator('#message')).toContainText('Credenciales o sesión inválidas'); await expect(page.locator('#view-account')).toBeHidden();
});
test('contraseña actual incorrecta conserva sesión y el cambio correcto la revoca', async ({ page, request }) => {
  const user = account(); await register(request, user); await login(page, user);
  const form = page.locator('#change'); const next = randomBytes(18).toString('base64url');
  await form.getByLabel('Contraseña actual', { exact: true }).fill('Wrong-password-123!'); await form.getByLabel('Nueva contraseña', { exact: true }).fill(next); await form.getByLabel('Confirmar contraseña', { exact: true }).fill(next); await form.getByRole('button').click();
  await expect(page.locator('#message')).toContainText('Credenciales o sesión inválidas'); await expect(page.locator('#view-account')).toBeVisible();
  await form.getByLabel('Contraseña actual', { exact: true }).fill(user.password); await form.getByRole('button').click();
  await expect(page.locator('#message')).toContainText('Todas tus sesiones se cerraron'); await login(page, { ...user, password: next });
});
test('recuperación usa SMTP real, consume enlace y permite acceder con la nueva contraseña', async ({ page, request }) => {
  const user = account(); await register(request, user); await page.goto(`${base()}/#recovery`);
  await page.locator('#forgot').getByLabel('Correo', { exact: true }).fill(user.email); await page.locator('#forgot').getByRole('button').click();
  await expect(page.locator('#message')).toContainText('Si la cuenta es elegible');
  let message;
  await expect.poll(async () => { const inbox = await (await request.get(`${process.env.E2E_MAIL_URL}/api/v1/messages`)).json(); message = inbox.messages.find(m => m.To.some(to => to.Address === user.email)); return Boolean(message); }).toBe(true);
  const detail = await (await request.get(`${process.env.E2E_MAIL_URL}/api/v1/message/${message.ID}`)).json();
  const link = detail.Text.match(/http[^\s]+#token=[A-Za-z0-9_-]{43}/)?.[0]; expect(Boolean(link)).toBe(true);
  await page.goto(link); await expect(page.locator('#view-reset')).toBeVisible(); expect(new URL(page.url()).hash).toBe('#reset');
  const form = page.locator('#reset'); const password = randomBytes(18).toString('base64url');
  await form.getByLabel('Nueva contraseña', { exact: true }).fill(password); await form.getByLabel('Confirmar contraseña', { exact: true }).fill(password); await form.getByRole('button').click();
  await expect(page.locator('#message')).toContainText('Contraseña actualizada'); await login(page, { ...user, password });
});
test('administración pagina, asigna roles y desactiva con persistencia real', async ({ page, request }) => {
  const target = account(); const registered = await register(request, target);
  // Página completa fuerza la navegación paginada real, con usuarios únicos de prueba.
  for (let i = 0; i < 11; i++) await register(request, account());
  await login(page, { email: 'qa-admin@gmail.com', password: process.env.E2E_ADMIN_PASSWORD });
  await page.getByRole('link', { name: 'Administración', exact: true }).click(); await page.getByRole('button', { name: 'Consultar usuarios' }).click();
  await expect(page.locator('#next-page')).toBeEnabled(); await page.locator('#next-page').click(); await expect(page.locator('#page-number')).toContainText('Página 2'); await page.locator('#previous-page').click(); await expect(page.locator('#page-number')).toContainText('Página 1');
  await page.locator('#roles input[name=id]').fill(registered.id); await page.locator('#roles input[name=roles]').fill('ADMIN'); await page.locator('#roles button').click(); await expect(page.locator('#message')).toContainText('Permisos actualizados');
  const authenticated = await request.post(`${base()}/api/auth/login`, { data: { email: target.email, password: target.password } }); const body = await authenticated.json();
  expect((await request.get(`${base()}/api/admin/users`, { headers: { Authorization: `Bearer ${body.accessToken}` } })).status()).toBe(200);
  await page.locator('#disable input[name=id]').fill(registered.id); page.once('dialog', dialog => dialog.accept()); await page.locator('#disable button').click(); await expect(page.locator('#message')).toContainText('Cuenta desactivada');
  expect((await request.get(`${base()}/api/users/me`, { headers: { Authorization: `Bearer ${body.accessToken}` } })).status()).toBe(401);
});
test('rutas privadas redirigen al login y diseño móvil no desborda', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 }); await page.goto(`${base()}/#admin`); await expect(page.locator('#view-login')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByRole('link', { name: 'Crear cuenta', exact: true }).click(); await expect(page.locator('#view-register')).toBeVisible(); await page.goBack(); await expect(page.locator('#view-login')).toBeVisible();
});
test('dos pestañas restauran sesión sin reutilizar simultáneamente el refresh', async ({ page, context, request }) => {
  const user = account(); await register(request, user); await login(page, user); const second = await context.newPage();
  await Promise.all([page.reload(), second.goto(`${base()}/#account`)]);
  await expect(page.locator('#view-account')).toBeVisible(); await expect(second.locator('#view-account')).toBeVisible();
  await page.reload(); await expect(page.locator('#view-account')).toBeVisible(); await second.close();
});
test('evidencia visual final y controles de carga disponibles', async ({ page, request, browser }) => {
  writeFileSync('test-results/browser.json', JSON.stringify({ version: browser.version(), channel: process.env.PW_CHANNEL || 'chromium' }, null, 2));
  const user = account(); await register(request, user); await login(page, user);
  await page.getByRole('button', { name: 'Cerrar todas mis sesiones' }).click(); await expect(page.locator('#message')).toContainText('Sesiones cerradas');
  await expect(page.locator('#health')).toContainText('Servicio disponible');
  await page.screenshot({ path: '../documentacion/auditoria/interfaz-final.png', fullPage: true });
});
