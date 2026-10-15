# Estado Actual del Proyecto Heimdall: Inventario Técnico y de Dominio

Este documento presenta el inventario exhaustivo del código existente en el backend del proyecto **Heimdall** (Spring Boot 4.1.1 con Java 25), con foco en el estado de implementación del módulo **IAM** (*Identity & Access Management*), sus contratos, reglas de negocio, persistencia, seguridad y dependencias, sirviendo como línea base técnica para el diseño del modelo relacional y de dominio global.

---

## 1. Módulos, Paquetes Existentes y Estructura de Capas

El backend adopta los principios de **Clean Architecture**, **Domain-Driven Design (DDD)** y arquitectura hexagonal (puertos y adaptadores). Actualmente, la funcionalidad implementada se concentra en el módulo `iam` dentro del paquete base `com.example.heimdall`.

```
src/main/java/com/example/heimdall/
├── HeimdallApplication.java                     # Punto de entrada del servicio Spring Boot
└── iam/
    ├── domain/                                  # Capa de Dominio Puro (Aislada de frameworks)
    │   ├── model/                               # Entidades, Value Objects y Enums
    │   ├── port/
    │   │   ├── in/                              # Puertos de entrada (Casos de uso del dominio)
    │   │   └── out/                             # Puertos de salida (Contratos hacia infraestructura)
    │   └── exception/                           # Excepciones de negocio del dominio
    │
    ├── application/                             # Capa de Aplicación (Orquestación del flujo de negocio)
    │   ├── usecase/                             # Implementación de casos de uso y políticas
    │   └── dto/                                 # Comandos y resultados internos de aplicación
    │
    ├── infrastructure/                          # Capa de Infraestructura (Adaptadores tecnológicos)
    │   ├── persistence/                         # Persistencia relacional con PostgreSQL / Spring Data JPA
    │   │   ├── entity/                          # Entidades JPA (@Entity)
    │   │   ├── repository/                      # Repositorios Spring Data JPA
    │   │   ├── mapper/                          # Mapeador de persistencia Entity <-> Dominio
    │   │   └── PostgresUserRepositoryAdapter.java
    │   ├── security/                            # Adaptadores de seguridad, JWT y criptografía
    │   │   ├── config/                          # SecurityFilterChain, manejo de errores HTTP
    │   │   ├── jwt/                             # Adaptador JJWT, propiedades y filtro Bearer OncePerRequest
    │   │   └── encoder/                         # Adaptador BCrypt
    │   └── config/                              # Inyección y cableado de beans de aplicación
    │       └── IamConfiguration.java
    │
    └── presentation/                            # Capa de Presentación (Adaptadores de entrada HTTP/REST)
        ├── rest/                                # AuthController, IamExceptionHandler, AuthResponseMapper
        └── dto/
            ├── request/                         # DTOs de entrada con validación Jakarta Bean Validation
            └── response/                        # DTOs de salida serializables en JSON
```

### Reglas Arquitectónicas y Aislamiento del Dominio
* **Pureza del Dominio:** La capa `com.example.heimdall.iam.domain` está estrictamente aislada. La prueba arquitectónica `DomainArchitectureTest` verifica mediante inspección de código fuente que ninguna clase en `domain` importe bibliotecas externas o paquetes fuera de `java.*` y `com.example.heimdall.iam.domain.*`.
* **Inversión de Dependencias:** La capa de aplicación y la de infraestructura dependen del dominio. Los adaptadores de infraestructura (`PostgresUserRepositoryAdapter`, `BCryptPasswordEncoderAdapter`, `JwtTokenServiceAdapter`) implementan los puertos de salida definidos en el dominio.

---

## 2. Modelo de Dominio Actual (`iam.domain.model`)

Todos los modelos de dominio están implementados como tipos inmutables (`record` y `enum`), garantizando la validación de invariantes en el momento de la instanciación.

