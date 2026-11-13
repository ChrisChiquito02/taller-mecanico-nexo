import test from 'node:test';
import assert from 'node:assert/strict';
import { createApiClient, ApiError } from '../../src/api-client.js';
const access = { accessToken: 'synthetic-access', tokenType: 'Bearer', expiresIn: 900 };
const user = { id: 'test', email: 'test@gmail.com', displayName: 'Test', status: 'ACTIVE', roles: ['USER'], permissions: ['profile:read'] };
const response = (body, status = 200) => new Response(body === null ? null : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
test('envía Bearer solo a operaciones protegidas y mantiene cookie entre orígenes autorizados', async () => {
  const calls = []; const api = createApiClient({ fetchImpl: async (url, options) => { calls.push({ url, options }); return response(user); } });
  api.setSession(access); await api.request('/users/me', undefined, 'GET'); await api.request('/auth/logout', {});
  assert.equal(calls[0].options.headers.Authorization, 'Bearer synthetic-access'); assert.equal(calls[1].options.headers.Authorization, undefined);
  assert.equal(calls[1].options.credentials, 'include'); assert.equal(calls[1].options.headers['Content-Type'], 'application/json');
});
test('contraseña actual incorrecta no renueva ni borra la sesión', async () => {
  let ended = 0; const calls = [];
  const api = createApiClient({ onSessionEnd: () => ended++, fetchImpl: async (url, options) => { calls.push({ url, options }); return url.endsWith('change-password') ? response({ code: 'INVALID_CREDENTIALS', title: 'Credencial incorrecta' }, 401) : response(user); } });
  api.setSession(access); await assert.rejects(api.request('/auth/change-password', {}), error => error.status === 401);
  await api.profile(); assert.equal(ended, 0); assert.equal(calls.length, 2); assert.equal(calls[1].options.headers.Authorization, 'Bearer synthetic-access');
});
test('renovación concurrente se realiza una sola vez y reintenta con acceso nuevo', async () => {
  let refreshes = 0;
  const api = createApiClient({ fetchImpl: async (url, options) => {
    if (url.endsWith('/refresh')) { refreshes++; await new Promise(resolve => setTimeout(resolve, 10)); return response(access); }
    return options.headers.Authorization ? response(user) : response({ code: 'UNAUTHORIZED' }, 401);
  } });
  const result = await Promise.all([api.profile(), api.profile()]); assert.equal(result.length, 2); assert.equal(refreshes, 1);
});
test('401 Bearer después de renovar cierra sesión sin bucle', async () => {
  let calls = 0, ended = 0;
  const api = createApiClient({ onSessionEnd: () => ended++, fetchImpl: async url => { calls++; return url.endsWith('/refresh') ? response(access) : response({ code: 'UNAUTHORIZED' }, 401); } });
  await assert.rejects(api.profile(), error => error.status === 401); assert.equal(calls, 3); assert.equal(ended, 1);
});
test('403 conserva sesión y no intenta renovar permisos', async () => {
  let calls = 0, ended = 0; const api = createApiClient({ onSessionEnd: () => ended++, fetchImpl: async () => { calls++; return response({ code: 'FORBIDDEN', title: 'Sin permiso' }, 403); } });
  api.setSession(access); await assert.rejects(api.users(0), error => error.status === 403); assert.equal(calls, 1); assert.equal(ended, 0);
});
test('timeout informa resultado incierto y no repite una mutación', async () => {
  let calls = 0;
  const api = createApiClient({ timeout: 5, fetchImpl: async (_, options) => { calls++; return new Promise((resolve, reject) => options.signal.addEventListener('abort', () => reject(new Error('abort')))); } });
  await assert.rejects(api.request('/auth/register', {}), error => error instanceof ApiError && error.code === 'TIMEOUT'); assert.equal(calls, 1);
});
test('respuesta HTML no revela su contenido como error al usuario', async () => {
  const api = createApiClient({ fetchImpl: async () => new Response('<html>detalle interno sensible</html>', { status: 500 }) });
  await assert.rejects(api.request('/auth/login', {}), error => error.code === 'INVALID_RESPONSE' && !error.message.includes('sensible'));
});
test('rechaza contratos de acceso y perfil inválidos', async () => {
  const api = createApiClient({ fetchImpl: async () => response({ email: 'incomplete' }) });
  assert.throws(() => api.setSession({ accessToken: 1 }), /inválida/); await assert.rejects(api.profile(), /Perfil inválido/);
});
test('rechaza HTTP externo y no reintenta fallos de red', async () => {
  assert.throws(() => createApiClient({ base: 'http://external.test/api' }));
  let calls = 0; const api = createApiClient({ fetchImpl: async () => { calls++; throw new TypeError('private-network-details'); } });
  await assert.rejects(api.request('/auth/login', {}), error => error.code === 'NETWORK_ERROR' && !error.message.includes('private-network')); assert.equal(calls, 1);
});
test('usa exclusión mutua configurada para renovar entre pestañas', async () => {
  let locked = 0; const api = createApiClient({ refreshLock: async task => { locked++; return task(); }, fetchImpl: async () => response(access) });
  await api.refresh(); assert.equal(locked, 1);
});

test('API HTTPS fija permite CORS con credenciales y rechaza destinos ambiguos', async () => {
  const api = createApiClient({ base: 'https://api.example.test/api', fetchImpl: async (url, options) => {
    assert.equal(url, 'https://api.example.test/api/users/me');
    assert.equal(options.credentials, 'include');
    return response(user);
  } });
  await api.profile();
  for (const base of ['//evil.test/api', 'https://u:p@evil.test/api', 'https://api.test/api?x=1', 'https://api.test/api#x', 'https://api.test/api/'])
    assert.throws(() => createApiClient({ base }));
});
