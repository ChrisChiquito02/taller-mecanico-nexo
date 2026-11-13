const $ = selector => document.querySelector(selector);
let lastReport = null;
const text = (selector, value) => { $(selector).textContent = String(value); };
const formatRoles = roles => roles.length ? roles.join(' + ') : 'Sin roles';
function element(tag, value, className) {
  const node = document.createElement(tag); node.textContent = value;
  if (className) node.className = className;
  return node;
}
function render(report) {
  lastReport = report;
  const result = report.requested;
  text('#risk-score', `${result.risk.score}/100`);
  text('#risk-label', `Riesgo ${result.risk.label} · minuto ${result.at_minute}`);
  text('#affected-count', result.affected.length);
  text('#session-count', result.affected.reduce((sum, user) => sum + user.sessions_invalidated_by_change, 0));
  $('#affected').replaceChildren();
  for (const user of result.affected) {
    const row = element('article', '', 'person');
    row.append(element('strong', user.user), element('p', `${formatRoles(user.before_roles)} → ${formatRoles(user.proposed_roles)}`));
    if (user.gained.length) row.append(element('p', `Permisos nuevos: ${user.gained.join(', ')}`));
    if (user.lost.length) row.append(element('p', `Permisos retirados: ${user.lost.join(', ')}`));
    row.append(element('small', `${user.sessions_still_valid} tokens siguen válidos; ${user.sessions_invalidated_by_change} quedarían invalidados por el cambio.`));
    $('#affected').append(row);
  }
  if (!result.affected.length) $('#affected').append(element('p', 'Este escenario no altera permisos ni revoca sesiones al momento elegido.'));
  const alternative = report.alternative_evaluated;
  if (alternative) {
    text('#alternative-title', alternative.no_change_needed ? 'Conservar el estado actual' : 'Revisar otro conjunto de roles');
    const roles = Object.entries(alternative.change.assignments).map(([id, roles]) => `${id}: ${formatRoles(roles)}`).join('; ');
    const message = alternative.no_change_needed ? 'No llamar a la operación de cambio de roles. La necesidad ya está cubierta.' : roles;
    text('#alternative-body', `${message} Riesgo calculado: ${alternative.simulation.risk.score}/100.`);
    $('#catalog-gap').hidden = !report.least_privilege_candidates[0]?.extra_permissions.includes('users:write');
  } else {
    text('#alternative-title', 'No hay alternativa evaluada');
    text('#alternative-body', 'Revisa las necesidades y los roles disponibles antes de decidir.');
    $('#catalog-gap').hidden = true;
  }
  $('#rollback').replaceChildren();
  const rollback = result.rollback_proposal_only;
  for (const [id, roles] of Object.entries(rollback.assignments)) $('#rollback').append(element('p', `${id}: volver al conjunto ${formatRoles(roles)}.`));
  for (const [role, permissions] of Object.entries(rollback.permission_patch)) $('#rollback').append(element('p', `Catálogo ${role}: volver a ${permissions.join(', ') || 'ningún permiso'}.`));
  if (!Object.keys(rollback.assignments).length && !Object.keys(rollback.permission_patch).length) $('#rollback').append(element('p', 'No hay modificaciones de roles o catálogo que revertir en este escenario.'));
  if (report.ai.status === 'executed') {
    text('#ai-status', 'Inferencia realizada');
    text('#explanation', report.ai.advisory_untrusted.explanation);
    text('#review', report.ai.advisory_untrusted.human_review);
    text('#ai-meta', `${report.ai.model} · ${new Date(report.ai.created_at).toLocaleString('es-MX')} · Recomendación para revisión humana`);
  } else {
    text('#ai-status', 'Motor de simulación');
    text('#explanation', 'Se calcularon los efectos con las reglas del proyecto. No se solicitó una inferencia de IA.');
    text('#review', 'Puedes activar la explicación con IA local y volver a analizar el escenario.');
    text('#ai-meta', 'Sin inferencia generativa en este informe.');
  }
  $('#empty').hidden = true; $('#results').hidden = false;
}
$('#simulation').addEventListener('submit', async event => {
  event.preventDefault();
  if (!event.currentTarget.reportValidity()) return;
  const button = $('#analyze'); button.disabled = true;
  $('#results').hidden = true; $('#empty').hidden = true; $('.output').setAttribute('aria-busy', 'true');
  const useAI = $('#use-ai').checked;
  text('#message', useAI ? 'Calculando escenarios y consultando la IA local… Puede tardar unos minutos.' : 'Calculando el impacto del escenario…');
  try {
    const response = await fetch('/api/simulate', {method: 'POST', headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({scenario: $('input[name=scenario]:checked').value, at_minute: Number($('#minute').value), use_ai: useAI})});
    const report = await response.json();
    if (!response.ok) throw new Error(report.error || 'No se pudo completar el análisis.');
    render(report); text('#message', 'Análisis completado. No se aplicaron cambios reales.');
  } catch (error) {
    lastReport = null; $('#results').hidden = true; $('#empty').hidden = false;
    text('#message', error.message || 'No se pudo conectar con el panel local.');
  } finally {
    button.disabled = false; $('.output').setAttribute('aria-busy', 'false');
  }
});
$('#download').addEventListener('click', () => {
  if (!lastReport) return;
  const blob = new Blob([JSON.stringify(lastReport, null, 2)], {type: 'application/json'});
  const url = URL.createObjectURL(blob); const link = document.createElement('a');
  link.href = url; link.download = 'nexo-delta-informe.json'; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
});