### 2.1 `Role` (Enum)
Define los roles admitidos en el sistema:
```java
package com.example.heimdall.iam.domain.model;

public enum Role {
    ADMIN, SCI_CHIEF, OPERATOR
}
```
* **Valores:**
  - `ADMIN`: Administrador con privilegios de gestión global.
  - `SCI_CHIEF`: Jefe del Sistema de Comando de Incidentes (*Incident Command System Chief*).
  - `OPERATOR`: Operador estándar del sistema (rol asignado por defecto al registrarse).

### 2.2 `UserId` (Record)
Value Object que encapsula el identificador único del usuario:
```java
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "User ID is required");
    }
    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }
}
```
* **Campos:** `UUID value`.
* **Invariantes:**
  - `value` no puede ser nulo (`NullPointerException("User ID is required")`).
* **Método de fábrica:** `UserId.random()` genera un nuevo UUID versión 4 en memoria antes de persistir la entidad.

### 2.3 `Email` (Record)
Value Object que encapsula y normaliza la dirección de correo electrónico:
```java
public record Email(String value) {
    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                    + "[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");

    public Email {
        if (value == null) {
            throw new IllegalArgumentException("Email is required");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        int separator = value.indexOf('@');
        if (value.length() > 254 || separator > 64 || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address");
        }
        for (String label : value.substring(separator + 1).split("\\.")) {
            if (label.length() > 63) {
                throw new IllegalArgumentException("Invalid email address");
            }
        }
    }
}
```
* **Campos:** `String value`.
* **Normalización:**
  - Se eliminan espacios en los extremos (`strip()`).
  - Se convierte a minúsculas usando `Locale.ROOT` para evitar comportamientos inconsistentes según el idioma del entorno de ejecución.
* **Invariantes y Reglas de Validación:**
  - No puede ser nulo (`IllegalArgumentException("Email is required")`).
  - Longitud total máxima: **254 caracteres** (`value.length() > 254`).
  - Longitud máxima de la parte local (previa al `@`): **64 caracteres** (`separator > 64`).
  - Cumplimiento estricto de expresión regular estándar de formato de correo (`FORMAT`).
  - Longitud máxima de cada etiqueta en la sección de dominio (separadas por punto): **63 caracteres** (`label.length() > 63`).
  - Cualquier violación lanza `IllegalArgumentException("Invalid email address")`.

### 2.4 `User` (Record)
Entidad central del agregado de identidad:
```java
public record User(UserId id, Email email, String passwordHash, Role role) {
    public User {
        Objects.requireNonNull(id, "User ID is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(role, "Role is required");
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", email=" + email + ", role=" + role + "]";
    }
}
```
* **Campos:**
  - `UserId id`: Identificador único.
  - `Email email`: Correo normalizado y validado.
  - `String passwordHash`: Hash criptográfico de la contraseña (BCrypt).
  - `Role role`: Rol de autorización.
* **Invariantes:**
  - `id`, `email`, y `role` no pueden ser nulos (`NullPointerException`).
  - `passwordHash` no puede ser nulo ni estar en blanco (`IllegalArgumentException("Password hash is required")`).
* **Protección de Datos:**
  - Sobrescribe `toString()` para excluir explícitamente `passwordHash`, evitando fugas accidentales del secreto en volcados de depuración o logs.

### 2.5 `TokenClaims` (Record)
Modelo de dominio que representa los datos de identidad extraídos de un JWT verificado:
```java
public record TokenClaims(UserId userId, Email email, Role role) {
    public TokenClaims {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(role, "Role is required");
    }
}
```
* **Campos:** `UserId userId`, `Email email`, `Role role`.
* **Invariantes:** Todos los campos son obligatorios y no nulos.

