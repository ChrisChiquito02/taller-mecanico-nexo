import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { JSDOM } from 'jsdom';
import { applyPermissions, routeAllowed, renderProfile, matchingPasswords } from '../../src/ui.js';
const html = await readFile(new URL('../../public/index.html', import.meta.url), 'utf8');
const doc = () => new JSDOM(html, { url: 'http://localhost/' }).window.document;
const user = { id: '123', email: 'qa@gmail.com', displayName: 'QA', status: 'ACTIVE', roles: ['USER'], permissions: ['profile:read'] };
test('rutas de cuenta y administración requieren sesión y permisos', () => {
  assert.equal(routeAllowed('account', null), 'login'); assert.equal(routeAllowed('admin', user), 'account');
  assert.equal(routeAllowed('admin', { ...user, permissions: ['users:read'] }), 'admin'); assert.equal(routeAllowed('reset', null, null), 'recovery');
});
test('permiso de lectura administrativa no muestra formularios de escritura', () => {
  const document = doc(); applyPermissions(document, { ...user, permissions: ['users:read'] });
  assert.equal(document.querySelector('[data-permission="users:read"]').hidden, false);
  assert.equal(document.querySelector('[data-permission="users:write"]').hidden, true);
});
test('logout oculta controles privilegiados y restaura navegación pública', () => {
  const document = doc(); applyPermissions(document, { ...user, permissions: ['users:read', 'users:write'] }); applyPermissions(document, null);
  assert.equal(document.querySelector('[data-member]').hidden, true); assert.equal(document.querySelector('[data-guest]').hidden, false);
  assert.ok([...document.querySelectorAll('[data-permission]')].every(el => el.hidden));
});
test('perfil trata contenido malicioso como texto, sin ejecutar HTML', () => {
  const document = doc(); renderProfile(document, { ...user, displayName: '<img src=x onerror=alert(1)>' });
  assert.equal(document.querySelector('#profile img'), null); assert.ok(document.querySelector('#profile').textContent.includes('<img'));
});
test('confirmación de contraseña bloquea diferencias y acepta igualdad', () => {
  const document = doc(); const form = document.querySelector('#register');
  form.elements.namedItem('password').value = 'Long-password-123'; form.elements.namedItem('confirmPassword').value = 'Other-password-123';
  assert.equal(matchingPasswords(form), false); form.elements.namedItem('confirmPassword').value = 'Long-password-123'; assert.equal(matchingPasswords(form), true);
});
test('formularios declaran restricciones, etiquetas y regiones accesibles', () => {
  const document = doc();
  for (const input of document.querySelectorAll('input')) assert.ok(input.closest('label'));
  assert.equal(document.querySelector('#register input[name=password]').minLength, 12);
  assert.equal(document.querySelector('#message').getAttribute('aria-live'), 'polite');
  assert.ok(document.querySelector('nav[aria-label]')); assert.ok(document.querySelector('a.skip-link')); assert.ok(document.querySelector('meta[name=viewport]'));
});
