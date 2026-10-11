# Credit Scoring Engine

Motor de scoring crediticio explicable para el sector financiero. Evalúa el riesgo de un solicitante de crédito, devuelve una decisión (aprobar / rechazar / revisión manual) y explica el porqué de esa decisión de forma trazable y auditable, tal como exige la regulación financiera ("right to explanation").

Proyecto de portafolio orientado a roles **Backend + IA**: el foco no es solo que el modelo prediga bien, sino demostrar un backend con **auditoría, versionado y governance** de decisiones automatizadas, con explicabilidad como ciudadano de primera clase.

## Estado actual

- **Fase 1 — Esqueleto backend** completada: autenticación JWT, CRUD de solicitudes de crédito y persistencia en Postgres.
- **Fase 2 — Servicio de ML base** completada: microservicio Python/FastAPI (`ml-service/`) entrenado sobre el dataset público "Give Me Some Credit", expone `/score`. Ver [ml-service/README.md](ml-service/README.md).
- **Fase 3 — Explicabilidad** completada: el servicio de ML expone `/explain` (valores SHAP exactos) y el backend evalúa solicitudes y persiste cada decisión con su score, explicación, versión de modelo, política aplicada y snapshot de los datos de entrada.
- **Fase 4 — Model registry y versionado** completada: cada entrenamiento genera una versión inmutable con metadata completa (dataset, métricas, hiperparámetros, hash del artefacto); el backend registra versiones, decide cuál está activa y guarda el historial de activaciones con su motivo.

