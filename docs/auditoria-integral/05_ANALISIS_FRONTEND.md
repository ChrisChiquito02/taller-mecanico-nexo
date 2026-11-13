# Análisis del frontend

No existe frontend dentro de `sistema-m01-m02` ni en el resto del espacio de trabajo autorizado: no hay `package.json`, `vite.config.*`, `src` JavaScript/TypeScript, HTML de aplicación, CSS, React, Bootstrap, rutas, componentes, formularios, recursos estáticos o pruebas frontend.

La URL `http://localhost:5173` aparece únicamente como valor de desarrollo en `.env.example` y `application.yml` para orígenes permitidos y recuperación de contraseña. Esa referencia indica la intención de integrar un frontend Vite, pero no prueba que se haya creado.

## Capacidad actual de usuario
Con el código fuente disponible no hay interfaz desde la que un usuario pueda registrarse, iniciar sesión, administrar roles, recuperar contraseña o consumir datos. Tampoco hay API expuesta en el árbol a la que conectarse.

## Brecha con backend
Las entidades de identidad y sus propiedades podrían soportar pantallas de acceso, recuperación y gestión de usuarios/roles en el futuro. Todas esas conexiones están pendientes porque faltan endpoints y frontend.