### 2.6 `AuthenticationResult` (Record)
Resultado generado tras completar exitosamente una autenticación o registro:
```java
public record AuthenticationResult(String accessToken, User user) {
    public AuthenticationResult {
        Objects.requireNonNull(user, "User is required");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }
    }

    @Override
    public String toString() {
        return "AuthenticationResult[user=" + user + "]";
    }
}
```
* **Campos:** `String accessToken`, `User user`.
* **Invariantes:** `user` no nulo; `accessToken` no nulo ni en blanco.
* **Protección de Datos:** `toString()` omite el `accessToken`.

### 2.7 Excepciones de Dominio (`iam.domain.exception`)
Todas extienden de `RuntimeException`:
1. `UserAlreadyExistsException`: Lanzada cuando el correo normalizado ya se encuentra registrado. Mensaje por defecto: `"A user with this email already exists"`.
2. `InvalidCredentialsException`: Lanzada cuando el correo no existe, la contraseña no coincide o las credenciales no satisfacen los requisitos básicos. Mensaje por defecto: `"Invalid email or password"`.
3. `UserNotFoundException`: Lanzada cuando una entidad solicitada por identificador o correo no existe. Mensaje por defecto: `"User not found"`.
4. `InvalidTokenException`: Lanzada cuando un JWT es inválido, manipulado, expirado o posee estructura incorrecta. Soporta constructor con causa (`Throwable cause`). Mensaje por defecto: `"Invalid or expired access token"`.

---

## 3. Puertos de Dominio (Entrada y Salida)

Los puertos definen las fronteras desacopladas del módulo IAM.

### 3.1 Puertos de Entrada (`iam.domain.port.in`)
Representan los contratos de los casos de uso disponibles para los clientes externos (controladores REST, adaptadores de entrada).

```java
package com.example.heimdall.iam.domain.port.in;

import com.example.heimdall.iam.domain.model.AuthenticationResult;

public interface RegisterUserUseCase {
    AuthenticationResult register(String email, String password);
}
```

```java
package com.example.heimdall.iam.domain.port.in;

import com.example.heimdall.iam.domain.model.AuthenticationResult;

public interface AuthenticateUserUseCase {
    AuthenticationResult authenticate(String email, String password);
}
```

* **Nota de diseño:** Los métodos reciben tipos básicos (`String email`, `String password`) en lugar de DTOs de presentación o aplicación, evitando acoplar el dominio al transporte HTTP.

### 3.2 Puertos de Salida (`iam.domain.port.out`)
Representan los contratos requeridos por el dominio para interactuar con infraestructura (persistencia, cifrado y emisión de tokens).

```java
package com.example.heimdall.iam.domain.port.out;

import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.User;
import com.example.heimdall.iam.domain.model.UserId;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findByEmail(Email email);
    Optional<User> findById(UserId id);
    User save(User user);
    boolean existsByEmail(Email email);
}
```

```java
package com.example.heimdall.iam.domain.port.out;

public interface PasswordEncoderPort {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
```

```java
package com.example.heimdall.iam.domain.port.out;

import com.example.heimdall.iam.domain.model.TokenClaims;
import com.example.heimdall.iam.domain.model.User;

public interface TokenServicePort {
    String generateToken(User user);
    TokenClaims validateToken(String token);
}
```

---

## 4. Casos de Uso y Reglas de Negocio

La capa de aplicación (`iam.application`) implementa la lógica de orquestación y hace cumplir las políticas del sistema.

### 4.1 Política de Contraseñas (`PasswordPolicy`)
Clase con visibilidad de paquete que encapsula las restricciones técnicas y de seguridad de las contraseñas:
```java
final class PasswordPolicy {
    static void validate(String password) {
        if (password == null || password.isBlank() || password.codePointCount(0, password.length()) < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must contain at least 8 characters and at most 72 UTF-8 bytes");
        }
    }
}
```
* **Reglas exactas:**
  1. No nula y no en blanco (`password == null || password.isBlank()`).
  2. Longitud mínima de **8 caracteres / puntos de código Unicode** (`password.codePointCount(0, password.length()) < 8`).
  3. Longitud máxima estricta de **72 bytes codificados en UTF-8** (`password.getBytes(StandardCharsets.UTF_8).length > 72`).
