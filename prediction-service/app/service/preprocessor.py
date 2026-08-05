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
) -> tuple[SeriesMap, list[str]]:
    """Converte os registos recebidos do batch-service em séries temporais prontas a modelar.

    Produz uma série por combinação (campo_alvo, grupo). Quando group_field é None,
    o grupo é None e toda a informação fica numa única série por campo.
    """
    if not records:
        raise ValueError("Lista de registos está vazia.")

    warnings: list[str] = []
    agg_per_field = {tf.field_name: tf.aggregation for tf in target_fields}
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

        for tf in target_fields:
            rv = record.get(tf.field_name)
            if rv is None:
                value_invalid += 1
                continue
            try:
                value = float(str(rv).replace(",", "."))
                raw[(tf.field_name, group_val)].append((date, value))
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

    result: SeriesMap = {}

    for (field_name, group_val), points in raw.items():
        if len(points) < 2:
            label = f"'{field_name}'" + (f", grupo '{group_val}'" if group_val else "")
            warnings.append(f"Campo {label} tem menos de 2 pontos válidos — ignorado.")
            continue

        dates, values = zip(*points)
        s = pd.Series(list(values), index=pd.DatetimeIndex(list(dates))).sort_index()
        s = s.resample(frequency).agg(agg_per_field.get(field_name, "sum"))
        s = s.ffill().fillna(0)

        if len(s) < 2:
            label = f"'{field_name}'" + (f", grupo '{group_val}'" if group_val else "")
            warnings.append(f"Campo {label} tem menos de 2 períodos após agregação — ignorado.")
            continue

        result[(field_name, group_val)] = s

    if not result:
        raise ValueError(
            "Nenhuma série temporal válida pôde ser construída. "
            "Verifique a configuração de frequência e os dados enviados."
        )

    return result, warnings


def default_season_period(frequency: str) -> int:
    return _FREQ_TO_SEASON_PERIOD.get(frequency, 12)
