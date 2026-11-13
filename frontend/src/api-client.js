import { validateApiBase } from './api-base.js';
const publicAuth = /^\/auth\/(register|login|refresh|logout|forgot-password|reset-password)$/;
export class ApiError extends Error {
  constructor(message, status = 0, code = 'NETWORK_ERROR') { super(message); this.name = 'ApiError'; this.status = status; this.code = code; }
}
export function accessContract(value) {
  return value && typeof value.accessToken === 'string' && value.accessToken.length > 0
    && value.tokenType === 'Bearer' && Number.isFinite(value.expiresIn) && value.expiresIn > 0;
}
export function userContract(value) {
  return value && typeof value.id === 'string' && typeof value.email === 'string'
    && typeof value.displayName === 'string' && typeof value.status === 'string'
    && Array.isArray(value.roles) && value.roles.every(v => typeof v === 'string')
    && Array.isArray(value.permissions) && value.permissions.every(v => typeof v === 'string');
}
export function createApiClient({ fetchImpl = globalThis.fetch, base = '/api', timeout = 20000, onSessionEnd = () => {}, refreshLock = task => globalThis.navigator?.locks ? globalThis.navigator.locks.request('examenerik-refresh', task) : task() } = {}) {
  validateApiBase(base);
  let accessToken = null;
  let refreshPending = null;
  const clear = () => { accessToken = null; onSessionEnd(); };
  async function send(path, data, method, protectedRequest) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeout);
    try {
      const headers = {};
      if (data !== undefined) headers['Content-Type'] = 'application/json';
      if (accessToken && protectedRequest) headers.Authorization = `Bearer ${accessToken}`;
      const response = await fetchImpl(`${base}${path}`, { method, headers, credentials: 'include',
        signal: controller.signal, body: data === undefined ? undefined : JSON.stringify(data) });
      let body = null;
      if (response.status !== 204) {
        try { body = await response.json(); }
        catch { throw new ApiError('Respuesta inesperada del servidor', response.status, 'INVALID_RESPONSE'); }
      }
      return { response, body };
    } catch (error) {
      if (error instanceof ApiError) throw error;
      if (controller.signal.aborted) throw new ApiError('El servidor tardó demasiado. Comprueba el resultado antes de repetir la operación.', 0, 'TIMEOUT');
      throw new ApiError('No se pudo conectar con el servidor. Inténtalo de nuevo.', 0, 'NETWORK_ERROR');
    } finally { clearTimeout(timer); }
  }
  async function refresh() {
    if (!refreshPending) refreshPending = Promise.resolve().then(() => refreshLock(async () => {
      const body = await request('/auth/refresh', {}, 'POST', false);
      if (!accessContract(body)) throw new ApiError('Respuesta de sesión inválida', 502, 'INVALID_RESPONSE');
      accessToken = body.accessToken;
      return body;
    })).finally(() => { refreshPending = null; });
    return refreshPending;
  }
  async function request(path, data, method = 'POST', retry = true) {
    if (!path.startsWith('/') || path.startsWith('//')) throw new Error('Ruta de API inválida');
    const protectedRequest = !publicAuth.test(path) && path !== '/health';
    const { response, body } = await send(path, data, method, protectedRequest);
    if (response.status === 401 && protectedRequest && body?.code === 'UNAUTHORIZED') {
      if (retry) {
        try { await refresh(); } catch (error) { clear(); throw error; }
        return request(path, data, method, false);
      }
      clear();
    }
    if (!response.ok) throw new ApiError(typeof body?.title === 'string' ? body.title.slice(0, 220) : 'Solicitud rechazada', response.status, body?.code || 'HTTP_ERROR');
    return body;
  }
  async function stream(path, data, onEvent, signal, retry = true) {
    if (!/^\/nexo\/conversations\/[0-9a-f-]{36}\/messages$/.test(path)) throw new Error('Ruta de streaming inválida');
    const controller = new AbortController();
    const abort = () => controller.abort();
    signal?.addEventListener('abort', abort, { once: true });
    if (signal?.aborted) controller.abort();
    const timer = setTimeout(abort, 390000);
    let reader;
    try {
      const response = await fetchImpl(`${base}${path}`, { method: 'POST', credentials: 'include',
        headers: { 'Content-Type': 'application/json', ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}) },
        body: JSON.stringify(data), signal: controller.signal });
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        if (response.status === 401 && retry && body.code === 'UNAUTHORIZED') {
          try { await refresh(); } catch (e) { clear(); throw e; }
          return await stream(path, data, onEvent, signal, false);
        }
        if (response.status === 401) clear();
        throw new ApiError(body.title || 'No se pudo iniciar la respuesta', response.status, body.code);
      }
      reader = response.body.getReader(); const decoder = new TextDecoder(); let buffer = '', done = false, size = 0;
      while (true) {
        const part = await reader.read(); buffer += decoder.decode(part.value, { stream: !part.done });
        size += part.value?.length || 0;
        if (size > 2000000 || buffer.length > 500000) throw new Error('Respuesta fuera de límite');
        let boundary;
        while ((boundary = buffer.indexOf('\n')) >= 0) {
          const raw = buffer.slice(0, boundary); buffer = buffer.slice(boundary + 1); if (!raw.trim()) continue;
          const event = JSON.parse(raw); if (event.type === 'error') { if (event.code === 'UNAUTHORIZED') clear(); throw new ApiError(event.text, event.code === 'UNAUTHORIZED' ? 401 : 503, event.code); }
          if (event.type === 'done' || event.type === 'truncated') done = true; onEvent(event);
        }
        if (part.done) break;
      }
      if (!done) throw new Error('Respuesta interrumpida: no se confirmó su finalización');
    } finally { await reader?.cancel().catch(() => {}); clearTimeout(timer); signal?.removeEventListener('abort', abort); }
  }
  return {
    request, refresh, clear, stream,
    setSession(body) { if (!accessContract(body)) throw new ApiError('Respuesta de sesión inválida', 502, 'INVALID_RESPONSE'); accessToken = body.accessToken; },
    async profile() { const body = await request('/users/me', undefined, 'GET'); if (!userContract(body)) throw new ApiError('Perfil inválido', 502, 'INVALID_RESPONSE'); return body; },
    async users(page, size = 10) { const body = await request(`/admin/users?page=${page}&size=${size}`, undefined, 'GET'); if (!Array.isArray(body) || !body.every(userContract)) throw new ApiError('Listado inválido', 502, 'INVALID_RESPONSE'); return body; }
  };
}
