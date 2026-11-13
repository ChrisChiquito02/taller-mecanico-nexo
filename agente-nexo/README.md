# NEXO-Δ AI — ExamenErik

El asistente principal está integrado en el frontend y en Spring Boot. Abre `Abrir NEXO.command` dentro del proyecto completo. [Guía](GUIA_DE_USO.md).

`nexo.py`, `servidor.py`, `panel/` y los fixtures conservan el laboratorio Python anterior para comparar resultados. El backend Java reutiliza su metodología y fixtures, con lecturas autorizadas y simulación sin cambios IAM.

Pruebas del motor anterior: `python3 -m unittest discover -s agente-nexo -v` desde la raíz.
