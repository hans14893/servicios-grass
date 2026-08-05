# Servicios Grass

Backend Spring Boot para una aplicación de reservas de grass/cancha deportiva.

## Requisitos

- Java 17
- Maven 3.9+
- PostgreSQL

## Base de datos

Crear una base llamada `resergrass` o configurar variables:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/resergrass"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
$env:JWT_SECRET="0123456789012345678901234567890101234567890123456789012345678901"
```

## Ejecutar

```powershell
mvn spring-boot:run
```

Usuario administrador inicial:

- Correo: `admin@resergrass.com`
- Clave: `admin123`

## WebSocket

Endpoint SockJS/STOMP: `/ws`

Tema de disponibilidad:

```text
/topic/availability/{courtId}/{yyyy-MM-dd}
```