* **Razón de seguridad:** El algoritmo nativo de BCrypt procesa como máximo 72 bytes. Si una contraseña supera este límite, BCrypt descarta silenciosamente los bytes sobrantes. Limitar la entrada a 72 bytes UTF-8 garantiza que la contraseña completa forme parte del cálculo del hash y no un prefijo truncado.

### 4.2 Registro de Usuario (`RegisterUserService`)
Implementa `RegisterUserUseCase`:
* **Flujo de ejecución:**
  1. Instancia `Email normalizedEmail = new Email(email)` (aplica recorte de espacios y minúsculas).
  2. Valida la contraseña contra `PasswordPolicy.validate(password)`.
  3. Consulta `users.existsByEmail(normalizedEmail)`. Si ya existe, aborta lanzando `UserAlreadyExistsException`.
  4. **Rol por Defecto:** Asigna de forma invariable y fija `Role.OPERATOR`. La API pública de registro no permite autoasignarse los roles `ADMIN` ni `SCI_CHIEF`.
  5. Genera un identificador único con `UserId.random()`.
  6. Hashea la contraseña con `passwords.encode(password)` utilizando BCrypt con factor de costo 12.
  7. Guarda el usuario en el repositorio (`users.save(...)`).
  8. Genera un token JWT de acceso (`tokens.generateToken(user)`).
  9. Retorna `AuthenticationResult(accessToken, user)`.
* **Sobrecarga de Aplicación:** Dispone de un método adicional `register(RegisterCommand command)` que devuelve `AuthResult`.

### 4.3 Autenticación y Mitigación de Enumeración (`AuthenticateUserService`)
Implementa `AuthenticateUserUseCase`:
* **Protección contra Enumeración de Cuentas (Timing Attacks):**
  - En su constructor precalcula un hash dummy con BCrypt:
    ```java
    this.dummyHash = passwords.encode("heimdall-dummy-password-for-login");
    ```
* **Flujo de ejecución:**
  1. Intenta normalizar el correo (`new Email(email)`) y validar la contraseña (`PasswordPolicy.validate(password)`).
  2. Si ocurre un `IllegalArgumentException` (formato o longitud errónea), captura el error y lanza inmediatamente `InvalidCredentialsException`, impidiendo distinguir si falló el correo o la contraseña.
  3. Busca al candidato en el repositorio: `users.findByEmail(normalizedEmail)`.
  4. Realiza la comprobación de contraseña contra el hash del usuario o contra el `dummyHash`:
     ```java
     boolean matches = passwords.matches(password, candidate.map(User::passwordHash).orElse(dummyHash));
     ```
     **Efecto:** El tiempo de cálculo criptográfico de BCrypt se ejecuta siempre, tanto si la cuenta existe como si no existe. Esto evita que un atacante determine la existencia de correos en el sistema midiendo discrepancias de tiempo de respuesta.
  5. Si el candidato no existe o la contraseña no coincide (`candidate.isEmpty() || !matches`), lanza `InvalidCredentialsException`.
  6. Emite el token JWT (`tokens.generateToken(user)`) y retorna `AuthenticationResult(accessToken, user)`.
* **Sobrecarga de Aplicación:** Ofrece `authenticate(LoginCommand command)` retornando `AuthResult`.

### 4.4 DTOs de Capa de Aplicación
- `RegisterCommand(String email, String password)`: Oculta `password` en `toString()`.
- `LoginCommand(String email, String password)`: Oculta `password` en `toString()`.
- `AuthResult(String accessToken, UserId userId, Email email, Role role)`: Oculta `accessToken` en `toString()`.

---

## 5. Persistencia y Base de Datos

### 5.1 Entidad JPA (`UserJpaEntity`)
Mapea la tabla relacional `iam_users`:
```java
@Entity
@Table(name = "iam_users", uniqueConstraints = @UniqueConstraint(name = "uk_iam_users_email", columnNames = "email"))
public class UserJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;
    // ...
}
```