El what-if analysis y el frontend llegan en fases posteriores — ver [Roadmap](#roadmap).

## Arquitectura

```
[Frontend (React SPA)] / [Clientes API]                                  (fase 6)
        │
        ▼
[Spring Boot API] ──► [PostgreSQL] (solicitudes, decisiones, auditoría, versiones de modelo)
        │
        ├──► [ML Service (FastAPI)] ──► [Versiones de modelo en disco + SHAP]
        │           │      /score?model_version= ✅ (fase 2)   /explain ✅ (fase 3)   /models ✅ (fase 4)
        │           └──► /what-if (simulación de variables)      (fase 5)
        │
        └──► [Model Registry] ✅ (fase 4): versiones registradas + log de activaciones (en PostgreSQL)
```

El backend Java es responsable de la orquestación, persistencia y governance; el servicio Python (`ml-service/`, FastAPI) vive en un microservicio separado, responsable exclusivamente de scoring y explicabilidad.

### Model registry y versionado

El backend es la **única fuente de verdad** de qué versión de modelo decide; el servicio de ML no tiene noción de "versión activa" y sirve la versión exacta que se le pide.

```
entrenar (ml-service)  ──►  models/<version>/  model.joblib + metadata.json   (inmutable; nunca se sobreescribe)
        │
POST /model-versions   ──►  el backend lee la metadata desde GET /models/{version} y la guarda en model_versions
        │
POST /model-activations ─►  {version, reason}: se inserta en model_activations; la última activación es la vigente
        │
POST .../decisions     ──►  /score?model_version=<activa>  y  /explain?model_version=<activa>
```

- **Metadata por versión:** algoritmo e hiperparámetros, fecha de entrenamiento, huella SHA-256 del dataset, filas de entrenamiento/test y tasa de default, métricas (ROC-AUC, Brier) y variables de entrada. La metadata se toma del servicio de ML, nunca del cliente que registra.
- **Integridad del artefacto:** al entrenar se calcula el SHA-256 de `model.joblib`. El servicio de ML se niega a servir una versión cuyo archivo ya no coincide (`409`). El backend no registra artefactos alterados y, al activar, exige que el hash que sirve el ML sea **el mismo que se registró**.
- **Compatibilidad:** solo se registran versiones cuyas variables de entrada coinciden con las que el backend sabe calcular.
- **Activación auditada:** cada activación guarda quién, cuándo y por qué (motivo obligatorio). Un rollback es activar de nuevo una versión anterior; el historial completo responde "qué modelo decidía en tal fecha".
- **Append-only:** `model_versions` y `model_activations` rechazan `UPDATE`/`DELETE` por trigger, y `credit_decisions.model_version` tiene FK a `model_versions`.
- Registrar y activar requieren rol `ADMIN`; consultar, cualquier usuario autenticado.

### Flujo de evaluación (`POST /credit-applications/{id}/decisions`)

1. Se carga la solicitud y se toma un **snapshot** de sus datos.
2. Se fija la **versión de modelo activa** en ese momento. Si no hay ninguna, la API responde `409` y no se registra nada.
3. Se traduce la solicitud a las variables del modelo (ver tabla abajo).
4. Se llama a `/score` y a `/explain` del servicio de ML **con esa versión explícita**, fuera de cualquier transacción de base de datos.
5. Si el ML falla, no responde, o responde con una versión distinta de la pedida, se registra una decisión `FAILED` con el motivo, la solicitud sigue `PENDING` y la API responde `503`.
6. Si todo va bien, la política de crédito (umbrales externalizados) produce `APPROVED` / `REJECTED` / `MANUAL_REVIEW` con sus motivos.
7. En una sola transacción se inserta la decisión y se actualiza el estado de la solicitud.

Cada evaluación crea una decisión nueva; las anteriores se conservan. La tabla `credit_decisions` es **append-only**: un trigger de Postgres rechaza cualquier `UPDATE` o `DELETE`, y un `CHECK` impide guardar una decisión `COMPLETED` sin score, explicación y versión de modelo.

### De la solicitud a las variables del modelo

El modelo se entrenó con variables tipo buró de crédito. La solicitud incluye un bloque `creditHistory` con ellas, y el resto se deriva:

| Variable del modelo | Origen |
|---|---|
| `age` | Años cumplidos a la fecha de evaluación, desde `birthDate` |
| `debt_ratio` | (`existingMonthlyDebt` + `requestedAmount` / `loanTermMonths`) / `monthlyIncome`: endeudamiento **incluyendo la cuota nueva** (estimada sin intereses) |
| `monthly_income`, `number_of_dependents` | Directo de la solicitud |
| `revolving_utilization_of_unsecured_lines` | `creditHistory.revolvingUtilization` |
| `number_of_open_credit_lines_and_loans` | `creditHistory.openCreditLines` |
| `number_real_estate_loans_or_lines` | `creditHistory.realEstateLoans` |
| `number_of_time_30_59_days_past_due_not_worse` | `creditHistory.latePayments30To59Days` |
| `number_of_time_60_89_days_past_due_not_worse` | `creditHistory.latePayments60To89Days` |
| `number_of_times_90_days_late` | `creditHistory.latePayments90DaysOrMore` |

`employmentYears` se guarda en el snapshot pero el modelo actual no lo usa.

### Política de crédito

Configurable por variables de entorno (ver `.env.example`); se guarda con cada decisión para saber qué umbrales se aplicaron:

1. **Reglas duras**, con rechazo sin importar el score: edad mínima (`CREDIT_POLICY_MIN_APPLICANT_AGE`, 18) y endeudamiento máximo con la cuota nueva (`CREDIT_POLICY_MAX_DEBT_RATIO`, 50%).
2. Probabilidad de default ≥ `CREDIT_POLICY_REJECT_MIN_PD` (0.60): `REJECTED`.
3. Probabilidad de default ≤ `CREDIT_POLICY_APPROVE_MAX_PD` (0.30): `APPROVED`.
4. Entre ambos umbrales: `MANUAL_REVIEW`.

Una política inválida (p. ej. umbral de aprobación ≥ umbral de rechazo) impide arrancar la aplicación.

> El modelo se entrena con `class_weight="balanced"`, así que sus probabilidades no están calibradas a la tasa real de default (~7%). Los umbrales por defecto son ilustrativos y deben revisarse con cada versión del modelo.

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
├── scoring/                 Evaluación y decisiones: política de crédito, explicación SHAP, cliente HTTP del servicio de ML,
│                            persistencia append-only de decisiones
├── modelregistry/           Versiones de modelo registradas, activaciones auditadas, catálogo del servicio de ML
└── common/                  Excepciones de dominio, manejo de errores centralizado, entidad base de auditoría JPA,
                             TransactionRunner (port para delimitar transacciones sin acoplar los casos de uso a Spring)
```

**Regla de dependencia:** `domain` no conoce Spring ni JPA; `application` solo depende de `domain` (los ports); `infrastructure` implementa los ports de salida y consume los casos de uso, nunca al revés.

## Stack técnico

- **Backend:** Java 21 + Spring Boot 4 (Spring Web, Spring Data JPA, Spring Security con JWT, Bean Validation)
- **Base de datos:** PostgreSQL, migraciones versionadas con Flyway (nunca `ddl-auto`)
- **Auditoría de solicitudes:** timestamps automáticos (`createdAt`/`updatedAt`) y soft-delete (`deletedAt`), acorde al enfoque de trazabilidad del proyecto
- **Seguridad:** JWT stateless (JJWT), contraseñas con BCrypt, roles `ADMIN` / `ANALYST`
- **Observabilidad:** Spring Actuator + logs estructurados en JSON (Logstash encoder)
- **Testing:** JUnit 5 + Mockito (unitarios) y Testcontainers + Postgres real (integración)
- **Servicio de ML:** Python + FastAPI (`ml-service/`), scikit-learn (Regresión Logística), SHAP, pytest — ver [ml-service/README.md](ml-service/README.md)
- **Integración backend → ML:** `RestClient` de Spring con timeouts configurables; los fallos se registran como decisiones `FAILED`
- **Infraestructura:** Docker Compose para Postgres y el servicio de ML

## Cómo levantar el entorno

### 1. Variables de entorno

```bash
cp .env.example .env
# editar .env: credenciales de Postgres, JWT_SECRET (mínimo 32 caracteres) y el admin inicial
```

### 2. Base de datos y servicio de ML

```bash
docker-compose up -d
# entrenar un modelo (requiere data/raw/cs-training.csv, ver ml-service/README.md); imprime la versión creada
docker-compose exec ml-service python -m training.train
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
        "numberOfDependents": 1,
        "creditHistory": {
          "revolvingUtilization": 0.3,
          "openCreditLines": 5,
          "realEstateLoans": 1,
          "latePayments30To59Days": 0,
          "latePayments60To89Days": 0,
          "latePayments90DaysOrMore": 0
        }
      }'

