export function can(user, permission) { return Boolean(user?.permissions?.includes(permission)); }
export function applyPermissions(document, user) {
  for (const element of document.querySelectorAll('[data-permission]')) element.hidden = !can(user, element.dataset.permission);
  for (const element of document.querySelectorAll('[data-member]')) element.hidden = !user;
  for (const element of document.querySelectorAll('[data-guest]')) element.hidden = Boolean(user);
}
export function routeAllowed(route, user, resetToken) {
  if (route === 'nexo' && !can(user, 'profile:read')) return user ? 'account' : 'login';
  if (route === 'account' && !user) return 'login';
  if (route === 'admin' && !can(user, 'users:read')) return user ? 'account' : 'login';
  if (route === 'reset' && !resetToken) return 'recovery';
  return ['login', 'register', 'recovery', 'reset', 'account', 'admin', 'nexo'].includes(route) ? route : 'login';
}
export function renderProfile(document, user) {
  const root = document.querySelector('#profile'); root.replaceChildren();
  for (const [name, value] of [['Nombre', user.displayName], ['Correo', user.email], ['Estado', user.status], ['Roles', user.roles.join(', ')], ['ID', user.id]]) {
    const term = document.createElement('dt'); term.textContent = name;
    const description = document.createElement('dd'); description.textContent = value;
    root.append(term, description);
  }
}
export function matchingPasswords(form) {
  const password = form.elements.namedItem('password');
  const confirmation = form.elements.namedItem('confirmPassword');
  if (!confirmation) return true;
  confirmation.setCustomValidity(password.value === confirmation.value ? '' : 'Las contraseñas no coinciden');
  return confirmation.checkValidity();
}
