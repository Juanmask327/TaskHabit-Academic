-- =====================================================================================
--  Se ejecuta UNA sola vez, conectado como el superusuario "postgres":
--     psql -U postgres -f db/crear_bd.sql
--  Crea el usuario y la base de datos que usa el backend (ver application.properties).
--  Las TABLAS no se crean aquí: las crea Flyway al arrancar el backend (V1__esquema_inicial.sql).
-- =====================================================================================

-- Usuario (rol) con el que el backend se conecta. Solo desarrollo: en producción usar otra contraseña.
CREATE USER taskhabit WITH PASSWORD 'taskhabit';

-- Base de datos cuyo dueño es ese usuario: así puede crear y modificar sus tablas.
CREATE DATABASE taskhabit OWNER taskhabit ENCODING 'UTF8';
