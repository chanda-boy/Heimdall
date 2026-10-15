# Planificación de Arquitectura y Diseño: Módulo IAM (Identity & Access Management)

Este documento detalla la arquitectura, el diseño DDD / Clean Architecture y los conceptos fundamentales de seguridad basados en JWT para el módulo **IAM** del backend **Heimdall**.

---

## 1. ¿Qué es IAM y cuál es su objetivo en Heimdall?
**IAM** (*Identity and Access Management*) es el Bounded Context responsable de:
1. **Identidad:** Registrar usuarios, almacenar sus credenciales de manera segura (con algoritmos irreversibles como BCrypt) y gestionar sus datos de perfil.
2. **Autenticación:** Verificar quién dice ser el usuario (proceso de Login comprobando correo y contraseña).
3. **Autorización:** Asignar y validar permisos/roles (ej. `ADMIN`, `SCI_CHIEF`, `OPERATOR`) para determinar qué recursos puede manipular un usuario autenticado.

---

## 2. Conceptos Clave sobre JWT (Guía Didáctica)

### 2.1 ¿Por qué usamos JWT en vez de sesiones tradicionales de servidor?
En aplicaciones web clásicas (como las que usan JSP o Thymeleaf), el servidor crea una sesión en memoria (`JSESSIONID`) por cada usuario conectado. 
* **Problema:** Si el backend crece y tenemos 3 servidores corriendo detrás de un balanceador de carga, el servidor B no sabrá qué usuario inició sesión en el servidor A a menos que sincronicemos la memoria entre ellos (complejo y costoso).
* **Solución Stateless (JWT):** Con **JWT (JSON Web Token)**, el servidor **no guarda sesiones en memoria**. Cuando un usuario se autentica exitosamente, el servidor genera un token firmado criptográficamente y se lo entrega al cliente. El cliente lo almacena (en local storage, memoria o secure storage) y lo envía en el header HTTP de cada petición:
  ```http
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  ```
  Cualquier servidor que tenga la **clave secreta** puede verificar la autenticidad del token instantáneamente, sin consultar la base de datos ni consultar memoria compartida.

### 2.2 Estructura de un JWT
Un JWT consta de 3 secciones separadas por un punto (`.`):

1. **Header (Cabecera):**
   ```json
   {
     "alg": "HS256",
     "typ": "JWT"
   }
   ```
2. **Payload (Cuerpo / Claims):**
   Contiene los datos del usuario. Los datos estándar se llaman *claims*:
   * `sub` (*Subject*): Identificador único del usuario (ej. UUID).
   * `email`: Correo electrónico.
   * `role`: Rol del usuario.
   * `iat` (*Issued At*): Timestamp de creación.
   * `exp` (*Expiration*): Timestamp de expiración.
   
   > ⚠️ **REGLA DE ORO DE SEGURIDAD:** El payload está codificado en Base64, **NO encriptado**. Cualquiera puede leer su contenido si intercepta el token. Por lo tanto, **jamás** se deben incluir contraseñas, claves privadas ni datos ultraconfidenciales en el payload.

3. **Signature (Firma Criptográfica):**
   Se genera aplicando el algoritmo elegido (HMAC-SHA256) al Header y al Payload usando una **Secret Key** (clave secreta privada) que solo conoce el servidor backend:
   ```
   HMACSHA256(
     base64UrlEncode(header) + "." + base64UrlEncode(payload),
     secret_key
   )
   ```
   Si un usuario malintencionado modifica el payload (por ejemplo, cambia su rol de `OPERATOR` a `ADMIN`), la firma resultante no coincidirá con la calculada por el servidor, y el token será rechazado automáticamente (HTTP 401 Unauthorized).

---

## 3. Arquitectura del Módulo IAM (Clean Architecture / DDD)

Para evitar atar nuestro núcleo de negocio al framework (Spring Security o librerías de JWT), organizamos el código en 4 capas concéntricas:

