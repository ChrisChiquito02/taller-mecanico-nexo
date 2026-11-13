import csv,json
from pathlib import Path
from collections import Counter
p=Path(__file__).parent
with (p.parent/'verificacion-nexo/matriz-antes-despues.csv').open(encoding='utf-8-sig') as f:
    rows=list(csv.DictReader(f))
weights={'LLM':20,'Coherencia':20,'Memoria':15,'Agente':15,'Frontend/backend':10,'Persistencia/seguridad':10,'Estabilidad':10}
updates={
 'Detección de premisas falsas e incompatibilidades':('FUNCIONAL',1,'bateria.jsonl casos 28–35; casos.json; contradiccion-abierta.json'),
 'Explicación IA de simulación':('FUNCIONAL',1,'casos.json sim; navegador.json UI12; métricas renderizadas desde el laboratorio'),
 'Redactar informe fiel a datos':('FUNCIONAL',1,'casos.json report y report-mixed; informe extractivo con priorización y datos íntegros'),
 'SMTP y recuperación':('PARCIAL',1,'correo.json verifica recuperación completa en Mailpit; SMTP externo pendiente'),
 'Despliegue TLS/secretos/cookies entre hosts':('PARCIAL',0.5,'tls/chrome.json y health.json; un host local y certificado autofirmado'),
 'Concurrencia de varias réplicas y carga sostenida':('FUNCIONAL',1,'carga.json; seis cuentas y dos réplicas, cuota compartida, 60,73 s'),
 'Recuperación desde backup de producción':('PARCIAL',0.5,'mysql-restauracion.log; esquema sintético, sin infraestructura de producción')
}
for r in rows:
    s,i,e=updates.get(r['feature'],(r['state'],float(r['i1']),r['evidence']))
    r.update(final=s,i2=i,evidencia_final=e)
assert len(rows)==41
def verify(x):return int(x in {'FUNCIONAL','SIMULADA'})
metrics={}
for category in weights:
    members=[r for r in rows if r['category']==category]
    vals=[sum(float(r[col]) for r in members) for col in ('i0','i1','i2')]+[
        sum(verify(r[col]) for r in members) for col in ('baseline','state','final')]
    metrics[category]=[100*v/len(members) for v in vals]
totals=[round(sum(weights[g]*m[i]/100 for g,m in metrics.items()),2) for i in range(6)]
with (p/'matriz-final.csv').open('w',newline='',encoding='utf-8-sig') as f:
    w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
print(json.dumps(dict(implementation=totals[:3],verified=totals[3:],
    states=Counter(r['final'] for r in rows),category=metrics),ensure_ascii=False,indent=2))
