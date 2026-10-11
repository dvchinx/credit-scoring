"""Entrena el modelo de riesgo crediticio sobre el dataset "Give Me Some Credit".

Uso:
    python -m training.train --input data/raw/cs-training.csv --output-dir models

Cada ejecución crea una versión nueva e inmutable. Entrenar no activa la versión: el registro
y la activación se hacen desde el backend (model registry), que es quien gobierna qué modelo
decide.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import pandas as pd
from sklearn.metrics import brier_score_loss, roc_auc_score
from sklearn.model_selection import train_test_split

from app.registry import MODEL_FILENAME, METADATA_FILENAME, sha256_of_file
from training.preprocessing import TARGET_COLUMN, build_pipeline, raw_dataframe_to_features


def split_train_test(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame, pd.Series, pd.Series]:
    """Split determinista (X_train, X_test, y_train, y_test). El set de entrenamiento es también
    el background implícito de las explicaciones SHAP (ver app.model.explain)."""
    X = raw_dataframe_to_features(df)
    y = df[TARGET_COLUMN]
    return train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)


def new_version_id() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")


def dataset_fingerprint(df: pd.DataFrame) -> str:
    """SHA-256 del contenido del dataset (filas, columnas e índice), independiente del formato de archivo."""
    row_hashes = pd.util.hash_pandas_object(df, index=True).values
    columns = ",".join(map(str, df.columns)).encode()
    return hashlib.sha256(columns + row_hashes.tobytes()).hexdigest()


def describe_pipeline(pipeline) -> tuple[str, dict]:
    algorithm = " -> ".join(type(step).__name__ for _, step in pipeline.steps)
    classifier_params = pipeline.steps[-1][1].get_params()
    hyperparameters = {
        name: value for name, value in classifier_params.items()
        if value is None or isinstance(value, (str, int, float, bool))
    }
    return algorithm, hyperparameters


def train_and_save(df: pd.DataFrame, output_dir: Path, dataset_source: str = "in-memory") -> dict:
    """Entrena el pipeline, lo evalúa y persiste una nueva versión inmutable en output_dir/<version>/.

    Falla si la carpeta de la versión ya existe: una versión entrenada nunca se sobreescribe.
    """
    X_train, X_test, y_train, y_test = split_train_test(df)

    pipeline = build_pipeline()
    pipeline.fit(X_train, y_train)

    test_probabilities = pipeline.predict_proba(X_test)[:, 1]

    version = new_version_id()
    version_dir = Path(output_dir) / version
    version_dir.mkdir(parents=True, exist_ok=False)

    joblib.dump(pipeline, version_dir / MODEL_FILENAME)
    algorithm, hyperparameters = describe_pipeline(pipeline)

    metadata = {
        "version": version,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "algorithm": algorithm,
        "hyperparameters": hyperparameters,
        "dataset": {
            "source": dataset_source,
            "sha256": dataset_fingerprint(df),
            "training_rows": len(X_train),
            "test_rows": len(X_test),
            "default_rate": float(y_train.mean()),
        },
        "metrics": {
            "roc_auc": float(roc_auc_score(y_test, test_probabilities)),
            "brier_score": float(brier_score_loss(y_test, test_probabilities)),
        },
        "feature_names": list(X_train.columns),
        "artifact": {"file": MODEL_FILENAME, "sha256": sha256_of_file(version_dir / MODEL_FILENAME)},
    }
    (version_dir / METADATA_FILENAME).write_text(json.dumps(metadata, indent=2))

    return metadata


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=Path("data/raw/cs-training.csv"))
    parser.add_argument("--output-dir", type=Path, default=Path("models"))
    args = parser.parse_args()

    df = pd.read_csv(args.input, index_col=0)
    metadata = train_and_save(df, args.output_dir, dataset_source=args.input.name)

    print(f"Modelo entrenado: version={metadata['version']} roc_auc={metadata['metrics']['roc_auc']:.4f}")
    print(f"Guardado en: {args.output_dir / metadata['version']}")
    print("Para usarlo, regístralo y actívalo desde el backend (POST /model-versions y POST /model-activations).")


if __name__ == "__main__":
    main()
