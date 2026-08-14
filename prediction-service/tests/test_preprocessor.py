import pandas as pd
import pytest

from app.schemas.config import TargetFieldConfig
from app.service.preprocessor import build_series_map

_TARGETS = [TargetFieldConfig(field_name="sales")]

_BASE_RECORDS = [
    {"date": "2023-01-15", "sales": "100"},
    {"date": "2023-02-10", "sales": "120"},
    {"date": "2023-03-20", "sales": "130"},
    {"date": "2023-04-05", "sales": "110"},
]


def test_single_field_no_group():
    series_map, exog_map, warnings = build_series_map(_BASE_RECORDS, "date", _TARGETS, None, "ME")
    assert len(series_map) == 1
    assert ("sales", None) in series_map
    assert len(series_map[("sales", None)]) == 4
    assert len(warnings) == 0
    assert exog_map == {}


def test_values_correct():
    series_map, _, _ = build_series_map(_BASE_RECORDS, "date", _TARGETS, None, "ME")
    s = series_map[("sales", None)]
    assert s.iloc[0] == 100.0
    assert s.iloc[1] == 120.0


def test_series_has_datetime_index():
    series_map, _, _ = build_series_map(_BASE_RECORDS, "date", _TARGETS, None, "ME")
    assert isinstance(series_map[("sales", None)].index, pd.DatetimeIndex)


def test_multiple_target_fields():
    targets = [TargetFieldConfig(field_name="sales"), TargetFieldConfig(field_name="qty")]
    records = [
        {"date": "2023-01-01", "sales": "100", "qty": "10"},
        {"date": "2023-02-01", "sales": "120", "qty": "12"},
    ]
    series_map, _, _ = build_series_map(records, "date", targets, None, "ME")
    assert ("sales", None) in series_map
    assert ("qty", None) in series_map
    assert series_map[("qty", None)].iloc[0] == 10.0


def test_group_field_creates_separate_series():
    records = [
        {"date": "2023-01-01", "sales": "100", "produto": "A"},
        {"date": "2023-01-01", "sales": "200", "produto": "B"},
        {"date": "2023-02-01", "sales": "110", "produto": "A"},
        {"date": "2023-02-01", "sales": "210", "produto": "B"},
    ]
    series_map, _, _ = build_series_map(records, "date", _TARGETS, "produto", "ME")
    assert ("sales", "A") in series_map
    assert ("sales", "B") in series_map
    assert series_map[("sales", "A")].iloc[0] == 100.0
    assert series_map[("sales", "B")].iloc[0] == 200.0


def test_aggregation_sum():
    records = [
        {"date": "2023-01-01", "sales": "100"},
        {"date": "2023-01-15", "sales": "50"},
        {"date": "2023-02-01", "sales": "200"},
    ]
    series_map, _, _ = build_series_map(records, "date", _TARGETS, None, "ME")
    assert series_map[("sales", None)].iloc[0] == 150.0


def test_aggregation_mean():
    targets = [TargetFieldConfig(field_name="sales", aggregation="mean")]
    records = [
        {"date": "2023-01-01", "sales": "100"},
        {"date": "2023-01-15", "sales": "50"},
        {"date": "2023-02-01", "sales": "200"},
    ]
    series_map, _, _ = build_series_map(records, "date", targets, None, "ME")
    assert series_map[("sales", None)].iloc[0] == 75.0


def test_aggregation_last():
    targets = [TargetFieldConfig(field_name="sales", aggregation="last")]
    records = [
        {"date": "2023-01-01", "sales": "100"},
        {"date": "2023-01-15", "sales": "50"},
        {"date": "2023-02-01", "sales": "200"},
    ]
    series_map, _, _ = build_series_map(records, "date", targets, None, "ME")
    assert series_map[("sales", None)].iloc[0] == 50.0


def test_skips_invalid_date():
    records = _BASE_RECORDS + [{"date": "nao-e-data", "sales": "99"}]
    series_map, _, warnings = build_series_map(records, "date", _TARGETS, None, "ME")
    assert ("sales", None) in series_map
    assert any("data" in w.lower() for w in warnings)


def test_skips_invalid_value():
    records = _BASE_RECORDS + [{"date": "2023-05-01", "sales": "abc"}]
    _, _, warnings = build_series_map(records, "date", _TARGETS, None, "ME")
    assert len(warnings) == 1
    assert "valor" in warnings[0].lower()


def test_skips_missing_field():
    records = _BASE_RECORDS + [{"date": "2023-05-01"}]
    _, _, warnings = build_series_map(records, "date", _TARGETS, None, "ME")
    assert len(warnings) == 1


def test_comma_decimal_separator():
    records = [
        {"date": "2023-01-01", "sales": "1500,75"},
        {"date": "2023-02-01", "sales": "2000,00"},
    ]
    series_map, _, _ = build_series_map(records, "date", _TARGETS, None, "ME")
    assert abs(series_map[("sales", None)].iloc[0] - 1500.75) < 1e-9


def test_empty_records_raises():
    with pytest.raises(ValueError, match="vazia"):
        build_series_map([], "date", _TARGETS, None, "ME")


def test_no_valid_data_raises():
    with pytest.raises(ValueError):
        build_series_map([{"date": "nao-e-data", "sales": "100"}], "date", _TARGETS, None, "ME")


# ---------- campos exógenos ----------

def test_exog_field_returned_separately_from_targets():
    exog = [TargetFieldConfig(field_name="promo", aggregation="max")]
    records = [
        {"date": "2023-01-01", "sales": "100", "promo": "0"},
        {"date": "2023-01-15", "sales": "50", "promo": "1"},
        {"date": "2023-02-01", "sales": "200", "promo": "0"},
    ]
    series_map, exog_map, _ = build_series_map(records, "date", _TARGETS, None, "ME", exog_fields=exog)
    assert ("sales", None) in series_map
    assert ("promo", None) not in series_map
    assert ("promo", None) in exog_map
    # max no período: houve promo=1 em janeiro
    assert exog_map[("promo", None)].iloc[0] == 1.0


def test_exog_field_optional_defaults_to_empty():
    series_map, exog_map, _ = build_series_map(_BASE_RECORDS, "date", _TARGETS, None, "ME")
    assert exog_map == {}
    assert ("sales", None) in series_map


def test_missing_target_still_raises_with_exog_present():
    exog = [TargetFieldConfig(field_name="promo")]
    records = [{"date": "2023-01-01", "promo": "1"}]  # sem "sales"
    with pytest.raises(ValueError):
        build_series_map(records, "date", _TARGETS, None, "ME", exog_fields=exog)