```
src/main/java/com/example/heimdall/iam/
├── domain/                                   # Capa 1: Dominio Puro (Cero dependencias externas)
│   ├── model/
│   │   ├── User.java                         # Entidad de dominio con reglas e invariantes
│   │   ├── Role.java                         # Enum de roles
│   │   ├── UserId.java                       # Value Object para el ID
│   │   └── Email.java                        # Value Object que valida formato de correo
│   ├── port/
│   │   ├── in/                               # Interfaces de entrada (Casos de uso)
│   │   │   ├── RegisterUserUseCase.java
│   │   │   └── AuthenticateUserUseCase.java
│   │   └── out/                              # Interfaces de salida (Contratos hacia infra)
│   │       ├── UserRepositoryPort.java       # Contrato para guardar y buscar usuarios
│   │       ├── PasswordEncoderPort.java      # Contrato para hashear y verificar contraseñas
│   │       └── TokenServicePort.java         # Contrato para generar y validar JWT
│   └── exception/
│       ├── UserAlreadyExistsException.java
│       ├── InvalidCredentialsException.java
│       └── UserNotFoundException.java
│
├── application/                              # Capa 2: Aplicación (Orquestación del flujo)
│   ├── usecase/
│   │   ├── RegisterUserService.java         # Implementa RegisterUserUseCase
│   │   └── AuthenticateUserService.java     # Implementa AuthenticateUserUseCase
│   └── dto/
│       ├── RegisterCommand.java
│       ├── LoginCommand.java
│       └── AuthResult.java
│
├── infrastructure/                           # Capa 3: Adaptadores Tecnológicos
│   ├── persistence/                          # Adaptador de Base de Datos (PostgreSQL)
│   │   ├── entity/UserJpaEntity.java         # @Entity JPA con anotaciones de Hibernate
│   │   ├── repository/SpringDataUserRepository.java
│   │   ├── mapper/UserPersistenceMapper.java
│   │   └── PostgresUserRepositoryAdapter.java # Implementa UserRepositoryPort
│   ├── security/                             # Adaptador de Seguridad (Spring Security & JJWT)
│   │   ├── config/SecurityConfiguration.java # Configura SecurityFilterChain (Stateless, CSRF off)
│   │   ├── jwt/
│   │   │   ├── JwtProperties.java            # Lee heimdall.jwt.secret y expiration
│   │   │   ├── JwtTokenServiceAdapter.java   # Implementa TokenServicePort con JJWT 0.12.x
│   │   │   └── JwtAuthenticationFilter.java  # Filtro OncePerRequestFilter que extrae el Bearer token
│   │   └── encoder/
│   │       └── BCryptPasswordEncoderAdapter.java # Implementa PasswordEncoderPort con BCrypt
│
└── presentation/                             # Capa 4: Adaptadores de Entrada (Controladores REST)
    ├── rest/
    │   └── AuthController.java               # POST /api/v1/auth/register y POST /api/v1/auth/login
    └── dto/
        ├── request/RegisterRequestDto.java
        ├── request/LoginRequestDto.java
        └── response/AuthResponseDto.java
```

---

## 4. Reparto de Trabajo y Dinámica Colaborativa

| Área | Quién lo hace | Descripción |
| :--- | :--- | :--- |
| **Dominio Core** | **Usuario** | Diseñar y escribir `User.java`, `Role.java`, reglas de validación en `Email.java`. |
| **Contratos (Puertos)** | **Asistente** | Crear interfaces `UserRepositoryPort`, `TokenServicePort`, `PasswordEncoderPort`. |
| **Casos de Uso** | **Usuario / Asistente** | Escribir los servicios de aplicación (`RegisterUserService`, `AuthenticateUserService`). |
| **Seguridad e Infraestructura JWT** | **Asistente** | Escribir la configuración de Spring Security, el filtro `JwtAuthenticationFilter`, el generador JJWT y los mappers JPA, explicando paso a paso cómo funciona cada método. |
| **Controlador REST y DTOs** | **Asistente** | Endpoints de entrada y validaciones con `@Valid`. |
| **Auditoría de Arquitectura** | **Subagente** | Verificar que `domain/` no importe Spring ni librerías externas. |

## 5. Estado de implementación

El módulo IAM implementa registro, login y consulta del usuario autenticado, junto con persistencia PostgreSQL, BCrypt y autenticación JWT sin estado. Los contratos REST, diagramas, decisiones de arquitectura, configuración y límites conocidos se documentan en la [guía de IAM](../../../../../../../docs/IAM.md).

Algunas decisiones concretan o ajustan este diseño inicial:

* Los puertos de entrada reciben correo y contraseña como `String` y devuelven `AuthenticationResult` del dominio. Los DTOs HTTP permanecen en presentación; esto evita que el dominio dependa de DTOs de aplicación.
* El registro público siempre asigna `OPERATOR`; el JSON no permite autoasignar `ADMIN` ni `SCI_CHIEF`. No se incluye todavía un flujo administrativo para cambiar roles.
* `/api/v1/auth/me` devuelve `userId`, `email` y `role`, de forma coherente con la respuesta de registro/login.
* `AuthResponseMapper` pertenece a presentación. La configuración de infraestructura le proporciona el `expiresIn` en segundos, así el controlador no depende de `JwtProperties`.
* El esquema inicial se crea con `schema.sql` al arrancar (`spring.sql.init.mode=always`) y Hibernate solo lo valida (`ddl-auto=validate`). Es un bootstrap para este alcance; al evolucionar las tablas se requieren migraciones versionadas.
* Las pruebas Spring activan el perfil `test`, que carga `src/test/resources/application-test.properties` y configura H2 en modo de compatibilidad PostgreSQL. Esto no verifica el comportamiento contra un servidor PostgreSQL real; cualquier afirmación de compatibilidad del motor requiere una ejecución de integración contra PostgreSQL.
