FROM python:3.12-slim

WORKDIR /app

# Copied and installed separately from the rest of the source so Docker only
# re-runs `pip install` when requirements.txt itself changes, not on every
# code edit (dependency layer caching).
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app app
COPY evaluation evaluation

EXPOSE 8085
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8085"]
