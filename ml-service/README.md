# ML Service — Credit Scoring

Microservicio Python (FastAPI) que entrena y expone el modelo de riesgo crediticio. Vive separado del backend Java: es responsable únicamente de scoring, no de persistencia ni orquestación (ver `CLAUDE.md` en la raíz del repo).

## Modelo

- **Dataset:** [Give Me Some Credit](https://www.kaggle.com/c/GiveMeSomeCredit/data) (Kaggle) — ~150k solicitudes históricas con 10 variables numéricas y la etiqueta `SeriousDlqin2yrs` (impago serio en los siguientes 2 años).
- **Algoritmo:** Regresión logística (`scikit-learn`), con imputación de medianas + escalado dentro de un `Pipeline`, priorizando interpretabilidad sobre performance marginal (ver `CLAUDE.md`).
- **Salida:** probabilidad de default + score de riesgo en escala 300–850 (estilo FICO: a mayor score, menor riesgo).
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

El mapeo entre los campos de `CreditApplicationRequest` (Java) y las variables nativas de este dataset (historial de mora, líneas de crédito abiertas, etc.) es responsabilidad de la orquestación del backend y se resuelve en una fase posterior. Este servicio expone el modelo con el esquema de entrenamiento original para mantener trazabilidad total entre lo que el modelo aprendió y lo que expone — nada de "traducciones" implícitas que compliquen la auditoría.
