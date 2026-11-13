import csv
import json
from collections import Counter
from pathlib import Path

here = Path(__file__).parent
baseline = here.parent/'auditoria-nexo'/'evidencias'/'matriz-base.tsv'
weights = {'LLM':20, 'Coherencia':20, 'Memoria':15, 'Agente':15, 'Frontend/backend':10,
           'Persistencia/seguridad':10, 'Estabilidad':10}
# Same criteria and weights as the audit. A partial outcome still contributes zero to verification.
changes = {
    'Inferencia y transmisión Ollama a navegador': ('FUNCIONAL', 1, 'UI18; huellas.txt'),
    'Planificación automática y ejecución permitida': ('FUNCIONAL', 1, 'AUTO; ollama.log'),
    'Fallo del proveedor sin reemplazo': ('FUNCIONAL', 1, 'MODEL-OFFLINE'),
    'Detección de premisas falsas e incompatibilidades': ('PARCIAL', 1, 'Caso 29 aprobado; caso 30 falló'),
    'Fidelidad y citas del proyecto': ('FUNCIONAL', 1, 'UI05, UI06 y DOC-falso'),
    'Historial después de recarga y reinicio': ('FUNCIONAL', 1, 'UI08 y DB-restart'),
    'Recuperación de contexto antiguo': ('FUNCIONAL', 1, 'MEM-restart: DIAMANTE-742 después de ocho turnos y reinicio'),
    'Expiración y limpieza programada': ('FUNCIONAL', 1, 'TTL-visibility y TTL-purge'),
    'Explicación IA de simulación': ('PARCIAL', 1, 'UI12 y simulate_self; prosa no siempre distingue refresh de todas las sesiones'),
    'Analizar contenido de un TXT': ('FUNCIONAL', 1, 'UI07, ATT-1 a ATT-4; extracción literal acotada'),
    'Redactar informe fiel a datos': ('PARCIAL', 0.5, 'informe-verificado.json; plantilla fiel, síntesis flexible pendiente'),
    'Resistencia de salida a instrucciones de adjuntos': ('FUNCIONAL', 1, 'Caso 40, ATT-0 a ATT-4 y ATT-context'),
    'Nuevo chat, restaurar y eliminar': ('FUNCIONAL', 1, 'UI08, UI09, UI16; DELETE 204'),
    'Detener turno y liberar backend': ('FUNCIONAL', 1, 'UI15, UI16 y REPLICAS; MySQL sin mensajes'),
    'Protección por cambio de autoridad durante stream': ('FUNCIONAL', 1, 'REVOKE; versión de sesión revocada durante inferencia'),
    'Finalización íntegra o señal de truncamiento': ('FUNCIONAL', 1, 'Ollama length 64; truncamiento.json; SQL 0 mensajes'),
    'Concurrencia de varias réplicas y carga sostenida': ('PARCIAL', 0.5, 'REPLICAS pasó; falta carga sostenida y cuota minuto sigue local'),
    'Recuperación desde backup de producción': ('PARCIAL', 0.5, 'restauracion.log: copia sintética comprobada; producción pendiente'),
}
rows=[]
for line in baseline.read_text().splitlines():
    category, feature, state, implementation, evidence, notes = line.split('|')
    new_state, new_impl, new_evidence = changes.get(feature, (state, float(implementation), 'Criterio conservado; '+evidence))
    rows.append(dict(category=category, feature=feature, baseline=state, state=new_state,
                     i0=float(implementation), i1=new_impl, evidence=new_evidence))
def verified(state):
    return int(state in {'FUNCIONAL','SIMULADA'})
def points(subset, key):
    return 100*sum(key(r) for r in subset)/len(subset)
by_group={}
for category, weight in weights.items():
    subset=[r for r in rows if r['category']==category]
    by_group[category]=[points(subset,k) for k in (lambda r:r['i0'], lambda r:r['i1'],
        lambda r:verified(r['baseline']), lambda r:verified(r['state']))]
totals=[round(sum(weights[g]*scores[n]/100 for g,scores in by_group.items()),2) for n in range(4)]
with (here/'matriz-antes-despues.csv').open('w',encoding='utf-8-sig',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
print(json.dumps(dict(totals=totals, categories=by_group, states=Counter(r['state'] for r in rows)),ensure_ascii=False,indent=2))
