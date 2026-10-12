"""Genera un dataset sintético con el mismo esquema que "Give Me Some Credit", SOLO para demos y tests.

El dataset real de Kaggle requiere cuenta para descargarse. Este generador permite levantar el
sistema completo sin él: las etiquetas salen de una relación de riesgo con sentido (más moras,
más utilización y más endeudamiento suben el riesgo; más ingreso y más edad lo bajan), así que un
modelo entrenado aquí produce decisiones y explicaciones coherentes. Sus métricas no dicen nada
sobre el mundo real; las versiones entrenadas con él quedan marcadas por su `dataset.source`.

Uso:
    python -m training.synthetic --output data/raw/synthetic-demo.csv --rows 20000
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

from training.preprocessing import RAW_COLUMN_NAMES, TARGET_COLUMN


def generate(rows: int = 20_000, seed: int = 7) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    utilization = np.clip(rng.beta(1.2, 2.5, rows), 0, 1.5)
    age = rng.integers(21, 80, rows)
    late_30_59 = rng.poisson(0.25, rows)
    late_60_89 = rng.poisson(0.08, rows)
    late_90 = rng.poisson(0.07, rows)
    income = np.round(rng.lognormal(mean=np.log(4500), sigma=0.55, size=rows), 2)
    debt_ratio = np.clip(rng.gamma(2.0, 0.15, rows), 0, 3)
    open_lines = rng.poisson(8, rows)
    real_estate = rng.poisson(1.0, rows)
    dependents = rng.poisson(0.8, rows).astype(float)

    logit = (
        -3.4
        + 2.6 * utilization
        + 0.55 * late_30_59
        + 0.9 * late_60_89
        + 1.3 * late_90
        + 0.9 * np.minimum(debt_ratio, 2)
        - 0.025 * (age - 45)
        - 0.45 * np.log(income / 4500)
        + 0.08 * dependents
    )
    default = rng.random(rows) < 1 / (1 + np.exp(-logit))

    # Como en el dataset real, el ingreso y los dependientes a veces faltan (el pipeline imputa medianas).
    income[rng.random(rows) < 0.05] = np.nan
    dependents[rng.random(rows) < 0.02] = np.nan

    df = pd.DataFrame(
        {
            TARGET_COLUMN: default.astype(int),
            "RevolvingUtilizationOfUnsecuredLines": utilization,
            "age": age,
            "NumberOfTime30-59DaysPastDueNotWorse": late_30_59,
            "DebtRatio": debt_ratio,
            "MonthlyIncome": income,
            "NumberOfOpenCreditLinesAndLoans": open_lines,
            "NumberOfTimes90DaysLate": late_90,
            "NumberRealEstateLoansOrLines": real_estate,
            "NumberOfTime60-89DaysPastDueNotWorse": late_60_89,
            "NumberOfDependents": dependents,
        }
    )
    df.index = pd.RangeIndex(1, rows + 1)
    return df[[TARGET_COLUMN, *RAW_COLUMN_NAMES]]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--output", type=Path, default=Path("data/raw/synthetic-demo.csv"))
    parser.add_argument("--rows", type=int, default=20_000)
    parser.add_argument("--seed", type=int, default=7)
    args = parser.parse_args()

    df = generate(args.rows, args.seed)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(args.output)
    print(f"Dataset sintético (solo demo): {len(df)} filas, tasa de default {df[TARGET_COLUMN].mean():.1%} -> {args.output}")


if __name__ == "__main__":
    main()
