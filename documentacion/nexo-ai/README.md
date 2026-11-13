# NEXO-Δ AI — Asistente Inteligente de ExamenErik

Implementación local integrada, 8 de noviembre de 2026 (los registros UTC posteriores a las 18:00 aparecen como 9 de noviembre). El proyecto es **ExamenErik**. No se ha publicado una aplicación ni contratado un servicio durante esta evolución.

NEXO utiliza **inferencia real de Qwen2.5-Coder 7B mediante Ollama**, con historial privado en MySQL, contexto autorizado, herramientas acotadas y simulación determinista. La identidad, el orquestador, la interfaz y el laboratorio son personalizados. El modelo fundacional es preentrenado de terceros; no se afirma haberlo entrenado ni garantizar originalidad mundial.

## Documentos de entrega

- [Auditoría y arquitectura](ARQUITECTURA.md)
- [Motor de IA, privacidad y alternativas](MOTOR_IA.md)
- [Manual de operación y despliegue](OPERACION.md)
- [Pruebas y evidencia](PRUEBAS.md)
- [Guion de exposición](GUION_EXPOSICION.md)
- [Evidencias de esta evolución](evidencia/)

## Inicio rápido

Dentro del proyecto completo abre `agente-nexo/Abrir NEXO.command`. Inicia sesión en `http://localhost:8090` y pulsa **NEXO-Δ AI** en la navegación. El lanzador prepara un entorno de desarrollo separado, con datos sintéticos y MySQL persistente. No se conecta a la base de otras aplicaciones.

La cuenta inicial se guarda **solo en** `agente-nexo/.local/Acceso local.txt`, con permisos privados. No se incluye en la entrega, no se imprime en evidencias y no debe compartirse. También puedes registrar una cuenta de prueba propia. La cuenta USER puede conversar y simular su propio acceso; un administrador autorizado puede leer escenarios de otras cuentas.

El lanzador integrado reemplaza la apertura predeterminada del antiguo panel aislado. La demo sintética Python se conserva y sus pruebas siguen disponibles. El ZIP antiguo `NEXO-Delta-ExamenErik.zip` no contiene esta nueva integración: utiliza el proyecto completo o la nueva entrega.

## Alcance operativo

Conversaciones de varios turnos, generación progresiva, referencias consultables, siete herramientas/modos de consulta, carga acotada de archivos de texto, exportación JSON, historial privado, borrado, retención, límites de uso, auditoría sin mensajes y Predictive Access Lab. No hay herramientas de escritura IAM, ejecución de comandos, SQL arbitrario, navegación web ni lectura libre del disco.

La publicación gratuita en tres proveedores sigue preparada para la aplicación IAM. **La inferencia pública independiente de esta Mac requiere infraestructura adicional y validación de capacidad**. No se presenta la ejecución local como despliegue público.
