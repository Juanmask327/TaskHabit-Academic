# TaskHabit Academic

Aplicación Android para organizar tareas y hábitos académicos, registrar asistencia a clase con código QR y medir el cumplimiento con reportes. La app trabaja con su propia API REST en Spring Boot y una base de datos PostgreSQL.

![Android](https://img.shields.io/badge/Android-Java-3DDC84?logo=android&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth-FFCA28?logo=firebase&logoColor=black)

<!-- Demo: reemplazar por el video o GIF de la app funcionando -->
<!-- ![Demo](docs/capturas/demo.gif) -->

## Funcionalidades

- **Tareas y hábitos:** crear, editar, completar y eliminar, con tipo (tarea o hábito), prioridad y fecha de entrega. Se guardan en el teléfono (Room) y se sincronizan con la API.
- **Asistencia con QR y GPS:** el docente abre una sesión de clase y genera un código QR; el estudiante lo escanea con la cámara (CameraX + ML Kit) y la app valida su ubicación antes de registrar la asistencia.
- **Reportes de cumplimiento:** para un periodo, la API calcula cuántas tareas y hábitos se completaron y el porcentaje de cumplimiento.
- **Cuentas con roles:** registro e inicio de sesión con Firebase Authentication, con rol de docente o estudiante.

## Arquitectura

```
App Android (Java)  ──HTTP + token de Firebase──▶  API REST (Spring Boot)  ──JDBC──▶  PostgreSQL
  Room (datos locales)                               Spring Security (JWT)             Flyway (migraciones)
  CameraX + ML Kit (QR)                              JPA / Hibernate
  GPS (Play Services)
```

- **Seguridad:** cada petición lleva el token de Firebase y la API lo valida contra el proyecto antes de responder. Cada usuario solo ve y modifica sus propios datos.
- **Base de datos:** el esquema lo crea Flyway con migraciones versionadas. Las reglas de negocio están protegidas también en la base (roles y prioridades válidos, periodos de reporte coherentes, borrado lógico de usuarios).
- **Red:** la API solo escucha en `127.0.0.1` del computador. El teléfono llega a ella por un túnel de `adb reverse` que se crea solo en cada compilación, así el servidor nunca queda expuesto en la red.

## Capturas

<!-- Agregar las imágenes en docs/capturas/ y descomentar -->
<!--
| Tareas | Escáner QR | Reportes |
|---|---|---|
| ![Tareas](docs/capturas/tareas.png) | ![QR](docs/capturas/qr.png) | ![Reportes](docs/capturas/reportes.png) |
-->

## Estructura

```
app/       Aplicación Android: auth, tareas, asistencia, reportes
backend/   API REST: módulos usuario, tarea y reporte; migraciones en src/main/resources/db/migration
```

## Ejecutar el proyecto

Solo se necesita **Android Studio** y **PostgreSQL 17**. Maven y Gradle vienen incluidos (`mvnw.cmd`, `gradlew.bat`) y el Java de Android Studio sirve también para el backend.

### 1. Firebase

Crear un proyecto en Firebase con Authentication (correo y contraseña) y una app Android con el paquete `com.taskhabit.academic`. Descargar su `google-services.json` y guardarlo en `app/` (ver `app/google-services.json.example`). Configurar el mismo ID de proyecto en el backend con la variable de entorno `FIREBASE_PROJECT_ID`.

### 2. Base de datos (una sola vez)

1. Instalar PostgreSQL 17 (`winget install PostgreSQL.PostgreSQL.17 --interactive`), anotar la contraseña de `postgres` y dejar el puerto **5432**.
2. Crear el usuario y la base del proyecto:
   ```powershell
   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -f backend\db\crear_bd.sql
   ```
   Las tablas las crea el backend al arrancar (Flyway).

### 3. Backend

Desde la carpeta `backend`:
```powershell
# Solo si no hay un JDK 21+ instalado: usar el que trae Android Studio
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

.\mvnw.cmd spring-boot:run
```
Está listo cuando aparece `Tomcat started on port 8000`.

### 4. App

1. Abrir la carpeta raíz en Android Studio y esperar el Gradle Sync.
2. Conectar el celular por USB (con depuración USB) o usar un emulador.
3. Pulsar ▶ Run. No hay que configurar IPs ni el firewall.

Si se desconecta el cable, volver a pulsar ▶ Run (o ejecutar `adb reverse tcp:8000 tcp:8000`). Para un emulador sin `adb reverse`, agregar a `local.properties`: `taskhabit.api.url=http://10.0.2.2:8000/api/`.

## Pruebas

```powershell
cd backend
.\mvnw.cmd test
```
Pruebas de integración del CRUD sobre una base H2 en memoria (no necesitan PostgreSQL).

## Equipo

| Integrante | Responsabilidad |
|---|---|
| **Juan Manuel Benítez Skolik** | Backend: modelo de datos en PostgreSQL, migraciones, API REST con el CRUD de usuarios, tareas y reportes, y sus pruebas |
| **Allan Gómez Pérez** | App Android e integración con Firebase |
| **Martín Armando Bonilla** | App Android e integración con Firebase |

Desarrollado en la materia de Aplicaciones Móviles de Ingeniería de Software, Uniempresarial (2026).
