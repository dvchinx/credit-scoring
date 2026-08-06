"""Definición de features y preprocesamiento compartidos entre entrenamiento e inferencia."""
from __future__ import annotations

import pandas as pd

TARGET_COLUMN = "SeriousDlqin2yrs"

# (columna original del CSV de Kaggle, nombre canónico de la feature usado por el modelo y la API)
FEATURE_COLUMNS = [
    ("RevolvingUtilizationOfUnsecuredLines", "revolving_utilization_of_unsecured_lines"),
    ("age", "age"),
    ("NumberOfTime30-59DaysPastDueNotWorse", "number_of_time_30_59_days_past_due_not_worse"),
    ("DebtRatio", "debt_ratio"),
    ("MonthlyIncome", "monthly_income"),
    ("NumberOfOpenCreditLinesAndLoans", "number_of_open_credit_lines_and_loans"),
    ("NumberOfTimes90DaysLate", "number_of_times_90_days_late"),
    ("NumberRealEstateLoansOrLines", "number_real_estate_loans_or_lines"),
    ("NumberOfTime60-89DaysPastDueNotWorse", "number_of_time_60_89_days_past_due_not_worse"),
    ("NumberOfDependents", "number_of_dependents"),
]

FEATURE_NAMES = [feature_name for _, feature_name in FEATURE_COLUMNS]
RAW_COLUMN_NAMES = [raw_name for raw_name, _ in FEATURE_COLUMNS]


def raw_dataframe_to_features(df: pd.DataFrame) -> pd.DataFrame:
    """Selecciona y renombra las columnas crudas del CSV de Kaggle a los nombres canónicos del modelo, en orden."""
    renamed = df.rename(columns=dict(FEATURE_COLUMNS))
    return renamed[FEATURE_NAMES]


def build_pipeline():
    from sklearn.impute import SimpleImputer
    from sklearn.linear_model import LogisticRegression
    from sklearn.pipeline import Pipeline
    from sklearn.preprocessing import StandardScaler

    # class_weight="balanced": el target real está muy desbalanceado (~6-7% de impagos),
    # sin esto el modelo tiende a predecir casi siempre "no default".
    return Pipeline(steps=[
        ("imputer", SimpleImputer(strategy="median")),
        ("scaler", StandardScaler()),
        ("classifier", LogisticRegression(max_iter=1000, class_weight="balanced")),
    ])
