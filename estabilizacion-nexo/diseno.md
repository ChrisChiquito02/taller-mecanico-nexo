# Diseño previo a los cambios

- El chat general seguirá usando Ollama. Un validador previo resolverá únicamente restricciones de cardinalidad explícitas cuya intersección pueda calcularse. Ante conflicto pedirá aclaración. El modelo recibirá instrucciones para priorizar el turno actual y reconocer otros conflictos o ambigüedades.
- En informes, Ollama seleccionará y ordenará fragmentos identificados de los datos aportados. Java comprobará identificadores, orden, estructura y unicidad, y renderizará la evidencia literal. El modo será extractivo: acota las alucinaciones y no da por verificados los datos del usuario.
- La explicación de métricas del laboratorio combinará una interpretación asistida por Ollama con cifras, etiquetas de tokens y límites escritos por el motor. Los datos y roles no se editan.
- V5 añade una tabla de cuota por cuenta compartida entre réplicas. La reserva existente se renueva durante inferencia con expiración corta, para que una caída del proceso no bloquee la cuenta durante varios minutos. El límite de hilos de inferencia sigue siendo por proceso. V1–V4 conservan sus sumas de comprobación.
