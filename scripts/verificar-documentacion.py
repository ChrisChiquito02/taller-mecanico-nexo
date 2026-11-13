#!/usr/bin/env python3
"""Valida referencias y contratos locales sin leer archivos de secretos."""
from pathlib import Path
import json
import re
from urllib.parse import unquote
ROOT = Path(__file__).resolve().parents[1]
FILES = [ROOT / 'orquestador.md', ROOT / 'README.md', ROOT / 'frontend/README.md', ROOT / 'sistema-m01-m02/README.md', ROOT / 'sistema-m01-m02/SECURITY.md', *sorted((ROOT / 'documentacion').rglob('*.md'))]
required = ['orquestador.md', 'documentacion/frontend/documentadorFrontend.md', 'documentacion/backend/documentadorBackend.md', 'documentacion/auditoria/INFORME_FINAL.md', 'documentacion/auditoria/MATRIZ_INTEGRACION.md']
for name in required:
    assert (ROOT / name).is_file(), f'Falta {name}'
checked = 0
for file in FILES:
    for link in re.findall(r'\]\(([^)]+)\)', file.read_text()):
        if '://' in link or link.startswith('#'):
            continue
        target = unquote(link.split('#')[0]).strip('<>')
        assert (file.parent / target).exists(), f'Referencia inexistente en {file.relative_to(ROOT)}: {target}'
        checked += 1
spec = json.loads((ROOT / 'docs/integracion/openapi.json').read_text())
def refs(value):
    if isinstance(value, dict):
        if '$ref' in value:
            assert value['$ref'].startswith('#/'), 'No se admiten referencias OpenAPI externas sin revisión'
            target = spec
            for part in value['$ref'][2:].split('/'):
                target = target[part]
        for item in value.values(): refs(item)
    elif isinstance(value, list):
        for item in value: refs(item)
refs(spec)
register = spec['paths']['/api/auth/register']['post']['responses']
assert '202' in register and '201' not in register
assert spec['openapi'] == '3.1.0'
assert len(spec['paths']) == 19
assert '/api/nexo/conversations/{conversationId}/messages' in spec['paths']
print(f'Documentación: {len(FILES)} archivos, {checked} referencias locales y 19 rutas HTTP comprobados. No es un validador completo OpenAPI.')