* **Mapeo de Columnas:**
  - `id`: Tipo `UUID`, clave primaria.
  - `email`: Tipo `VARCHAR(254)`, no nulo, con restricción de unicidad `uk_iam_users_email`.
  - `password_hash`: Tipo `VARCHAR(100)`, no nulo.
  - `role`: Tipo `VARCHAR(32)`, no nulo, mapeado como cadena (`EnumType.STRING`).

### 5.2 Contenido Completo de `src/main/resources/db/schema.sql`
```sql
CREATE TABLE IF NOT EXISTS iam_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT uk_iam_users_email UNIQUE (email),
    CONSTRAINT ck_iam_users_role CHECK (role IN ('ADMIN', 'SCI_CHIEF', 'OPERATOR'))
);
```

### 5.3 Análisis de Correspondencia: Java `Role` vs. SQL `CHECK`
* **En Java (`Role.java`):** `ADMIN`, `SCI_CHIEF`, `OPERATOR`.
* **En SQL (`schema.sql`):** `CHECK (role IN ('ADMIN', 'SCI_CHIEF', 'OPERATOR'))`.
* **Diagnóstico de Correspondencia:**
  - Actualmente existe una **coincidencia exacta** entre los tres valores del enum en Java y los literales de la restricción `CHECK` en la base de datos.
* **Observación y Riesgo Futuro:**
  - El uso de un `CHECK` codificado rígidamente en el DDL acopla la base de datos a los valores del enum. Si el dominio incorpora nuevos roles (por ejemplo, `DEPUTY_SCI_CHIEF` o roles multi-organización), la aplicación fallará con `DataIntegrityViolationException` a menos que se ejecute una migración de esquema sobre la restricción `ck_iam_users_role`.
  - La configuración `spring.jpa.hibernate.ddl-auto=validate` verifica que la tabla y columnas existan con los tipos correctos, pero Hibernate no valida el contenido interno de las expresiones `CHECK`.

### 5.4 Control de Concurrencia en Persistencia (`PostgresUserRepositoryAdapter`)
* Implementa `save(User user)` ejecutando `repository.saveAndFlush(...)`.
* El `flush` inmediato fuerza la ejecución de las sentencias SQL dentro del ciclo de la llamada, permitiendo interceptar violaciones de clave única (`uk_iam_users_email`) producidas por registros concurrentes simultáneos.
* Si se produce una violación de unicidad con el nombre `uk_iam_users_email`, se traduce limpiamente a `UserAlreadyExistsException`.

---

## 6. Configuración JWT y Endpoints REST Expuestos

### 6.1 Configuración de Tokens JWT
La configuración se define mediante `JwtProperties` bajo el prefijo `heimdall.jwt` (definido en `application.properties`):
* `heimdall.jwt.secret`: Secreto en Base64 (`${HEIMDALL_JWT_SECRET:}`).
  - Invariante: Debe ser Base64 válido y contener **al menos 32 bytes decodificados** (256 bits requeridos para `HmacSHA256`).
* `heimdall.jwt.expiration`: Tiempo de validez (`${HEIMDALL_JWT_EXPIRATION:PT15M}`).
  - Por defecto: 15 minutos (`PT15M`).
  - Invariante: Mínimo 1 segundo (`>= Duration.ofSeconds(1)`).
* `heimdall.jwt.issuer`: Emisor (`${HEIMDALL_JWT_ISSUER:heimdall}`).
  - Por defecto: `"heimdall"`. No puede estar en blanco.

### 6.2 Claims y Estructura del JWT
Generado mediante `JwtTokenServiceAdapter` con JJWT 0.12.6:
* **Algoritmo:** `HS256` (HMAC-SHA256).
* **Claims en el Payload:**
  - `iss`: Emisor configurado (`heimdall`).
  - `sub`: Identificador único del usuario (`user.id().value().toString()`).
  - `email`: Correo normalizado (`user.email().value()`).
  - `role`: Nombre del rol (`user.role().name()`).
  - `iat`: Timestamp de emisión (en segundos UTC enteros).
  - `exp`: Timestamp de expiración (`iat + expiration`).
