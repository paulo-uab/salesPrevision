import numpy as np


def mae(actual: list[float], predicted: list[float]) -> float:
    a, p = np.array(actual, dtype=float), np.array(predicted, dtype=float)
    return float(np.mean(np.abs(a - p)))


def rmse(actual: list[float], predicted: list[float]) -> float:
    a, p = np.array(actual, dtype=float), np.array(predicted, dtype=float)
    return float(np.sqrt(np.mean((a - p) ** 2)))


def mape(actual: list[float], predicted: list[float]) -> float | None:
    """Percentagem — None se todos os valores reais forem 0 (indefinido nesse caso)."""
    a, p = np.array(actual, dtype=float), np.array(predicted, dtype=float)
    nonzero = a != 0
    if not nonzero.any():
        return None
    return float(np.mean(np.abs((a[nonzero] - p[nonzero]) / a[nonzero])) * 100)
