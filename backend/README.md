# TaskHabit Academic — Backend (Spring Boot + PostgreSQL)

API REST con los módulos Usuarios, Tareas y Reportes. Escucha solo en `http://127.0.0.1:8000/api/`
(la app llega por `adb reverse`, ver el README principal).

## Requisitos

- JDK 21 o superior (sirve el que trae Android Studio en su carpeta `jbr`; ver el README principal).
  Maven viene incluido con `mvnw.cmd`, no hay que instalarlo.
- PostgreSQL 17 instalado en Windows.

## Base de datos (una sola vez)

1. Instalar PostgreSQL:
   ```
   winget install PostgreSQL.PostgreSQL.17 --interactive
   ```
   En el asistente: guardar la contraseña del superusuario `postgres` y dejar el puerto **5432**.
   Se instala como servicio de Windows y arranca solo con el PC.

2. Crear el usuario y la base del proyecto (pide la contraseña de `postgres`):
   ```
   "D:\PostgreSQL\17\bin\psql.exe" -U postgres -f db/crear_bd.sql
   ```
   (Ruta de este equipo; por defecto el instalador usa `C:\Program Files\PostgreSQL\17`.)

3. Listo. Las tablas las crea **Flyway** al arrancar el backend
   (`src/main/resources/db/migration/V1__esquema_inicial.sql`).

Para ver tablas y datos: abrir **pgAdmin 4** (se instala junto con PostgreSQL) →
Servers → PostgreSQL 17 → Databases → taskhabit → Schemas → public → Tables.

## Ejecutar

```
mvnw.cmd spring-boot:run      # arranca el API en el puerto 8000
mvnw.cmd test                 # pruebas (usan H2 en memoria, no necesitan PostgreSQL)
```

Si la base usa otros datos de conexión, se cambian con variables de entorno sin tocar código:
`DB_URL`, `DB_USER`, `DB_PASSWORD`.
