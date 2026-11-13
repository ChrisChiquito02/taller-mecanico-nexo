// All model and document text is rendered as text nodes, never trusted HTML.
export function renderAnswer(root, text) {
  root.replaceChildren();
  const parts = text.split('```');
  for (let i = 0; i < parts.length; i++) {
    const element = document.createElement(i % 2 ? 'pre' : 'div');
    if (i % 2) { const code = document.createElement('code'); code.textContent = parts[i].replace(/^[\w+-]*\n/, ''); element.append(code); }
    else element.textContent = parts[i];
    root.append(element);
  }
}
export function mountNexo({ api, getUser, notify }) {
  const $ = id => document.getElementById(id);
  let chat = null, messages = [], active = null, generation = 0, lastSimulation = null;
  function reset() {
    generation++;
    if (active?.accepted) cancel(active);
    active?.controller.abort(); active = null; chat = null; messages = []; lastSimulation = null;
    $('nexo-messages').replaceChildren(); $('nexo-chats').replaceChildren(); $('nexo-report').replaceChildren();
    $('nexo-send').disabled = false; $('nexo-stop').hidden = true; $('nexo-text').value = ''; $('nexo-file').value = '';
    $('nexo-explain').hidden = true; $('nexo-document').close(); $('nexo-document-text').textContent = ''; $('nexo-document-title').textContent = '';
    $('nexo-model').textContent = 'Comprobación pendiente';
  }
  function line(role, text) {
    const box = document.createElement('article'); box.className = `nexo-bubble ${role}`;
    const name = document.createElement('strong'); name.textContent = role === 'user' ? 'Tú' : 'NEXO-Δ AI';
    const body = document.createElement('div'); body.className = 'nexo-body'; renderAnswer(body, text); box.append(name, body); $('nexo-messages').append(box);
    return { box, body };
  }
  function referenceLinks(box, metadata) {
    if (metadata?.origin) {
      const source = document.createElement('p'); source.className = 'nexo-notice';
      source.textContent = {
        attachment: 'Extracción local del archivo',
        documentation: 'Extracto de documentación del proyecto',
        report: 'Selección de datos asistida por Ollama',
        simulation: 'Explicación asistida por Ollama con cifras del laboratorio',
        clarification: 'Validación de restricciones'
      }[metadata.origin] || 'Fuente del resultado';
      box.append(source);
    }
    if (metadata?.review) { const warning = document.createElement('p'); warning.className = 'nexo-notice'; warning.textContent = metadata.review; box.append(warning); }
    const refs = document.createElement('div'); refs.className = 'nexo-references'; refs.setAttribute('aria-label', 'Fuentes recuperadas para comprobar la respuesta');
    for (const ref of metadata?.references || []) {
      const button = document.createElement('button'); button.type = 'button'; button.textContent = `[${ref.id}] ${ref.title}`;
      button.addEventListener('click', async () => {
        const current = generation;
        try { const doc = await api.request(`/nexo/documents/${encodeURIComponent(ref.id)}`, undefined, 'GET'); if (current !== generation) return;
          $('nexo-document-title').textContent = doc.title; $('nexo-document-text').textContent = `${doc.source}\n\n${doc.text}`; $('nexo-document').showModal();
        } catch (e) { notify(e.message, true); }
      }); refs.append(button);
    }
    box.append(refs);
  }
  async function history() {
    const current = generation;
    const chats = await api.request('/nexo/conversations', undefined, 'GET'); if (current !== generation) return;
    $('nexo-chats').replaceChildren();
    for (const item of chats) {
      const button = document.createElement('button'); button.type = 'button'; button.textContent = `${item.title} · ${String(item.created_at).slice(5, 19).replace('T', ' ')} · ${item.id.slice(0, 8)}`;
      button.setAttribute('aria-pressed', String(chat === item.id));
      button.addEventListener('click', async () => {
        if (active) return;
        const epoch = generation;
        try { const loaded = await api.request(`/nexo/conversations/${item.id}`, undefined, 'GET'); if (epoch !== generation) return;
          chat = item.id; messages = loaded; $('nexo-messages').replaceChildren();
          for (const m of messages) { const { box } = line(m.speaker, m.content); if (m.speaker === 'assistant') { try { referenceLinks(box, JSON.parse(m.metadata)); } catch {} } }
          await history();
        } catch (e) { notify(e.message, true); }
      }); $('nexo-chats').append(button);
    }
  }
  async function open() {
    const current = generation;
    try {
      const status = await api.request('/nexo/status', undefined, 'GET'); if (current !== generation) return;
      $('nexo-model').textContent = `${status.connected ? '● Conectado' : '○ No disponible'} · ${status.model} · local`;
      $('nexo-model').classList.toggle('offline', !status.connected);
      $('nexo-model-guide').hidden = status.connected;
      $('nexo-model-guide').textContent = status.connected ? '' : status.guide;
      $('nexo-retention').textContent = `Historial privado · caduca en ${status.retentionDays} días`;
      const tools = await api.request('/nexo/tools', undefined, 'GET');
      if (current !== generation) return;
      const picker = $('nexo-tool');
      const selected = picker.value;
      for (const option of [...picker.options].slice(2)) option.remove();
      for (const tool of tools) picker.add(new Option(tool.name, tool.id));
      if ([...picker.options].some(option => option.value === selected)) picker.value = selected;
      await history();
    } catch (e) { if (current === generation) notify(e.message, true); }
  }
  async function submit(text, tool, simulation = null) {
    if (active) return;
    const current = generation;
    const controller = new AbortController();
    const job = { controller, turnId: crypto.randomUUID(), accepted: false, stopping: false, truncated: false };
    active = job;
    $('nexo-send').disabled = true; $('nexo-stop').hidden = false; $('nexo-stop').disabled = false;
    let reply, answer = '', metadata = {};
    try {
      let attachment = null; const file = $('nexo-file').files[0];
      if (file) { if (file.size > 12000) throw new Error('El archivo supera 12 KB. Usa un fragmento sin secretos.');
        const content = new TextDecoder('utf-8', { fatal: true }).decode(await file.arrayBuffer());
        attachment = { name: file.name, text: content }; if (attachment.text.length > 6000) throw new Error('Máximo 6000 caracteres por archivo.'); }
      if (current !== generation) return;
      if (!chat) { const created = await api.request('/nexo/conversations', {}); if (current !== generation) return; chat = created.id; }
      job.chat = chat;
      line('user', text); reply = line('assistant', 'Preparando respuesta…');
      await api.stream(`/nexo/conversations/${chat}/messages`, { text, tool, simulation, attachment, turnId: job.turnId }, event => {
        if (current !== generation) return;
        if (event.type === 'status') {
          job.accepted = true;
          if (job.stopping) cancel(job);
          else $('nexo-progress').textContent = event.text;
        }
        if (event.type === 'context') { metadata = event.metadata; $('nexo-progress').textContent = metadata.generated ? `Herramienta: ${metadata.tool} · generando respuesta` : 'Procesando fuentes verificables…'; if (metadata.simulation) report(metadata.simulation); }
        if (event.type === 'delta') { answer += event.text; renderAnswer(reply.body, answer); }
        if (event.type === 'truncated') {
          job.truncated = true;
          renderAnswer(reply.body, answer + '\n\n[Respuesta truncada: no se guardó este turno]');
          $('nexo-progress').textContent = 'Se alcanzó el límite de generación. Acota la pregunta para volver a intentarlo.';
        }
        if (event.type === 'done') { metadata = event.metadata; $('nexo-progress').textContent = 'Respuesta completa · revisa las recomendaciones antes de usarlas'; }
      }, controller.signal);
      if (current !== generation) return;
      if (job.truncated) return;
      referenceLinks(reply.box, metadata);
      messages.push({ speaker: 'user', content: text }, { speaker: 'assistant', content: answer, metadata: JSON.stringify(metadata) });
      $('nexo-text').value = ''; $('nexo-file').value = ''; await history();
    } catch (e) {
      if (current !== generation) return;
      if (reply) renderAnswer(reply.body, answer ? `${answer}\n\n[Respuesta incompleta: no se guardó este turno]` : e.message);
      $('nexo-progress').textContent = e.code === 'TURN_CANCELLED' ? 'Generación detenida. El turno incompleto no se guarda.' : e.message;
    } finally { if (current === generation) { active = null; $('nexo-send').disabled = false; $('nexo-stop').hidden = true; } }
  }
  function report(data) {
    const root = $('nexo-report'); root.replaceChildren();
    const risk = data.requested.risk;
    const score = document.createElement('p'); score.className = `nexo-score ${risk.label}`; score.textContent = `${risk.score}/100 · Riesgo ${risk.label}`; root.append(score);
    const scope = document.createElement('p'); scope.textContent = data.source === 'synthetic' ? 'Escenario sintético. Cálculos verificables; ningún cambio aplicado.' : 'Lectura autorizada. Se cuentan refresh vigentes conocidos. El total de JWT de acceso y de sesiones es desconocido.'; root.append(scope);
    const wrap = document.createElement('div'); wrap.className = 'table-wrap'; const table = document.createElement('table');
    const caption = document.createElement('caption'); caption.textContent = 'Estado actual frente a cambio hipotético'; table.append(caption);
    const head = document.createElement('tr'); for (const title of ['Cuenta', 'Antes', 'Hipótesis', data.source === 'synthetic' ? 'Tokens sintéticos afectados' : 'Refresh conocidos afectados']) { const th = document.createElement('th'); th.scope = 'col'; th.textContent = title; head.append(th); } table.append(head);
    for (const row of data.requested.affected) { const tr = document.createElement('tr'); for (const value of [row.user, row.before_permissions.join(', '), row.proposed_permissions.join(', '), String(row.sessions_invalidated_by_change)]) { const td = document.createElement('td'); td.textContent = value; tr.append(td); } table.append(tr); }
    wrap.append(table); root.append(wrap);
    const alt = document.createElement('p'), candidate = data.least_privilege_candidates?.[0], alternative = data.alternative_evaluated;
    alt.textContent = alternative?.simulation ? `Alternativa: ${alternative.no_change_needed ? 'conservar el estado actual' : candidate?.roles.join(', ')}. Puntaje ${alternative.simulation.risk.score}/100. ${candidate?.extra_permissions?.length ? `Permisos extra todavía necesarios por el catálogo: ${candidate.extra_permissions.join(', ')}.` : ''}` : 'El catálogo actual no satisface la tarea indicada.'; root.append(alt);
    const details = document.createElement('details'), summary = document.createElement('summary'), pre = document.createElement('pre'); summary.textContent = 'Método, recursos, reversión y límites'; pre.textContent = JSON.stringify(data, null, 2); details.append(summary, pre); root.append(details);
  }
  function simulationRequest() {
    const source = $('nexo-source').value;
    return { source, scenario: $('nexo-scenario').value, minute: Number($('nexo-minute').value), target: $('nexo-target').value || null,
      roles: [$('nexo-role').value], required: [$('nexo-required').value], patchRole: $('nexo-patch').checked ? 'USER' : null,
      patchPermissions: $('nexo-patch').checked ? ['profile:read'] : null };
  }
  $('nexo-form').addEventListener('submit', e => { e.preventDefault(); if (e.currentTarget.reportValidity()) submit($('nexo-text').value, $('nexo-tool').value); });
  async function cancel(job) {
    if (!job.accepted) return;
    try {
      await api.request(`/nexo/conversations/${job.chat}/turns/${job.turnId}`, undefined, 'DELETE');
    } catch (e) {
      if (active === job && e.code !== 'TURN_NOT_ACTIVE') {
        $('nexo-progress').textContent = 'No se pudo confirmar la cancelación. Puedes intentarlo de nuevo.';
        $('nexo-stop').disabled = false;
      }
    }
  }
  $('nexo-stop').addEventListener('click', () => {
    if (!active) return;
    active.stopping = true;
    $('nexo-stop').disabled = true;
    $('nexo-progress').textContent = 'Solicitando cancelación…';
    cancel(active);
  });
  $('nexo-new').addEventListener('click', () => { if (active) return; chat = null; messages = []; $('nexo-messages').replaceChildren(); history().catch(e => notify(e.message, true)); });
  $('nexo-delete').addEventListener('click', async () => { if (!chat || active || !confirm('¿Eliminar esta conversación de tu historial?')) return;
    try { await api.request(`/nexo/conversations/${chat}`, undefined, 'DELETE'); chat = null; messages = []; $('nexo-messages').replaceChildren(); await history(); } catch (e) { notify(e.message, true); }
  });
  $('nexo-lab-form').addEventListener('submit', async e => { e.preventDefault(); const button = $('nexo-simulate'); if (button.disabled) return; button.disabled = true; const current = generation;
    try { const request = simulationRequest(); const result = await api.request('/nexo/simulate', request); if (current !== generation) return; lastSimulation = { request, result }; report(result); $('nexo-explain').hidden = false; } catch (e) { if (current === generation) notify(e.message, true); } finally { button.disabled = false; }
  });
  $('nexo-explain').addEventListener('click', () => { if (lastSimulation) submit('Explica este cambio hipotético, sus riesgos, la alternativa y el plan de reversión. Distingue hechos e incertidumbres.', 'simulation', lastSimulation.request); });
  $('nexo-export').addEventListener('click', () => {
    const data = JSON.stringify({ project: 'ExamenErik', agent: 'NEXO-Δ AI', exported_at: new Date().toISOString(), messages, simulation: lastSimulation?.result || null }, null, 2);
    const url = URL.createObjectURL(new Blob([data], { type: 'application/json' })); const a = document.createElement('a'); a.href = url; a.download = 'nexo-informe.json'; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  });
  $('nexo-check').addEventListener('click', open);
  $('nexo-document-close').addEventListener('click', () => $('nexo-document').close());
  for (const button of document.querySelectorAll('[data-nexo-prompt]')) button.addEventListener('click', () => { $('nexo-text').value = button.dataset.nexoPrompt; $('nexo-text').focus(); });
  $('nexo-source').addEventListener('change', () => { $('nexo-live-options').hidden = $('nexo-source').value !== 'live'; $('nexo-synthetic-options').hidden = $('nexo-source').value !== 'synthetic'; });
  return { open, reset };
}
