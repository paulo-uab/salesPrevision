from typing import Optional

import numpy as np
import pandas as pd


class TimeSeriesFeatureBuilder:
    """Transforma uma série temporal num problema de aprendizagem supervisionada.

    Features:
    - Lag features: y(t-1) … y(t-n_lags)
    - Rolling mean (3 e 6 períodos) e rolling std (3 períodos)
    - Date features: mês e trimestre (quando include_date_features=True); dia da
      semana e fim de semana adicionais quando frequency="D" — para granularidades
      >= semanal, cada ponto já é uma agregação de vários dias, pelo que "dia da
      semana" seria sempre a mesma constante (não informativo).
    - Variáveis exógenas (quando fornecidas): valor contemporâneo de cada uma,
      alinhado ao mesmo período da observação alvo.
    - Trend: posição temporal (índice inteiro crescente)

    A previsão multi-passo usa estratégia recursiva: cada previsão é
    adicionada ao contexto antes do passo seguinte.
    """

    def __init__(self, n_lags: int = 12, include_date_features: bool = True, frequency: str = "ME") -> None:
        self.n_lags = n_lags
        self.include_date_features = include_date_features
        self.frequency = frequency
        self._feature_names: list[str] = []

    # ------------------------------------------------------------------
    # Interface pública
    # ------------------------------------------------------------------

    def fit_transform(self, series: pd.Series, exog: Optional[pd.DataFrame] = None) -> tuple[pd.DataFrame, pd.Series]:
        """Constrói features de treino a partir da série completa. Devolve (X, y)."""
        X, y = self._build_training(series, exog)
        self._feature_names = list(X.columns)
        return X, y

    def prediction_features(
        self,
        context: list[float],
        step: int,
        last_date: pd.Timestamp,
        freq,
        exog_values: Optional[dict[str, float]] = None,
    ) -> pd.DataFrame:
        """Constrói o vector de features para um único passo de previsão.

        context      — valores observados + previsões anteriores (cresce a cada passo)
        step         — índice do passo actual (0-based)
        last_date    — último timestamp da série de treino
        freq         — frequência pandas (e.g. pd.tseries.frequencies.to_offset("ME"))
        exog_values  — valor assumido de cada variável exógena para este passo
                       (esta versão não tem canal para valores futuros conhecidos,
                       por isso é sempre o último valor observado, repetido)
        """
        n = len(context)
        row: dict[str, float] = {}

        for i in range(1, self.n_lags + 1):
            row[f"lag_{i}"] = float(context[n - i]) if n >= i else np.nan

        row["rolling_mean_3"] = float(np.mean(context[max(0, n - 3):]))
        row["rolling_mean_6"] = float(np.mean(context[max(0, n - 6):]))
        row["rolling_std_3"] = float(np.std(context[max(0, n - 3):])) if n >= 3 else 0.0

        if self.include_date_features:
            future_date = last_date + (step + 1) * pd.tseries.frequencies.to_offset(freq)
            row.update(self._date_features(future_date))

        if exog_values:
            for col, val in exog_values.items():
                row[col] = float(val)

        row["trend"] = float(n)

        return pd.DataFrame([row], columns=self._feature_names)

    # ------------------------------------------------------------------
    # Interno
    # ------------------------------------------------------------------

    def _date_features(self, ts: pd.Timestamp) -> dict[str, float]:
        features = {"month": float(ts.month), "quarter": float(ts.quarter)}
        if self.frequency == "D":
            features["day_of_week"] = float(ts.dayofweek)
            features["is_weekend"] = float(ts.dayofweek >= 5)
        return features

    def _build_training(
        self, series: pd.Series, exog: Optional[pd.DataFrame] = None,
    ) -> tuple[pd.DataFrame, pd.Series]:
        vals = series.values.astype(float)
        n = len(vals)
        rows: list[dict] = []

        exog_aligned = None
        if exog is not None and not exog.empty:
            # reindex ao índice da série alvo — exógenas podem ter cobertura
            # temporal ligeiramente diferente; ffill/bfill evita NaN nas pontas.
            exog_aligned = exog.reindex(series.index).ffill().bfill()

        for t in range(self.n_lags, n):
            row: dict[str, float] = {}

            for i in range(1, self.n_lags + 1):
                row[f"lag_{i}"] = vals[t - i]

            row["rolling_mean_3"] = float(np.mean(vals[max(0, t - 3):t]))
            row["rolling_mean_6"] = float(np.mean(vals[max(0, t - 6):t]))
            row["rolling_std_3"] = float(np.std(vals[max(0, t - 3):t])) if t >= 3 else 0.0

            if self.include_date_features and isinstance(series.index, pd.DatetimeIndex):
                row.update(self._date_features(series.index[t]))

            if exog_aligned is not None:
                for col in exog_aligned.columns:
                    row[col] = float(exog_aligned[col].iloc[t])

            row["trend"] = float(t)
            rows.append(row)

        X = pd.DataFrame(rows)
        y = pd.Series(vals[self.n_lags:], name="y")
        return X, y
