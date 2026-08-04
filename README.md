# Credit Scoring Engine

Motor de scoring crediticio explicable para el sector financiero. Evalúa el riesgo de un solicitante de crédito, devuelve una decisión (aprobar / rechazar / revisión manual) y explica el porqué de esa decisión de forma trazable y auditable, tal como exige la regulación financiera ("right to explanation").

Proyecto de portafolio orientado a roles **Backend + IA**: el foco no es solo que el modelo prediga bien, sino demostrar un backend con **auditoría, versionado y governance** de decisiones automatizadas, con explicabilidad como ciudadano de primera clase.

## Estado actual

**Fase 1 — Esqueleto backend** completada: autenticación JWT, CRUD de solicitudes de crédito y persistencia en Postgres. El motor de ML (scoring + explicabilidad) llega en fases posteriores — ver [Roadmap](#roadmap).

## Arquitectura

```
[Cliente/API Gateway]
        │
        ▼
[Spring Boot API] ──► [PostgreSQL] (solicitudes, decisiones, auditoría, versiones de modelo)
        │
        ├──► [ML Service (FastAPI)] ──► [Modelo activo + SHAP]   (fase 2+)
        │           │
        │           └──► /what-if (simulación de variables)      (fase 5)
        │
        └──► [Model Registry] (metadata de versiones, para trazabilidad)   (fase 4)
```

El backend Java es responsable de la orquestación, persistencia y governance; el futuro servicio Python vivirá en un microservicio separado, responsable exclusivamente de scoring y explicabilidad.

### Backend: arquitectura hexagonal (ports & adapters)

Cada feature (`auth`, `creditapplication`) se organiza en tres capas internas, en vez de capas técnicas genéricas a nivel raíz:

```
com.florez.backend
├── config/                 Wiring de Spring: seguridad, auditoría JPA, conexión de los servicios de "application" con sus adapters
├── auth/
│   ├── domain/              Modelo (User, Role) y ports (in: casos de uso: out: contratos hacia infraestructura) — sin dependencias de Spring/JPA
│   ├── application/         AuthService: implementa los casos de uso orquestando el dominio a través de los ports
│   └── infrastructure/
│       ├── in/web/          AuthController + DTOs
│       └── out/             Adapters: persistencia (JPA), seguridad (JWT, BCrypt)
├── creditapplication/       Mismo esquema domain → application → infrastructure
└── common/                  Excepciones de dominio, manejo de errores centralizado, entidad base de auditoría JPA
```

**Regla de dependencia:** `domain` no conoce Spring ni JPA; `application` solo depende de `domain` (los ports); `infrastructure` implementa los ports de salida y consume los casos de uso, nunca al revés.

## Stack técnico

- **Backend:** Java 21 + Spring Boot 4 (Spring Web, Spring Data JPA, Spring Security con JWT, Bean Validation)
- **Base de datos:** PostgreSQL, migraciones versionadas con Flyway (nunca `ddl-auto`)
- **Auditoría de solicitudes:** timestamps automáticos (`createdAt`/`updatedAt`) y soft-delete (`deletedAt`), acorde al enfoque de trazabilidad del proyecto
- **Seguridad:** JWT stateless (JJWT), contraseñas con BCrypt, roles `ADMIN` / `ANALYST`
- **Observabilidad:** Spring Actuator + logs estructurados en JSON (Logstash encoder)
- **Testing:** JUnit 5 + Mockito (unitarios) y Testcontainers + Postgres real (integración)
- **Infraestructura:** Docker Compose para Postgres

## Cómo levantar el entorno

### 1. Variables de entorno

```bash
cp .env.example .env
# editar .env: credenciales de Postgres, JWT_SECRET (mínimo 32 caracteres) y el admin inicial
```

### 2. Base de datos

```bash
docker-compose up -d
```

### 3. Backend

```bash
cd backend
./mvnw spring-boot:run
```

Al arrancar por primera vez (tabla `users` vacía), se crea automáticamente el usuario `ADMIN` inicial con las credenciales de `ADMIN_USERNAME` / `ADMIN_PASSWORD` definidas en `.env` — nunca hay credenciales hardcodeadas en el código.

### 4. Probar la API

```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<ADMIN_PASSWORD del .env>"}'

# Crear una solicitud de crédito (usar el token devuelto arriba)
curl -X POST http://localhost:8080/credit-applications \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
        "applicantFullName": "Ada Lovelace",
        "documentId": "DOC-001",
        "birthDate": "1990-05-10",
        "monthlyIncome": 3500.00,
        "requestedAmount": 12000.00,
        "loanTermMonths": 24,
        "employmentYears": 4.5,
        "existingMonthlyDebt": 200.00,
        "numberOfDependents": 1
      }'
```

## Endpoints (Fase 1)

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/login` | público | Devuelve un JWT |
| POST | `/auth/users` | `ADMIN` | Crea un usuario analista/admin |
| POST | `/credit-applications` | JWT | Crea una solicitud de crédito |
| GET | `/credit-applications` | JWT | Lista solicitudes (paginado, excluye eliminadas) |
| GET | `/credit-applications/{id}` | JWT | Detalle de una solicitud |
| PUT | `/credit-applications/{id}` | JWT | Actualiza una solicitud |
| DELETE | `/credit-applications/{id}` | JWT | Elimina una solicitud (soft-delete) |
| GET | `/actuator/health` | público | Estado del servicio |

## Tests

```bash
cd backend
./mvnw test      # unitarios (JUnit 5 + Mockito)
./mvnw verify     # unitarios + integración (Testcontainers levanta un Postgres efímero; requiere Docker)
```

## Roadmap

1. ✅ **Fase 1 — Esqueleto backend:** CRUD de solicitudes, JWT, Postgres.
2. **Fase 2 — Servicio de ML base:** microservicio FastAPI entrenado sobre un dataset público (ej. "Give Me Some Credit"), expone `/score`.
3. **Fase 3 — Explicabilidad:** integrar SHAP, exponer `/explain`, persistir la explicación junto a cada decisión.
4. **Fase 4 — Model registry y versionado:** metadata de cada modelo entrenado, activación de versiones, trazabilidad histórica.
5. **Fase 5 — What-if analysis:** simulación de cambios en variables del solicitante.
6. **Fase 6 — Pulido:** documentación ampliada, dashboard de auditoría.

## Licencia

Distribuido bajo la licencia **GPL-3.0**. Ver [LICENSE](LICENSE).
