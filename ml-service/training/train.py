"""Entrena el modelo de riesgo crediticio sobre el dataset "Give Me Some Credit".

Uso:
    python -m training.train --input data/raw/cs-training.csv --output-dir models
"""
from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import pandas as pd
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import train_test_split

from training.preprocessing import TARGET_COLUMN, build_pipeline, raw_dataframe_to_features


def split_train_test(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame, pd.Series, pd.Series]:
    """Split determinista (X_train, X_test, y_train, y_test). El set de entrenamiento es también
    el background implícito de las explicaciones SHAP (ver app.model.explain)."""
    X = raw_dataframe_to_features(df)
    y = df[TARGET_COLUMN]
    return train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)


def train_and_save(df: pd.DataFrame, output_dir: Path) -> dict:
    """Entrena el pipeline, lo evalúa y persiste una nueva versión inmutable en output_dir/<version>/."""
    X_train, X_test, y_train, y_test = split_train_test(df)

    pipeline = build_pipeline()
    pipeline.fit(X_train, y_train)

    test_probabilities = pipeline.predict_proba(X_test)[:, 1]
    auc = roc_auc_score(y_test, test_probabilities)

    version = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    version_dir = Path(output_dir) / version
    version_dir.mkdir(parents=True, exist_ok=True)

    joblib.dump(pipeline, version_dir / "model.joblib")

    metadata = {
        "version": version,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "training_rows": len(X_train),
        "test_rows": len(X_test),
        "metrics": {"roc_auc": auc},
        "feature_names": list(X_train.columns),
    }
    (version_dir / "metadata.json").write_text(json.dumps(metadata, indent=2))

    # Puntero simple a la versión activa. La Fase 4 (model registry) lo formalizará en BD.
    (Path(output_dir) / "ACTIVE_VERSION").write_text(version)

    return metadata


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=Path("data/raw/cs-training.csv"))
    parser.add_argument("--output-dir", type=Path, default=Path("models"))
    args = parser.parse_args()

    df = pd.read_csv(args.input, index_col=0)
    metadata = train_and_save(df, args.output_dir)

    print(f"Modelo entrenado: version={metadata['version']} roc_auc={metadata['metrics']['roc_auc']:.4f}")
    print(f"Guardado en: {args.output_dir / metadata['version']}")


if __name__ == "__main__":
    main()
