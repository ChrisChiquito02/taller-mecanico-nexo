# Abrir NEXO-Δ AI integrado

Abre **Abrir NEXO.command** desde esta carpeta dentro del proyecto completo ExamenErik. Se prepara un entorno Docker propio y se abre http://localhost:8090/#nexo. Inicia sesión y selecciona **NEXO-Δ AI**.

La cuenta sintética inicial está en `.local/Acceso local.txt`, con permisos privados. No compartas ese archivo. Necesitas Docker Desktop iniciado, Node 24+, Python 3 y Ollama con `qwen2.5-coder:7b`.

Puedes cerrar Terminal: Docker mantiene el sitio. **Detener NEXO.command** detiene solamente este entorno y conserva la base.

[Manual completo](../documentacion/nexo-ai/OPERACION.md) · [Arquitectura y límites](../documentacion/nexo-ai/ARQUITECTURA.md).

La demo antigua se conserva: `python3 servidor.py --open`, solo escenarios sintéticos. No tiene autenticación IAM ni historial y no es la interfaz principal nueva.
