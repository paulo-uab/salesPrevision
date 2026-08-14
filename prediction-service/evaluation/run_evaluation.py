"""Avaliação experimental: baseline simples vs. modelos mais avançados.

Dataset: retail_sales_dataset.csv (Kaggle — confirmar e citar o link exato do
dataset no relatório final; 1000 transações, ano de 2023, 3 categorias de
produto: Beauty, Clothing, Electronics).

O que é previsto: Total Amount (receita), somada por semana, por Product
Category — 3 séries temporais de ~52 pontos cada.

Metodologia: validação em janela deslizante (walk-forward) com 2 folds,
horizonte de 4 semanas por fold, cobrindo as últimas 8 semanas do dataset
como teste. Cada fold treina só com dados anteriores ao seu próprio corte —
nunca há fuga de informação do futuro para o treino.

seasonal_naive foi deliberadamente excluído: precisa de pelo menos
season_period (52) semanas só para conseguir ajustar-se, e o conjunto de
treino de cada fold tem menos do que isso — não é executável com este
dataset, não é uma falha do código.

Corre com: python -m evaluation.run_evaluation (a partir de prediction-service/)
"""
from pathlib import Path

import pandas as pd

from app.schemas.config import PipelineConfig, TargetFieldConfig
from app.schemas.forecast import ForecastRequest
from app.service.forecast_service import ForecastService
from app.service.preprocessor import build_series_map
from evaluation.metrics import mae, mape, rmse

DATA_PATH = Path(__file__).parent.parent / "data" / "retail_sales_dataset.csv"
RESULTS_PATH = Path(__file__).parent / "results.csv"

DATE_FIELD = "Date"
TARGET_FIELD = "Total Amount"
GROUP_FIELD = "Product Category"
FREQUENCY = "W"
BASELINE_MODEL = "naive"
CANDIDATE_MODELS = ["naive", "mean", "drift", "ses", "holtwinters", "arima", "random_forest", "xgboost"]

TEST_WEEKS = 8
FOLD_HORIZON = 4
N_LAGS = 8


def _run_forecast(train_records: list[dict], model: str, horizon: int) -> dict:
    config = PipelineConfig(
        date_field=DATE_FIELD,
        target_fields=[TargetFieldConfig(field_name=TARGET_FIELD, aggregation="sum")],
        group_field=GROUP_FIELD,
        frequency=FREQUENCY,
        forecast_horizon=horizon,
        model=model,
        control_model=BASELINE_MODEL,
        n_lags=N_LAGS,
    )
    request = ForecastRequest(
        pipelineId=1,
        pipelineName="Retail Sales Evaluation",
        scheduleConfigId=1,
        batchExecutionId=1,
        recordCount=len(train_records),
        records=train_records,
        config=config,
    )
    response = ForecastService().run_forecast(request)

    # model -> group -> period -> value (há 1 target_field mas vários grupos;
    # indexar só por período perderia as categorias, que partilham os mesmos
    # timestamps de previsão)
    by_model: dict[str, dict[str, dict[str, float]]] = {}
    for p in response.predictions:
        by_model.setdefault(p.model, {}).setdefault(p.group, {})[p.period] = p.value
    return by_model


def main() -> None:
    df = pd.read_csv(DATA_PATH)
    df[DATE_FIELD] = pd.to_datetime(df[DATE_FIELD])
    all_records = df.to_dict("records")

    full_series_map, _, warnings = build_series_map(
        records=all_records,
        date_field=DATE_FIELD,
        target_fields=[TargetFieldConfig(field_name=TARGET_FIELD, aggregation="sum")],
        group_field=GROUP_FIELD,
        frequency=FREQUENCY,
    )
    for w in warnings:
        print(f"[aviso preprocessamento] {w}")

    categories = sorted({group for (_, group) in full_series_map.keys()})
    lengths = {cat: len(full_series_map[(TARGET_FIELD, cat)]) for cat in categories}
    if len(set(lengths.values())) != 1:
        raise RuntimeError(
            f"As séries por categoria têm comprimentos diferentes ({lengths}); "
            "a grelha semanal de referência assume que são iguais."
        )

    reference_index = full_series_map[(TARGET_FIELD, categories[0])].index
    n_total = len(reference_index)
    n_folds = TEST_WEEKS // FOLD_HORIZON

    rows: list[dict] = []

    for fold in range(n_folds):
        train_end_pos = (n_total - TEST_WEEKS) + fold * FOLD_HORIZON
        cutoff = reference_index[train_end_pos - 1]
        fold_test_weeks = reference_index[train_end_pos: train_end_pos + FOLD_HORIZON]

        train_df = df[df[DATE_FIELD] <= cutoff]
        train_records = train_df.to_dict("records")

        print(f"\n=== Fold {fold + 1}/{n_folds} — treino até {cutoff.date()}, "
              f"teste {fold_test_weeks[0].date()}..{fold_test_weeks[-1].date()} "
              f"({len(train_records)} registos de treino) ===")

        for candidate in CANDIDATE_MODELS:
            try:
                by_model = _run_forecast(train_records, candidate, FOLD_HORIZON)
            except Exception as exc:
                print(f"  [{candidate}] falhou: {exc}")
                continue

            for model_tag, groups in by_model.items():
                for category in categories:
                    period_values = groups.get(category, {})
                    actual_series = full_series_map[(TARGET_FIELD, category)].loc[fold_test_weeks]
                    predicted = [period_values.get(ts.isoformat()) for ts in fold_test_weeks]
                    if any(v is None for v in predicted):
                        continue  # este candidato não produziu previsão para este grupo (ex: erro específico dessa série)

                    rows.append({
                        "fold": fold + 1,
                        "model": model_tag,
                        "is_baseline": model_tag == BASELINE_MODEL,
                        "category": category,
                        "mae": mae(actual_series.tolist(), predicted),
                        "rmse": rmse(actual_series.tolist(), predicted),
                        "mape": mape(actual_series.tolist(), predicted),
                    })

    results = pd.DataFrame(rows)
    results.to_csv(RESULTS_PATH, index=False)

    print("\n=== Resultados por modelo (média entre categorias e folds) ===")
    summary = results.groupby("model")[["mae", "rmse", "mape"]].mean().sort_values("mae")
    print(summary.to_string(float_format=lambda v: f"{v:.2f}"))
    print(f"\nResultados completos gravados em {RESULTS_PATH}")


if __name__ == "__main__":
    main()
