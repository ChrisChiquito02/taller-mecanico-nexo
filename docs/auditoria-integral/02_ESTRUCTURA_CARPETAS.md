# Estructura de carpetas

## Raíz autorizada
`/Users/andrickeliasleonchavez/Downloads/ExamenErik` contiene un solo proyecto: `sistema-m01-m02`. También existe `.idea`, sin archivos relevantes detectados. No se inspeccionaron directorios fuera de esta raíz.

```text
ExamenErik/
├── .idea/                              # metadatos de IDE; no analizados como código
├── docs/auditoria-integral/             # documentación creada por esta auditoría
└── sistema-m01-m02/
    ├── .env.example                     # plantilla de variables de ejecución
    ├── .gitignore                       # exclusiones locales y secretos
    ├── SECURITY.md                      # decisiones de seguridad declaradas
    ├── pom.xml                          # Maven, Java 21 y dependencias
    └── src/main/
        ├── java/com/sistemam01m02/
        │   ├── SistemaApplication.java  # arranque Spring Boot
        │   ├── config/                  # propiedades tipadas de seguridad/recuperación
        │   ├── identity/entity/          # entidades de usuarios, roles y permisos
        │   └── shared/persistence/       # UUIDv7
        └── resources/
            ├── application.yml          # configuración Spring/DataSource/Flyway
            └── db/migration/README.md   # política futura de migraciones
```

Se excluyeron del árbol detallado directorios generados y dependencias; no se encontraron `node_modules`, `target`, `dist`, `build` ni `.git` en el proyecto. Tampoco se encontraron `src/test`, frontend, scripts, Dockerfile, Compose, archivos SQL de migración ni reportes de pruebas.

## Relación entre carpetas
- `src/main/java`: implementación backend; `SistemaApplication` escanea las propiedades de `config` y registra componentes como `UuidV7Generator`.
- `identity/entity`: las entidades JPA requieren tablas que deberían ser creadas por migraciones en `resources/db/migration`, pero sólo hay un README.
- `resources/application.yml` conecta Spring con MySQL/Flyway y enlaza `app.security`/`app.recovery` con los records de `config`.
- `pom.xml` declara las bibliotecas utilizadas por el backend y las herramientas de prueba previstas.
