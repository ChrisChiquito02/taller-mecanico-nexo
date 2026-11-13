import { build } from 'esbuild';
import { mkdir, readFile, writeFile, copyFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { validateApiBase } from '../src/api-base.js';
const root = fileURLToPath(new URL('../', import.meta.url));
const integrated = process.argv.includes('--integrated');
const output = path.resolve(root, integrated ? '../sistema-m01-m02/src/main/resources/static' : 'dist');
const apiBase = validateApiBase(process.env.APP_API_BASE_PATH || '/api');
await mkdir(output, { recursive: true });
await build({ entryPoints: [path.join(root, 'src/app.js')], outfile: path.join(output, 'app.js'),
  bundle: true, format: 'iife', target: ['es2022'], minify: true, sourcemap: false,
  define: { __API_BASE__: JSON.stringify(apiBase) }, legalComments: 'none' });
const html = await readFile(path.join(root, 'public/index.html'), 'utf8');
await writeFile(path.join(output, 'index.html'), html);
await writeFile(path.join(output, 'reset-password.html'), html);
await copyFile(path.join(root, 'src/styles.css'), path.join(output, 'styles.css'));
if (!integrated) {
  const connect = apiBase.startsWith('https:') ? ` 'self' ${new URL(apiBase).origin}` : " 'self'";
  await writeFile(path.join(output, '_headers'), `/*
  Content-Security-Policy: default-src 'none'; script-src 'self'; style-src 'self'; connect-src${connect}; form-action 'self'; base-uri 'none'; frame-ancestors 'none'
  X-Content-Type-Options: nosniff
  Referrer-Policy: no-referrer
  Permissions-Policy: camera=(), microphone=(), geolocation=()
  Cache-Control: no-store
`);
}
console.log(`Frontend compilado: ${integrated ? 'integrado (desarrollo)' : 'dist (publicación independiente)'}`);
