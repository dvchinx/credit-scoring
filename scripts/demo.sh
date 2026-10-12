#!/usr/bin/env bash
# Deja el sistema listo para explorar: entrena un modelo, lo registra y activa, y crea y evalúa
# solicitudes de ejemplo. Requiere el stack levantado con `docker compose up -d --build`.
#
# Usa el dataset real (ml-service/data/raw/cs-training.csv) si está descargado; si no, genera uno
# sintético SOLO para demo (sus métricas no dicen nada del mundo real).
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then set -a; . ./.env; set +a; fi
API="${API_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USERNAME:-admin}"
ADMIN_PASS="${ADMIN_PASSWORD:-ChangeMe123!}"
if docker compose version >/dev/null 2>&1; then DC="docker compose"; else DC="docker-compose"; fi

json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
step() { printf '\n==> %s\n' "$1"; }

step "Esperando al backend en $API"
for _ in $(seq 1 60); do
  curl -sf "$API/actuator/health" >/dev/null && break
  sleep 2
done
curl -sf "$API/actuator/health" >/dev/null || { echo "El backend no responde en $API" >&2; exit 1; }

step "Entrenando una versión de modelo"
if [ -f ml-service/data/raw/cs-training.csv ]; then
  DATASET=/app/data/raw/cs-training.csv
  echo "Dataset real de Kaggle: $DATASET"
else
  DATASET=/app/data/raw/synthetic-demo.csv
  $DC exec -T ml-service python -m training.synthetic --output "$DATASET"
fi
VERSION=$($DC exec -T ml-service python -m training.train --input "$DATASET" --output-dir /app/models \
  | tee /dev/stderr | sed -n 's/.*version=\([^ ]*\).*/\1/p')
[ -n "$VERSION" ] || { echo "No se pudo leer la versión entrenada" >&2; exit 1; }

step "Registrando y activando la versión $VERSION"
TOKEN=$(curl -sf -H 'Content-Type: application/json' \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" "$API/auth/login" | json "d['token']")
AUTH="Authorization: Bearer $TOKEN"
curl -sf -H "$AUTH" -H 'Content-Type: application/json' -d "{\"version\":\"$VERSION\"}" "$API/model-versions" \
  | json "'registrada', d['version'], '· ROC-AUC', round(d['metrics']['roc_auc'], 3)"
curl -sf -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"version\":\"$VERSION\",\"reason\":\"Demo: versión inicial\"}" "$API/model-activations" \
  | json "'activa', d['modelVersion']"

step "Creando y evaluando solicitudes de ejemplo"
RUN=$(date +%s)
create_and_evaluate() {
  local name=$1 income=$2 amount=$3 utilization=$4 late30=$5 late60=$6 late90=$7
  local id
  id=$(curl -sf -H "$AUTH" -H 'Content-Type: application/json' -d "{
      \"applicantFullName\": \"$name\", \"documentId\": \"DEMO-$RUN-${name// /-}\", \"birthDate\": \"1987-04-12\",
      \"monthlyIncome\": $income, \"requestedAmount\": $amount, \"loanTermMonths\": 24, \"employmentYears\": 6,
      \"existingMonthlyDebt\": 250, \"numberOfDependents\": 1,
      \"creditHistory\": {\"revolvingUtilization\": $utilization, \"openCreditLines\": 6, \"realEstateLoans\": 1,
        \"latePayments30To59Days\": $late30, \"latePayments60To89Days\": $late60, \"latePayments90DaysOrMore\": $late90}
    }" "$API/credit-applications" | json "d['id']")
  curl -sf -H "$AUTH" -X POST "$API/credit-applications/$id/decisions" \
    | json "f\"{'$name':<18} {d['outcome']:<14} PD {d['probabilityOfDefault']:.1%}  score {d['riskScore']}\"" >&2
  echo "$id"
}
create_and_evaluate "Ana Bajo Riesgo"   6500 8000 0.10 0 0 0 >/dev/null
create_and_evaluate "Eva Alto Riesgo"   2400 6000 0.95 3 2 2 >/dev/null
MEDIUM_ID=$(create_and_evaluate "Luis Riesgo Medio" 4000 9000 0.35 1 0 0)

step "What-if sobre Luis: ¿y si bajara su utilización al 10% y no tuviera moras?"
curl -sf -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"creditHistory":{"revolvingUtilization":0.10,"latePayments30To59Days":0}}' \
  "$API/credit-applications/$MEDIUM_ID/what-if" \
  | json "f\"{d['baseline']['outcome']} -> {d['scenario']['outcome']} (PD {d['probabilityOfDefaultDelta']:+.1%})\""

cat <<MSG

Listo. Frontend: http://localhost:3000  (usuario: $ADMIN_USER)
MSG