# Registrar y activar la versión de modelo entrenada (como ADMIN)
curl -X POST http://localhost:8080/model-versions \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"version": "<versión impresa por training.train>"}'
curl -X POST http://localhost:8080/model-activations \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"version": "<versión>", "reason": "Primera versión en producción"}'

# Evaluar la solicitud (usar el id devuelto al crearla)
curl -X POST http://localhost:8080/credit-applications/<id>/decisions \
  -H "Authorization: Bearer <token>"
```

Respuesta (resumida, valores ilustrativos):

```json
{
  "status": "COMPLETED",
  "outcome": "APPROVED",
  "probabilityOfDefault": 0.18,
  "riskScore": 751,
  "modelVersion": "20261004T120000Z",
  "reasons": ["Probabilidad de default de 18.0% dentro del umbral de aprobación de 30.0%"],
  "explanation": {
    "baseValue": -0.41,
    "outputValue": -1.52,
    "contributions": [
      {"feature": "age", "value": 36, "shapValue": -0.62, "direction": "DECREASES_RISK"},
      {"feature": "revolving_utilization_of_unsecured_lines", "value": 0.3, "shapValue": -0.31, "direction": "DECREASES_RISK"}
    ]
  },
  "modelInput": {"revolving_utilization_of_unsecured_lines": 0.3, "age": 36, "debt_ratio": 0.2, "...": "..."},
  "policy": {"approveMaxProbability": 0.3, "rejectMinProbability": 0.6, "maxDebtRatio": 0.5, "minApplicantAge": 18}
}
```

Los valores SHAP están en log-odds de default: `baseValue + Σ shapValue = outputValue`. `modelInput` es exactamente el payload que recibió el modelo, así que se puede reenviar a `/score` para reproducir el resultado.

## Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/login` | público | Devuelve un JWT |
| POST | `/auth/users` | `ADMIN` | Crea un usuario analista/admin |
| POST | `/credit-applications` | JWT | Crea una solicitud de crédito |
| GET | `/credit-applications` | JWT | Lista solicitudes (paginado, excluye eliminadas) |
| GET | `/credit-applications/{id}` | JWT | Detalle de una solicitud |
| PUT | `/credit-applications/{id}` | JWT | Actualiza una solicitud |
| DELETE | `/credit-applications/{id}` | JWT | Elimina una solicitud (soft-delete) |
| POST | `/credit-applications/{id}/decisions` | JWT | Evalúa la solicitud con el modelo activo y crea una decisión explicada (`409` si no hay modelo activo, `503` si el ML no está disponible) |
| GET | `/credit-applications/{id}/decisions` | JWT | Historial de decisiones de la solicitud, de la más reciente a la más antigua |
| POST | `/model-versions` | `ADMIN` | Registra una versión entrenada (`{"version"}`); la metadata se lee del servicio de ML |
| GET | `/model-versions` | JWT | Versiones registradas con su metadata y cuál está activa |
| GET | `/model-versions/{version}` | JWT | Detalle de una versión |
| POST | `/model-activations` | `ADMIN` | Activa una versión registrada (`{"version", "reason"}`); también sirve para rollback |
| GET | `/model-activations` | JWT | Historial de activaciones, de la más reciente a la más antigua |
| GET | `/model-activations/current` | JWT | Activación vigente (`404` si no hay ninguna) |
| GET | `/actuator/health` | público | Estado del servicio |

## Tests

```bash
cd backend
./mvnw test      # unitarios (JUnit 5 + Mockito)
./mvnw verify     # unitarios + integración (Testcontainers levanta un Postgres efímero; requiere Docker)
```

## Roadmap

1. ✅ **Fase 1 — Esqueleto backend:** CRUD de solicitudes, JWT, Postgres.
2. ✅ **Fase 2 — Servicio de ML base:** microservicio FastAPI entrenado sobre un dataset público ("Give Me Some Credit"), expone `/score`.
3. ✅ **Fase 3 — Explicabilidad:** `/explain` con valores SHAP, decisiones explicadas y append-only en el backend.
4. ✅ **Fase 4 — Model registry y versionado:** versiones inmutables con metadata completa y hash verificado, activación auditada con rollback, decisiones ligadas a su versión.
5. **Fase 5 — What-if analysis:** simulación de cambios en variables del solicitante.
6. **Fase 6 — Frontend:** SPA en React + TypeScript (Vite) sobre el API de Spring Boot: login, gestión de solicitudes, historial de decisiones con gráfico de contribuciones SHAP, simulador what-if, versiones de modelo y dashboard de auditoría. Solo presentación: las decisiones las toma siempre el backend.
7. **Fase 7 — Pulido:** documentación ampliada, README con diagramas.

## Licencia

Distribuido bajo la licencia **GPL-3.0**. Ver [LICENSE](LICENSE).
