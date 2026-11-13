import { mountNexo } from './nexo.js';
import { createApiClient } from './api-client.js';
import { applyPermissions, can, matchingPasswords, renderProfile, routeAllowed } from './ui.js';
const $ = selector => document.querySelector(selector);
let user = null;
let nexo = null;
let resetToken = new URLSearchParams(location.hash.slice(1)).get('token');
let currentPage = 0;
let restoring = true;
function notify(text, error = false) {
  const element = $('#message'); element.textContent = text; element.classList.toggle('error', error);
  element.setAttribute('role', error ? 'alert' : 'status'); element.setAttribute('aria-live', error ? 'assertive' : 'polite');
}
function clearSession() { nexo?.reset(); user = null; applyPermissions(document, null); $('#profile').replaceChildren(); $('#users').replaceChildren(); if (!restoring) navigate('login'); }
const api = createApiClient({ base: __API_BASE__, onSessionEnd: clearSession });
function navigate(requested, replace = false) {
  const route = routeAllowed(requested, user, resetToken);
  if (route === 'nexo') nexo?.open();
  document.querySelector('.intro').hidden = route === 'nexo';
  for (const section of document.querySelectorAll('[data-view]')) section.hidden = section.dataset.view !== route;
  for (const link of document.querySelectorAll('nav a')) {
    if (link.getAttribute('href') === `#${route}`) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
  }
  if (replace) history.replaceState(null, '', `#${route}`); else if (location.hash !== `#${route}`) history.pushState(null, '', `#${route}`);
  $(`#view-${route} h2`)?.focus({ preventScroll: true });
}
async function loadProfile() { user = await api.profile(); renderProfile(document, user); applyPermissions(document, user); }
function form(id, action) {
  const element = $(`#${id}`);
  element.addEventListener('input', () => { element.elements.namedItem('confirmPassword')?.setCustomValidity(''); });
  element.addEventListener('submit', async event => {
    event.preventDefault();
    if (!matchingPasswords(element) || !element.reportValidity()) { element.reportValidity(); return; }
    const button = element.querySelector('button'); if (button.disabled) return;
    const label = button.textContent; button.disabled = true; button.textContent = 'Procesando…'; element.setAttribute('aria-busy', 'true');
    const data = Object.fromEntries(new FormData(element)); delete data.confirmPassword;
    try { await action(data); element.reset(); } catch (error) { notify(error.message, true); }
    finally { button.disabled = false; button.textContent = label; element.removeAttribute('aria-busy'); }
  });
}
form('login', async data => { api.setSession(await api.request('/auth/login', data)); await loadProfile(); navigate('account'); notify('Sesión iniciada.'); });
form('register', async data => { await api.request('/auth/register', data); navigate('login'); notify('Solicitud de registro procesada. Puedes intentar iniciar sesión o recuperar el acceso.'); });
form('forgot', async data => { const result = await api.request('/auth/forgot-password', data); notify(result.message || 'Si la cuenta es elegible, recibirás instrucciones de recuperación'); });
form('reset', async data => { await api.request('/auth/reset-password', { ...data, token: resetToken }); resetToken = null; api.clear(); navigate('login', true); notify('Contraseña actualizada. Inicia sesión de nuevo.'); });
form('change', async data => { await api.request('/auth/change-password', data); api.clear(); notify('Contraseña actualizada. Todas tus sesiones se cerraron.'); });
async function loadUsers(page = 0) {
  const button = $('#load-users'); button.disabled = true; $('#users').setAttribute('aria-busy', 'true');
  try {
    const users = await api.users(page); currentPage = page; $('#users').replaceChildren();
    for (const value of users) {
      const row = document.createElement('tr');
      const name = document.createElement('td'); name.textContent = value.displayName; const email = document.createElement('small'); email.textContent = value.email; name.append(email);
      const state = document.createElement('td'); state.textContent = value.status;
      const roles = document.createElement('td'); roles.textContent = value.roles.join(', ');
      const id = document.createElement('td'); const code = document.createElement('code'); code.textContent = value.id; id.append(code);
      if (can(user, 'users:write')) { const select = document.createElement('button'); select.textContent = 'Seleccionar'; select.className = 'secondary'; select.addEventListener('click', () => { $('#roles input[name=id]').value = value.id; $('#roles input[name=roles]').value = value.roles.join(','); $('#disable input[name=id]').value = value.id; }); id.append(select); }
      row.append(name, state, roles, id); $('#users').append(row);
    }
    if (!users.length) { const row = document.createElement('tr'); const cell = document.createElement('td'); cell.colSpan = 4; cell.textContent = 'No hay usuarios en esta página.'; row.append(cell); $('#users').append(row); }
    $('#page-number').textContent = `Página ${page + 1}`; $('#previous-page').disabled = page === 0; $('#next-page').disabled = users.length < 10;
  } finally { button.disabled = false; $('#users').removeAttribute('aria-busy'); }
}
form('roles', async data => { await api.request(`/admin/users/${encodeURIComponent(data.id)}/roles`, { roles: data.roles.split(',').map(v => v.trim()) }, 'PUT'); if (data.id === user?.id) api.clear(); else await loadUsers(currentPage); notify('Permisos actualizados. El usuario debe iniciar sesión de nuevo.'); });
form('disable', async data => { if (!confirm('¿Desactivar esta cuenta y revocar sus sesiones?')) return; await api.request(`/admin/users/${encodeURIComponent(data.id)}`, undefined, 'DELETE'); await loadUsers(currentPage); notify('Cuenta desactivada.'); });
$('#logout').addEventListener('click', async event => { event.currentTarget.disabled = true; try { await api.request('/auth/logout', {}); api.clear(); notify('Sesiones cerradas.'); } catch (error) { notify(error.message, true); } finally { $('#logout').disabled = false; } });
$('#load-users').addEventListener('click', () => loadUsers(0).catch(error => notify(error.message, true)));
$('#previous-page').addEventListener('click', () => loadUsers(Math.max(0, currentPage - 1)).catch(error => notify(error.message, true)));
$('#next-page').addEventListener('click', () => loadUsers(currentPage + 1).catch(error => notify(error.message, true)));
nexo = mountNexo({ api, getUser: () => user, notify });
window.addEventListener('hashchange', () => navigate(location.hash.slice(1), true));
window.addEventListener('popstate', () => navigate(location.hash.slice(1), true));
(async () => {
  applyPermissions(document, null);
  if (resetToken) { history.replaceState(null, '', location.pathname); navigate('reset', true); }
  else { try { await api.refresh(); await loadProfile(); } catch { clearSession(); } }
  restoring = false;
  navigate(resetToken ? 'reset' : (location.hash.slice(1) || (user ? 'account' : 'login')), true);
  try { const health = await api.request('/health', undefined, 'GET'); $('#health').textContent = health.status === 'UP' ? 'Servicio disponible' : 'Servicio no disponible'; }
  catch { $('#health').textContent = 'Servicio no disponible'; }
})();