* **Validación:**
  - Requiere el algoritmo `HS256`.
  - Verifica la firma con la clave simétrica y exige que el `issuer` coincida.
  - Comprueba que `issuedAt` no sea futuro y que `expiration` sea posterior a `now`.
  - Verifica que `sub` sea un UUID canónico y reconstruye los objetos de dominio `UserId`, `Email` y `Role`.

### 6.3 Endpoints REST Expuestos (`AuthController` en `/api/v1/auth`)

| Método HTTP | Ruta | Seguridad / Acceso | Request Body | Códigos de Respuesta | Descripción |
|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Público (`permitAll`) | `RegisterRequestDto` | `201 CREATED`<br>`400 BAD REQUEST`<br>`409 CONFLICT` | Registra usuario con rol `OPERATOR`, persiste y retorna token. Añade encabezado `Cache-Control: no-store`. |
| `POST` | `/api/v1/auth/login` | Público (`permitAll`) | `LoginRequestDto` | `200 OK`<br>`400 BAD REQUEST`<br>`401 UNAUTHORIZED` | Autentica credenciales con protección anti-timing y retorna token. Añade encabezado `Cache-Control: no-store`. |
| `GET` | `/api/v1/auth/me` | Protegido (`Bearer <token>`) | Ninguno | `200 OK`<br>`401 UNAUTHORIZED` | Retorna los datos del usuario autenticado (`userId`, `email`, `role`) a partir de los claims del JWT. No consulta la BD. |

### 6.4 DTOs de Entrada y Salida REST
* **`RegisterRequestDto`:** `email` (`@NotBlank`, `@Size(max = 254)`), `password` (`@NotBlank`, `@Size(max = 72)`).
* **`LoginRequestDto`:** `email` (`@NotBlank`, `@Size(max = 254)`), `password` (`@NotBlank`, `@Size(max = 72)`).
* **`AuthResponseDto`:** `accessToken` (String), `tokenType` (`"Bearer"`), `expiresIn` (segundos enteros `long`), `userId` (UUID), `email` (String), `role` (`Role`).
* **`UserResponseDto`:** `userId` (UUID), `email` (String), `role` (`Role`).

### 6.5 Manejador de Excepciones HTTP (`IamExceptionHandler`)
Retorna respuestas basadas en el estándar RFC 7807 (`ProblemDetail`):
- `UserAlreadyExistsException` $\rightarrow$ `409 Conflict` ("Ya existe un usuario con ese correo").
- `InvalidCredentialsException` $\rightarrow$ `401 Unauthorized` ("Credenciales inválidas").
- `UserNotFoundException` $\rightarrow$ `404 Not Found` ("Usuario no encontrado").
- `IllegalArgumentException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException` $\rightarrow$ `400 Bad Request` ("Solicitud inválida; revisa el correo y la contraseña"). No expone los valores rechazados para no filtrar datos sensibles.
- Errores de filtro (`SecurityErrorResponse`):
  - `401 Unauthorized`: Emite cabecera `WWW-Authenticate: Bearer` y JSON `{"status":401,"error":"Unauthorized","message":"Authentication is required or the access token is invalid"}`.
  - `403 Forbidden`: Emite JSON `{"status":403,"error":"Forbidden","message":"You do not have permission to access this resource"}`.

---

## 7. Inventario de Dependencias en `build.gradle.kts`

A continuación se detalla el análisis de las dependencias declaradas en `build.gradle.kts`, clasificadas según su uso real en el código:

