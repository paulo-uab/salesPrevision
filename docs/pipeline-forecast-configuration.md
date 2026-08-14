# Forecast pipeline configuration guide

This guide explains how to configure a `ForecastPipeline` (pipeline-service)
so it produces correct forecasts in the `prediction-service` — in particular,
what each field means and why. The interactive reference (with per-field
examples) is available at `/swagger-ui.html` on pipeline-service and at
`/docs` on prediction-service; this document is the prose companion, with
the reasoning behind the design and a worked example.

## Table of contents

1. [The mental model: field roles](#1-the-mental-model-field-roles)
2. [Available models and when to use each](#2-available-models-and-when-to-use-each)
3. [Full example](#3-full-example)
4. [Configuration parameters, one by one](#4-configuration-parameters-one-by-one)
5. [Known limitations](#5-known-limitations)

---

## 1. The mental model: field roles

A pipeline doesn't loosely say "send these fields" — every field defined in
`fields` has a **role** (`forecastRole`) that tells the `prediction-service`
what to do with it. It's from these roles that `batch-service` automatically
derives `date_field`/`target_fields`/`group_field`/`exog_fields`, without
you having to repeat that configuration anywhere else.

| Role | How many | Required? | What it does |
|---|---|---|---|
| `DATE` | Exactly 1 | Yes | The temporal dimension of the series. |
| `TARGET` | 1 or more | Yes (at least 1) | What gets forecast. Each (target field × group) combination produces an independent series. |
| `GROUP` | 0 or 1 | No | Segments the data into separate series (e.g. one series per product category). Without this, all records form a single series per target field. |
| `EXOG` | 0 or more | No | Exogenous variable — **never forecast**, only extra context for the models that know how to use it (`arima`, `linear_regression`, `random_forest`, `xgboost`). Silently ignored by the rest. |
| `NONE` | — | — | Default when omitted. Field is extracted but not used in the forecast. |

If the pipeline doesn't have exactly one `DATE` and at least one `TARGET`,
creation fails immediately with 400 — you don't have to wait for a batch run
to discover something is missing.

## 2. Available models and when to use each

`forecastModel`/`controlModel` accept exactly these values (plain strings,
not a shared enum between Java and Python — see [limitations](#5-known-limitations)):

| Category | Model | When to use |
|---|---|---|
| Baseline | `naive` | Minimal reference — repeats the last value. Good baseline by default. |
| Baseline | `mean` | No clear trend or seasonality — uses the historical average. |
| Baseline | `drift` | Simple linear trend, no seasonality. |
| Baseline | `seasonal_naive` | Strong seasonality and history >= 1 full cycle (`season_period` observations). **Does not work with less history than that.** |
| Statistical | `ses` | Stable level, no trend or seasonality. |
| Statistical | `holtwinters` | Trend + seasonality. Automatically degrades to "trend only" when history is too short to detect seasonality (< 2×`season_period`). |
| Statistical | `arima` | More complex autocorrelation; the only one with confidence intervals and `EXOG` support. |
| ML | `linear_regression` | Roughly linear relationship between lags/features and the target; interpretable. |
| ML | `random_forest` | Non-linear patterns, robust to outliers; no normalization needed. |
| ML | `xgboost` | Like `random_forest`, generally more accurate with more data; supports `EXOG` and incremental training. |

**Rule of thumb**: always use `controlModel="naive"` (the default) unless
you have a specific reason for a different baseline — it's what gives the
"does the chosen model actually beat the trivial one?" comparison on every
single forecast, without you having to run anything separately.

## 3. Full example

Scenario: forecast weekly revenue (`Total Amount`) by product category
(`Product Category`), with a promotion exogenous variable, using XGBoost
against a `naive` baseline.

```json
POST /v1/api/pipelines
{
  "templateId": 1,
  "name": "Weekly sales forecast by category",
  "forecastModel": "xgboost",
  "controlModel": "naive",
  "frequency": "W",
  "forecastHorizon": 4,
  "nLags": 8,
  "includeDateFeatures": true,
  "fields": [
    {
      "sourceFieldName": "Date",
      "targetFieldName": "sale_date",
      "forecastRole": "DATE"
    },
    {
      "sourceFieldName": "Total Amount",
      "targetFieldName": "total_amount",
      "forecastRole": "TARGET",
      "aggregation": "sum"
    },
    {
      "sourceFieldName": "Product Category",
      "targetFieldName": "category",
      "forecastRole": "GROUP"
    },
    {
      "sourceFieldName": "Promotion",
      "targetFieldName": "promo",
      "forecastRole": "EXOG",
      "aggregation": "max"
    }
  ]
}
```

This produces, on every batch run, a 4-week forecast for each product
category, with `xgboost` using `promo` as an extra feature, and `naive`
running in parallel as a reference — with no further configuration needed
on the `prediction-service` side.

## 4. Configuration parameters, one by one

- **`frequency`** — aggregation granularity: `D`/`W`/`ME`/`QE`/`YE`. Pick the
  finest one that still gives enough signal per period; with few records,
  `D` produces series that are too sparse (many zero-record periods), and
  `seasonal_naive`/`holtwinters` lose the ability to detect seasonality.
- **`forecastHorizon`** — how many periods ahead to forecast, in the unit of
  `frequency`. A horizon that's too long relative to the available history
  tends to degrade any model's accuracy.
- **`seasonPeriod`** — only relevant for `seasonal_naive`/`holtwinters`. If
  omitted, it's inferred from `frequency` (12 for monthly, 52 for weekly,
  etc.). Worth confirming the available history covers at least that many
  periods.
- **`nLags`** — how many past values the ML models see as features. More
  lags = more context, but also more training observations "lost" at the
  start (the first `nLags` rows of the series don't have enough lags and
  don't enter training).
- **`incrementalTraining`** — only activates for `arima`/`xgboost`; avoids
  refitting the model from scratch on every scheduled run, reusing the
  previous state instead. Does not combine with `EXOG` — with exogenous
  variables present, the model always fits from scratch.

## 5. Known limitations

Worth keeping these in mind, and mentioning them explicitly in the report
as conscious decisions, not hidden gaps:

- **`EXOG` has no channel for known future values.** The system assumes
  the last observed value of the exogenous variable stays constant for the
  whole forecast horizon. For pre-planned, one-off promotions (e.g. "I know
  I'll be running a campaign next week"), this isn't used — it only helps
  the model explain the past, not anticipate known future events.
- **Model names are not validated by a shared enum.** `forecastModel`,
  `controlModel`, `aggregation` and `frequency` are free strings on the
  pipeline-service side — only the `prediction-service` (Pydantic) validates
  whether the value is one of the allowed ones, returning 422 if not. This
  was a deliberate choice (avoiding serialization issues between a Java enum
  and a Python `Literal`), but it means a typo in these fields is only
  caught when `batch-service` actually calls the `prediction-service`, not
  at pipeline creation time.
