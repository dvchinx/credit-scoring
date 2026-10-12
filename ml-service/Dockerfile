FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
# Timeout y reintentos generosos: las wheels de scikit-learn/pandas son grandes y PyPI a veces es lento.
RUN pip install --no-cache-dir --default-timeout=120 --retries 5 -r requirements.txt

# Usuario sin privilegios con uid 1000 (el habitual del primer usuario en Linux): los modelos y datos
# que escribe en los volúmenes montados quedan a nombre del usuario del host, no de root.
RUN useradd --uid 1000 --create-home app

COPY --chown=app app/ app/
COPY --chown=app training/ training/
RUN mkdir -p models data/raw && chown -R app /app
USER app

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
