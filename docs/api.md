# API REST

Referencia de los endpoints del backend. Todos salvo el login y el health requieren `Authorization: Bearer <token>`.

## Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/login` | público | Devuelve un JWT, junto con `username` y `role` |
| POST | `/auth/users` | `ADMIN` | Crea un usuario analista/admin |
| POST | `/credit-applications` | JWT | Crea una solicitud de crédito |
| GET | `/credit-applications` | JWT | Lista solicitudes (paginado, excluye eliminadas) |
| GET | `/credit-applications/{id}` | JWT | Detalle de una solicitud |
| PUT | `/credit-applications/{id}` | JWT | Actualiza una solicitud |
| DELETE | `/credit-applications/{id}` | JWT | Elimina una solicitud (soft-delete) |
| POST | `/credit-applications/{id}/decisions` | JWT | Evalúa la solicitud con el modelo activo y crea una decisión explicada (`409` si no hay modelo activo, `503` si el ML no está disponible) |
| GET | `/credit-applications/{id}/decisions` | JWT | Historial de decisiones de la solicitud, de la más reciente a la más antigua |
| POST | `/credit-applications/{id}/what-if` | JWT | Simula cambios en la solicitud y compara la decisión simulada con la real; no crea decisiones (`409` si no hay modelo activo) |
| GET | `/credit-applications/{id}/what-if` | JWT | Simulaciones hechas sobre la solicitud, de la más reciente a la más antigua |
| POST | `/model-versions` | `ADMIN` | Registra una versión entrenada (`{"version"}`); la metadata se lee del servicio de ML |
| GET | `/model-versions` | JWT | Versiones registradas con su metadata y cuál está activa |
| GET | `/model-versions/{version}` | JWT | Detalle de una versión |
| POST | `/model-activations` | `ADMIN` | Activa una versión registrada (`{"version", "reason"}`); también sirve para rollback |
| GET | `/model-activations` | JWT | Historial de activaciones, de la más reciente a la más antigua |
| GET | `/model-activations/current` | JWT | Activación vigente (`404` si no hay ninguna) |
| GET | `/audit/summary` | JWT | Agregados para auditoría: decisiones por resultado y por versión de modelo, intentos fallidos recientes, simulaciones |
| GET | `/actuator/health` | público | Estado del servicio |

Los listados paginados responden `{"content": [...], "page": {"size", "number", "totalElements", "totalPages"}}`.

Errores: el cuerpo sigue siempre el formato `{"timestamp", "status", "error", "message", "path", "details"}`. En un `400` de validación, `details` trae `"campo: mensaje"` por cada campo inválido (los anidados como `creditHistory.revolvingUtilization`).

| Código | Cuándo |
|---|---|
| `400` | Datos inválidos (validación por campo) |
| `401` | Sin token, token expirado o credenciales inválidas |
| `403` | El rol no alcanza (p. ej. un `ANALYST` registrando modelos) |
| `404` | Recurso inexistente |
| `409` | Conflicto de estado: duplicados, sin versión de modelo activa, artefacto de modelo alterado |
| `503` | El servicio de ML no está disponible (la evaluación queda registrada como `FAILED`) |

## Recorrido con curl

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
