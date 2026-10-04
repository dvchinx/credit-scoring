# ML Service — Credit Scoring

Microservicio Python (FastAPI) que entrena y expone el modelo de riesgo crediticio. Vive separado del backend Java: es responsable únicamente de scoring y explicabilidad, no de persistencia ni orquestación (ver `CLAUDE.md` en la raíz del repo).

## Modelo

- **Dataset:** [Give Me Some Credit](https://www.kaggle.com/c/GiveMeSomeCredit/data) (Kaggle) — ~150k solicitudes históricas con 10 variables numéricas y la etiqueta `SeriousDlqin2yrs` (impago serio en los siguientes 2 años).
- **Algoritmo:** Regresión logística (`scikit-learn`), con imputación de medianas + escalado dentro de un `Pipeline`, priorizando interpretabilidad sobre performance marginal (ver `CLAUDE.md`).
- **Salida:** probabilidad de default + score de riesgo en escala 300–850 (estilo FICO: a mayor score, menor riesgo).
- **Explicabilidad:** valores SHAP por variable (ver [Explicabilidad](#explicabilidad)).
- **Versionado:** cada entrenamiento genera una carpeta inmutable `models/<timestamp>/` (`model.joblib` + `metadata.json`) y actualiza el puntero `models/ACTIVE_VERSION`. Es una versión mínima de model registry — la Fase 4 del roadmap lo formalizará en base de datos.

## Setup local

```bash
python -m venv .venv
.venv\Scripts\activate          # Linux/Mac: source .venv/bin/activate
pip install -r requirements-dev.txt
```

## Entrenar el modelo

1. Descargar `cs-training.csv` desde Kaggle y ubicarlo en `data/raw/cs-training.csv`.
2. Ejecutar:

```bash
python -m training.train
```

Esto entrena el pipeline, evalúa AUC-ROC sobre un holdout del 20% y persiste la nueva versión activa en `models/`.

## Levantar la API

```bash
uvicorn app.main:app --reload --port 8000
```

Si `models/ACTIVE_VERSION` no existe todavía (no se entrenó ningún modelo), el servicio arranca igual pero `/health` reporta `degraded` y `/score` responde `503`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/health` | Estado del servicio y versión del modelo activo |
| POST | `/score` | Probabilidad de default + score de riesgo para las 10 variables del modelo |
| POST | `/explain` | Valores SHAP por variable para la misma entrada que `/score` |

### Ejemplo

```bash
curl -X POST http://localhost:8000/score \
  -H "Content-Type: application/json" \
  -d '{
        "revolving_utilization_of_unsecured_lines": 0.3,
        "age": 45,
        "number_of_time_30_59_days_past_due_not_worse": 0,
        "debt_ratio": 0.2,
        "monthly_income": 5000,
        "number_of_open_credit_lines_and_loans": 5,
        "number_of_times_90_days_late": 0,
        "number_real_estate_loans_or_lines": 1,
        "number_of_time_60_89_days_past_due_not_worse": 0,
        "number_of_dependents": 1
      }'
```

Para obtener la explicación, enviar el mismo payload a `/explain`:

```json
{
  "model_version": "20261004T120000Z",
  "base_value": -0.41,
  "output_value": -1.52,
  "contributions": [
    {"feature": "age", "value": 45, "shap_value": -0.62, "direction": "decreases_risk"},
    {"feature": "revolving_utilization_of_unsecured_lines", "value": 0.3, "shap_value": -0.31, "direction": "decreases_risk"}
  ]
}
```

## Explicabilidad

`/explain` devuelve el valor SHAP de cada variable en espacio **log-odds de default**: positivo aumenta el riesgo, negativo lo reduce. Se cumple `base_value + Σ shap_value = output_value`, y `sigmoid(output_value)` es la `probability_of_default` de `/score`.

Para una regresión logística sobre variables estandarizadas, el SHAP exacto (con features independientes y el set de entrenamiento como background) tiene forma cerrada: `φᵢ = βᵢ · zᵢ`, porque la media de cada variable estandarizada en entrenamiento es 0. El servicio lo calcula así en `app/model.py`, sin cargar la librería `shap` (ni numba) en la imagen de producción. `tests/test_explainability.py` verifica que el resultado coincide con `shap.LinearExplainer` (tolerancia 1e-9); `shap` solo es dependencia de desarrollo.

Si en el futuro se cambia a un modelo no lineal (p. ej. XGBoost), habrá que pasar a `shap.TreeExplainer` y llevar `shap` a las dependencias de runtime.

## Tests

```bash
pytest
```

Los tests entrenan un modelo sintético en un directorio temporal (`tmp_path`) — no dependen de tener el dataset real de Kaggle descargado.

## Docker

Desde la raíz del repo:

```bash
docker-compose up -d ml-service
```

El contenedor monta `./ml-service/models` y `./ml-service/data`, así que el entrenamiento (`docker-compose exec ml-service python -m training.train`) persiste los artefactos también en el host.

## Nota sobre integración con el backend

El mapeo entre la solicitud de crédito (Java) y las variables nativas de este dataset es responsabilidad del backend (`ModelFeatures.from(...)`, ver la tabla en el README raíz). Este servicio expone el modelo con el esquema de entrenamiento original para mantener trazabilidad total entre lo que el modelo aprendió y lo que expone — nada de "traducciones" implícitas que compliquen la auditoría. El backend guarda con cada decisión el payload exacto que envió, con estos mismos nombres de variable.
