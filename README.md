# LevelUp Store – Backend

API REST de la tienda LevelUp Store hecha con Spring Boot 4, Spring Security con JWT, Spring Data JPA y MySQL.

## Requisitos

- Java 21
- Maven 3.9+
- MySQL 8 (o el perfil `h2` para probar sin base de datos)

## Ejecutar

Con MySQL local (crea la base `levelup_store` si no existe):

```bash
mvn spring-boot:run
```

Sin MySQL, con base en memoria:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Variables de entorno disponibles:

| Variable | Uso | Valor por defecto |
|---|---|---|
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/levelup_store` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de MySQL | `levelup` / `levelup` |
| `JWT_SECRET` | Clave para firmar los tokens (mín. 32 caracteres) | solo para desarrollo |
| `JWT_ACCESS_MINUTES` | Duración del access token | `15` |
| `JWT_REFRESH_DAYS` | Duración del refresh token | `7` |
| `CORS_ORIGINS` | Orígenes permitidos separados por coma | `http://localhost:5173,http://localhost:4173` |

## Documentación

Con la API arriba: `http://localhost:8080/swagger-ui.html`

Para probar endpoints protegidos, hacer login en `POST /api/auth/login`, copiar el `accessToken` y pegarlo en el botón **Authorize**.

## Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| admin | admin123 | ADMIN |
| goku | 1234 | CLIENTE |

## Endpoints principales

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/auth/login` | Público |
| POST | `/api/auth/register` | Público |
| POST | `/api/auth/refresh` | Público (requiere refresh token) |
| POST | `/api/auth/logout` | Público (revoca el refresh token) |
| GET | `/api/auth/me` | Autenticado |
| GET | `/api/productos` | Público |
| GET | `/api/productos/{id}` | Público |
| POST / PUT / DELETE | `/api/productos` | ADMIN |
| POST | `/api/pedidos` | CLIENTE / ADMIN |
| GET | `/api/pedidos/mios` | Autenticado |
| GET | `/api/pedidos` | ADMIN |
| PATCH | `/api/pedidos/{id}/estado` | ADMIN |
| GET / PUT / DELETE | `/api/usuarios` | ADMIN |

## Pruebas

```bash
mvn test
```

Las pruebas usan H2 en memoria, no necesitan MySQL.

## Generar el jar

```bash
mvn clean package
java -jar target/levelup-store-backend-1.0.0.jar
```
