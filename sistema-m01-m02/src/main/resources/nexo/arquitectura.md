Arquitectura de ExamenErik
pom.xml; frontend/package.json; src/main/resources/application.yml
profile:read
ExamenErik tiene una aplicación backend Spring Boot modular que agrupa identidad y NEXO. La interfaz puede servirse desde esa aplicación o publicarse por separado. MySQL y Ollama son servicios de datos e inferencia, respectivamente.

ExamenErik gestiona identidad y acceso: registro, login, perfil, refresh, logout, cambio y recuperación de contraseña, administración por roles. No tiene ventas, pagos ni inventario. Usa Java 21, Spring Boot 4.1.1, MySQL 8.4, Flyway, HTML/CSS/JavaScript modular y esbuild. El frontend compila a frontend/dist; build:integrated conserva el desarrollo local. NEXO-Δ AI añade conversación real con un modelo Ollama local, herramientas limitadas y Predictive Access Lab. Su inteligencia usa un modelo preentrenado; no se entrenó un modelo fundacional propio. Java controla la autorización y los cálculos. El contenido generado puede ser incorrecto; distinguir datos, hipótesis y recomendaciones. El historial pertenece al usuario, caduca por configuración y puede borrarse.