| Dependencia Declarada | Ámbito | Estado de Uso | Justificación en el Código Actual |
|---|---|---|---|
| `org.springframework.boot:spring-boot-starter-webmvc` | `implementation` | **USADA** | Base para los controladores REST (`AuthController`), manejo de peticiones HTTP, mapeo JSON con Jackson y servidor web embebido (Tomcat). |
| `org.springframework.boot:spring-boot-starter-security` | `implementation` | **USADA** | Configuración de `SecurityFilterChain`, filtro de autenticación `OncePerRequestFilter`, codificador de contraseñas BCrypt y soporte para `@EnableMethodSecurity`. |
| `org.springframework.boot:spring-boot-starter-validation` | `implementation` | **USADA** | Validación declarativa de DTOs HTTP (`@Valid`, `@NotBlank`, `@Size`) en `RegisterRequestDto` y `LoginRequestDto`. |
| `io.jsonwebtoken:jjwt-api:0.12.6` | `implementation` | **USADA** | API para construcción, firma y verificación criptográfica de tokens JWT en `JwtTokenServiceAdapter`. |
| `io.jsonwebtoken:jjwt-impl:0.12.6` | `runtimeOnly` | **USADA** | Implementación de runtime de JJWT. |
| `io.jsonwebtoken:jjwt-jackson:0.12.6` | `runtimeOnly` | **USADA** | Serializador/deserializador JSON de JJWT para los claims del payload. |
| `org.springframework.boot:spring-boot-starter-data-jpa` | `implementation` | **USADA** | Motor ORM Hibernate y Spring Data JPA para la entidad `UserJpaEntity` y el repositorio `SpringDataUserRepository`. |
| `org.postgresql:postgresql` | `runtimeOnly` | **USADA** | Driver JDBC oficial para la conexión con el motor relacional PostgreSQL especificado en `application.properties`. |
| `org.springframework.boot:spring-boot-devtools` | `developmentOnly` | **HERRAMIENTA DEV** | Utilidad para reinicio automático en entorno de desarrollo. |
| `org.springframework.boot:spring-boot-starter-data-jdbc` | `implementation` | **APARENTEMENTE NO USADA** | No existen entidades `@Table` de Spring Data JDBC ni repositorios basados en JDBC puro. El acceso relacional se realiza en su totalidad mediante JPA. En `application-test.properties` se deshabilita con `spring.data.jdbc.repositories.enabled=false`. |
| `org.springframework.boot:spring-boot-starter-data-mongodb` | `implementation` | **APARENTEMENTE NO USADA** | No hay documentos `@Document`, repositorios Mongo ni configuración relacionada en el código. En `application-test.properties` se deshabilita con `spring.data.mongodb.repositories.enabled=false`. |
| `org.springframework.boot:spring-boot-starter-mongodb` | `implementation` | **APARENTEMENTE NO USADA** | Dependencia redundante / no utilizada junto a la anterior. |
| `org.springframework.boot:spring-boot-starter-data-redis` | `implementation` | **APARENTEMENTE NO USADA** | No hay plantillas `RedisTemplate`, servicios de caché `@Cacheable` ni repositorios de sesión en Redis. En `application-test.properties` se deshabilita con `spring.data.redis.repositories.enabled=false`. |
| `org.mariadb.jdbc:mariadb-java-client` | `runtimeOnly` | **APARENTEMENTE NO USADA** | Driver de MariaDB presente en el classpath pero sin configuración activa; la aplicación apunta exclusivamente a PostgreSQL (`jdbc:postgresql://`). |
| `com.h2database:h2` | `testRuntimeOnly` | **USADA EN TESTS** | Base de datos en memoria para la ejecución de la suite de pruebas unitarias y de integración (`application-test.properties`). |
| Starters de test (`webmvc-test`, `security-test`, `data-jpa-test`) | `testImplementation` | **USADAS EN TESTS** | Utilizadas en las pruebas de `src/test/java`. |
| Starters de test (`data-jdbc-test`, `data-mongodb-test`, `data-redis-test`, `mongodb-test`) | `testImplementation` | **APARENTEMENTE NO USADAS** | Dependencias de pruebas asociadas a los motores no utilizados en el código actual. |

