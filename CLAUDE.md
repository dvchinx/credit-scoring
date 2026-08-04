# CLAUDE.md

Este archivo guía a Claude Code al trabajar en este repositorio.

## Visión del proyecto

**Motor de scoring crediticio explicable** para el sector financiero. El sistema evalúa el riesgo crediticio de un solicitante (préstamo, tarjeta, etc.), devuelve una decisión (aprobar / rechazar / revisión manual) y, sobre todo, **explica el porqué** de esa decisión de forma trazable y auditable — algo que la regulación financiera exige en la vida real (ej. "right to explanation").

Este es un proyecto de portafolio orientado a roles **Backend + IA**. El objetivo no es solo "que el modelo prediga bien", sino demostrar:
- Diseño de un backend con foco en **auditoría, versionado y governance** de decisiones automatizadas.
- Integración seria de ML en un sistema productivo, con explicabilidad como ciudadano de primera clase (no un afterthought).
- Simulación de "qué pasaría si" (what-if analysis) sobre variables del solicitante.
- Trazabilidad completa: cada decisión debe poder reconstruirse meses después (qué modelo, qué versión, qué datos, qué explicación).

## Stack técnico

- **Backend:** Java 21 + Spring Boot 3.x
  - Spring Web (API REST)
  - Spring Data JPA
  - Spring Security (JWT) para autenticación de la API
  - Spring Validation
- **Base de datos:** PostgreSQL (solicitudes, decisiones, versiones de modelo, auditoría)
- **Servicio de ML:** Python (FastAPI) como microservicio separado que expone:
  - `/score` → score de riesgo + probabilidad de default
  - `/explain` → explicación (SHAP values) por variable
  - `/what-if` → recalcula el score simulando cambios en variables de entrada
  - Modelo: XGBoost o Logistic Regression (priorizar interpretabilidad sobre performance marginal)
- **Versionado de modelos:** cada modelo entrenado se registra con metadata (fecha, dataset, métricas, hash) — puede ser tan simple como una tabla `model_versions` + artefactos en disco/S3 local (MinIO).
- **Contenedores:** Docker + docker-compose
- **Testing:** JUnit 5 + Mockito, Testcontainers para tests de integración con Postgres real
- **Observabilidad:** Spring Actuator + Micrometer, logs estructurados (JSON)

> Nota: al igual que en el caso de fraude, el modelo de ML vive en un microservicio Python separado del backend Java. El backend Java es responsable de la orquestación, persistencia y governance; el servicio Python es responsable exclusivamente de scoring y explicabilidad.

## Arquitectura (alto nivel)

```
[Cliente/API Gateway]
        │
        ▼
[Spring Boot API] ──► [PostgreSQL] (solicitudes, decisiones, auditoría, versiones de modelo)
        │
        ├──► [ML Service (FastAPI)] ──► [Modelo activo + SHAP]
        │           │
        │           └──► /what-if (simulación de variables)
        │
        └──► [Model Registry] (metadata de versiones, para trazabilidad)
```

Flujo de una solicitud de crédito:
1. Llega vía API REST (`POST /credit-applications`) con datos del solicitante.
2. Se validan y normalizan los datos de entrada.
3. Se consulta al servicio de ML (`/score`) usando la **versión de modelo activa** en ese momento.
4. Se obtiene la explicación (`/explain`) — variables que más influyeron, en qué dirección y magnitud.
5. Se combina score + reglas de negocio (ej. umbrales regulatorios, listas de exclusión) → decisión final.
6. Se persiste la decisión junto con: score, explicación completa, versión de modelo usada, y snapshot de los datos de entrada (para poder reconstruir la decisión más adelante).
7. Endpoint adicional (`GET /credit-applications/{id}/what-if`) permite simular cómo cambiaría la decisión si una variable fuera distinta (ej. "¿y si el ingreso fuera 20% mayor?").

## Convenciones de código

- Paquetes por **feature/dominio** (`creditapplication/`, `scoring/`, `explainability/`, `audit/`), no por capa técnica genérica a nivel raíz.
- El backend Java sigue **arquitectura hexagonal (ports & adapters)**: cada feature se organiza internamente en `domain` (modelo + reglas de negocio, sin dependencias de Spring/JPA) → `application` (casos de uso, orquestan el dominio a través de ports) → `infrastructure` (adaptadores concretos: controllers REST, repositorios JPA, seguridad). El dominio nunca depende de `application` ni `infrastructure`.
- DTOs (en `infrastructure/in/web`) separados de entidades JPA (en `infrastructure/out/persistence`) y del modelo de dominio — nunca exponer entidades directamente en la API.
- Usar `record` de Java para DTOs inmutables.
- Manejo de errores centralizado con `@ControllerAdvice`.
- **Ninguna decisión se persiste sin su explicación asociada** — si el servicio de ML no puede explicar, la decisión no se guarda como final (se marca como pendiente/error).
- Versión de modelo usada en cada decisión debe quedar registrada explícitamente (nunca inferida después del hecho).
- Reglas regulatorias/umbrales externalizados (config o tabla en BD), no hardcodeados.

## Fases del proyecto (roadmap sugerido)

1. **Fase 1 — Esqueleto backend:** CRUD de solicitudes de crédito, autenticación JWT, persistencia en Postgres.
2. **Fase 2 — Servicio de ML base:** entrenar modelo con dataset público (ej. Kaggle "Give Me Some Credit" o "Home Credit Default Risk"), exponerlo vía FastAPI con `/score`.
3. **Fase 3 — Explicabilidad:** integrar SHAP, exponer `/explain`, persistir explicación junto a cada decisión.
4. **Fase 4 — Model registry y versionado:** registrar metadata de cada modelo entrenado, permitir "activar" una versión específica, asociar cada decisión histórica a su versión.
5. **Fase 5 — What-if analysis:** endpoint de simulación, útil tanto para el solicitante ("qué mejorar") como para auditoría interna.
6. **Fase 6 — Pulido:** tests de integración con Testcontainers, documentación, dashboard simple de auditoría (decisiones + explicaciones), README con diagramas.

**Empezar por la Fase 1.** No avanzar al servicio de ML sin tener el backend base sólido y testeado.

## Qué evitar

- No exponer el modelo como caja negra: toda decisión debe tener su explicación persistida y consultable.
- No mezclar el microservicio de ML con el backend Java en el mismo proceso.
- No sobreescribir versiones de modelo — cada entrenamiento genera una versión nueva e inmutable.
- No hardcodear credenciales ni umbrales regulatorios — usar variables de entorno / configuración externalizada.
- No optimizar performance del modelo por encima de la interpretabilidad sin justificarlo explícitamente.

## Comandos útiles

```bash
docker-compose up -d          # levanta Postgres y servicios auxiliares
./mvnw spring-boot:run         # corre el backend
./mvnw test                    # corre tests
```

(Actualizar esta sección a medida que se agreguen scripts reales del proyecto.)