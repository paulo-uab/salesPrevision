from collections import defaultdict
from typing import Any, Optional

import pandas as pd

from app.schemas.config import TargetFieldConfig

_FREQ_TO_SEASON_PERIOD = {
    "D": 7,    # ciclo semanal para dados diários
    "W": 52,   # ciclo anual para dados semanais
    "ME": 12,  # ciclo anual para dados mensais
    "QE": 4,   # ciclo anual para dados trimestrais
    "YE": 1,   # sem sazonalidade para dados anuais
}

SeriesMap = dict[tuple[str, Optional[str]], pd.Series]


def build_series_map(
    records: list[dict[str, Any]],
    date_field: str,
    target_fields: list[TargetFieldConfig],
    group_field: Optional[str],
    frequency: str,
    exog_fields: Optional[list[TargetFieldConfig]] = None,
) -> tuple[SeriesMap, SeriesMap, list[str]]:
    """Converte os registos recebidos do batch-service em séries temporais prontas a modelar.

    Produz uma série por combinação (campo, grupo) — tanto para os campos alvo
    (a prever) como para os exógenos (entrada extra, nunca previstos), usando
    exatamente a mesma agregação/resampling para ambos, para que fiquem
    alinhados na mesma grelha temporal.

    Devolve (target_series_map, exog_series_map, warnings). exog_series_map
    fica vazio quando exog_fields não é passado — mantém-se o comportamento
    anterior para quem não usa variáveis exógenas.
    """
    if not records:
        raise ValueError("Lista de registos está vazia.")

    exog_fields = exog_fields or []
    exog_names = {f.field_name for f in exog_fields}
    all_fields = list(target_fields) + list(exog_fields)

    warnings: list[str] = []
    agg_per_field = {f.field_name: f.aggregation for f in all_fields}
    raw: dict[tuple[str, Optional[str]], list[tuple[pd.Timestamp, float]]] = defaultdict(list)
    date_invalid = 0
    value_invalid = 0

    for record in records:
        raw_date = record.get(date_field)
        if raw_date is None:
            date_invalid += 1
            continue
        try:
            date = pd.to_datetime(str(raw_date))
        except Exception:
            date_invalid += 1
            continue

        group_val: Optional[str] = None
        if group_field:
            gv = record.get(group_field)
            group_val = str(gv) if gv is not None else None

        for f in all_fields:
            rv = record.get(f.field_name)
            if rv is None:
                value_invalid += 1
                continue
            try:
                value = float(str(rv).replace(",", "."))
                raw[(f.field_name, group_val)].append((date, value))
            except (ValueError, TypeError):
                value_invalid += 1

    if date_invalid:
        warnings.append(f"{date_invalid} registo(s) ignorado(s) por data inválida ou em falta.")
    if value_invalid:
        warnings.append(f"{value_invalid} valor(es) ignorado(s) por campo em falta ou inválido.")

    if not raw:
        raise ValueError(
            "Nenhum registo válido encontrado. "
            "Verifique se date_field e target_fields correspondem aos campos enviados pelo pipeline."
        )

    target_result: SeriesMap = {}
    exog_result: SeriesMap = {}

    for (field_name, group_val), points in raw.items():
        label = f"'{field_name}'" + (f", grupo '{group_val}'" if group_val else "")

        if len(points) < 2:
            warnings.append(f"Campo {label} tem menos de 2 pontos válidos — ignorado.")
            continue

        dates, values = zip(*points)
        s = pd.Series(list(values), index=pd.DatetimeIndex(list(dates))).sort_index()
        s = s.resample(frequency).agg(agg_per_field.get(field_name, "sum"))
        s = s.ffill().fillna(0)

        if len(s) < 2:
            warnings.append(f"Campo {label} tem menos de 2 períodos após agregação — ignorado.")
            continue

        if field_name in exog_names:
            exog_result[(field_name, group_val)] = s
        else:
            target_result[(field_name, group_val)] = s

    if not target_result:
        raise ValueError(
            "Nenhuma série temporal válida pôde ser construída. "
            "Verifique a configuração de frequência e os dados enviados."
        )

    return target_result, exog_result, warnings


def default_season_period(frequency: str) -> int:
    return _FREQ_TO_SEASON_PERIOD.get(frequency, 12)
