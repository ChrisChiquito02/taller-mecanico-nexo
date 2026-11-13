import test from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { renderAnswer } from '../../src/nexo.js';
import { createApiClient } from '../../src/api-client.js';
import { routeAllowed } from '../../src/ui.js';
test('model HTML and code are inert text; unauthenticated route is denied', () => {
  const dom = new JSDOM('<div id="answer"></div>'); globalThis.document = dom.window.document;
  const root = document.querySelector('#answer');
  renderAnswer(root, '<img src=x onerror=alert(1)>\n```js\n<script>alert(1)</script>```');
  assert.equal(root.querySelector('img'), null); assert.equal(root.querySelector('script'), null);
  assert.ok(root.querySelector('code').textContent.includes('<script>'));
  assert.equal(routeAllowed('nexo', null), 'login');
  assert.equal(routeAllowed('nexo', { permissions: ['profile:read'] }), 'nexo');
  delete globalThis.document;
});
test('stream requires authenticated headers and a completion event, never retries partial output', async () => {
  let calls = 0; const events = [];
  const api = createApiClient({ fetchImpl: async (url, options) => {
    calls++; assert.equal(options.headers.Authorization, 'Bearer test-access');
    return new Response('{"type":"delta","text":"hola"}\n', { status: 200 });
  }});
  api.setSession({ accessToken: 'test-access', tokenType: 'Bearer', expiresIn: 900 });
  await assert.rejects(api.stream('/nexo/conversations/00000000-0000-0000-0000-000000000000/messages', { text: 'Hola' }, e => events.push(e)), /interrumpida/);
  assert.equal(calls, 1); assert.equal(events[0].text, 'hola');
});
test('stream accepts truncation as its own terminal event', async () => {
  const events = [];
  const api = createApiClient({ fetchImpl: async () =>
    new Response('{"type":"delta","text":"parcial"}\n{"type":"truncated"}\n') });
  await api.stream('/nexo/conversations/00000000-0000-0000-0000-000000000000/messages',
    { text: 'Lista larga' }, event => events.push(event.type));
  assert.deepEqual(events, ['delta', 'truncated']);
});
