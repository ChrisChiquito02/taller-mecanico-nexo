# Punto de restauración académico

Identificador **NEXO académico cierre del 12 de noviembre de 2026**. La copia del JAR coincide byte a byte con `cierre-nexo/demo/nexo-demo.jar`. El tar contiene fuentes, pruebas, configuración declarativa y manifiesto de dependencias. Se extrajo y comparó el árbol Java antes de editar. El respaldo excluye credenciales y bases de datos.

Para verificar, ejecuta `shasum -c SHA256SUMS` desde este directorio. Para iniciar la versión académica usa [la guía vigente](../../cierre-nexo/04-Demostracion.md). Si necesitas recuperar el JAR, detén únicamente el proyecto de demostración, compara la huella de la copia con `cierre-nexo/demo/nexo-demo.sha256` y copia el JAR respaldado a esa carpeta antes de volver a iniciarlo. Conserva el archivo de credenciales local y el volumen MySQL. Extrae las fuentes en un directorio separado para revisarlas antes de restaurar archivos de trabajo.