---

## 8. Observaciones e Inconsistencias Relevantes para un Futuro Modelo Relacional

Al analizar el código actual frente a los requerimientos de un sistema integral de alerta temprana y gestión de riesgos territoriales (como se describe en `ANALISIS_DOMINIO.md`), se evidencian las siguientes observaciones y vacíos técnicos:

### 8.1 Campos de Usuario Faltantes para Operación Real
La tabla `iam_users` actual posee un diseño minimalista (`id`, `email`, `password_hash`, `role`). Para un entorno productivo y de gestión operativa de emergencias, faltan atributos esenciales:
1. **Datos de Identificación Personal y Contacto:**
   - No existen nombres ni apellidos (`first_name`, `last_name` o `full_name`).
   - No hay número telefónico de contacto (`phone_number`). En el documento de análisis del dominio (`ANALISIS_DOMINIO.md`), los actores clave como el Jefe SCI (`sciBoss`) y su Auxiliar (`auxiliarSciBoss`) requieren obligatoriamente canales de contacto (`sciBossContact`, `auxiliarSciBossContact`) para la emisión y recepción inmediata de alertas tempranas.
2. **Ciclo de Vida y Auditoría de la Cuenta:**
   - No existe un campo para deshabilitar o suspender cuentas (`is_active`, `status`). Actualmente, un usuario no puede ser bloqueado sin ser borrado físicamente de la base de datos.
   - No hay marcas de tiempo de auditoría (`created_at`, `updated_at`, `last_login_at`).
   - No hay confirmación o verificación de correo (`is_email_verified`).

### 8.2 Inexistencia del Concepto de Organización / Tenancy (Multi-tenant)
- En el código actual, los usuarios son globales y planos.
- En el dominio de negocio, las alertas y umbrales están asociados a **Proyectos Territoriales** específicos (`idProject`, `projectName`, `stateName`, `cityName`, coordenadas geográficas).
- No existe en el modelo de datos ninguna relación entre un usuario y una organización, cliente o proyecto determinado (`organization_id`, `project_id`).

### 8.3 Asignación y Gestión de Roles
- **Ausencia de Flujo Administrativo:** El auto-registro asigna irrevocablemente `Role.OPERATOR`. No existe ningún endpoint, servicio o caso de uso administrativo para que un `ADMIN` promueva a un usuario a `SCI_CHIEF` o cree cuentas de mando operativo.
- **Relación 1 a 1 Rígida:** El rol está almacenado como un `VARCHAR(32)` directo en la fila del usuario. Si un actor del sistema requiere cumplir múltiples funciones en diferentes proyectos (por ejemplo, Operador en el Proyecto A, pero Jefe SCI en el Proyecto B), el modelo requerirá una relación normalizada de muchos a muchos (`user_roles` o `project_memberships`).

### 8.4 Gestión de Sesiones y Ausencia de Revocación
- El esquema actual es 100% *stateless* basado únicamente en un Access Token JWT de 15 minutos.
- No se cuenta con **Refresh Tokens** ni tabla de persistencia de tokens/sesiones. Si un usuario cambia de rol, es despedido o se revoca su contraseña, el token emitido seguirá siendo aceptado por el backend hasta que transcurran los 15 minutos, sin mecanismo de invalidación inmediata.

### 8.5 Estrategia de Migraciones de Base de Datos
- Actualmente se depende de `src/main/resources/db/schema.sql` ejecutado mediante `spring.sql.init.mode=always` y validado por Hibernate (`spring.jpa.hibernate.ddl-auto=validate`).
- Este enfoque es adecuado únicamente para un prototipo inicial. Para evolucionar el modelo hacia múltiples entidades relacionales (proyectos, estaciones climáticas, umbrales, alertas, miembros), será imperativo incorporar una herramienta de migraciones versionadas y reproducibles como **Flyway** o **Liquibase**.
