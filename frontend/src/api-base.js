// La URL queda fijada durante el build; nunca se acepta desde formularios o querystrings.
export function validateApiBase(base) {
  if (typeof base !== 'string') throw new Error('Base de API inválida');
  if (/^\/(?!\/)[A-Za-z0-9/_-]+$/.test(base) && !base.endsWith('/')) return base;
  try {
    const u = new URL(base);
    if (u.protocol === 'https:' && !u.username && !u.password && !u.search && !u.hash
        && /^\/[A-Za-z0-9/_-]+$/.test(u.pathname) && !u.pathname.endsWith('/')
        && u.href === base) return base;
  } catch {}
  throw new Error('Base de API inválida: usa /api o una URL HTTPS sin credenciales, query ni barra final');
}
