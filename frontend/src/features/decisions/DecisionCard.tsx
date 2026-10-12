import type { CreditDecision } from "../../api/types";
import { StatusBadge } from "../../components/Badges";
import { formatDateTime, formatNumber, formatPercent, formatSigned } from "../../components/format";
import { featureLabel } from "./featureLabels";
import { RiskBand } from "./RiskBand";
import { ShapChart } from "./ShapChart";

const TOP_FACTORS = 5;

export function DecisionCard({ decision }: { decision: CreditDecision }) {
  const failed = decision.status === "FAILED";
  return (
    <article
      className={`decision${failed ? " decision-failed" : ""}`}
      aria-label={`Decisión del ${formatDateTime(decision.decidedAt)}`}
    >
      <header className="decision-header">
        <StatusBadge value={failed ? "FAILED" : decision.outcome!} />
        <span className="muted">
          {formatDateTime(decision.decidedAt)} · {decision.decidedBy}
        </span>
        <span className="spacer" />
        {decision.modelVersion && <span className="chip mono">modelo {decision.modelVersion}</span>}
      </header>

      <div className="decision-body">
        {failed ? (
          <p className="error">
            <span>
              <strong>Intento fallido:</strong> {decision.failureReason}. La solicitud no cambió de estado.
            </span>
          </p>
        ) : (
          <CompletedDecision decision={decision} />
        )}
      </div>
    </article>
  );
}

function CompletedDecision({ decision }: { decision: CreditDecision }) {
  const contributions = decision.explanation?.contributions ?? [];
  const policy = decision.policy;
  const probability = decision.probabilityOfDefault ?? 0;
  return (
    <>
      <dl className="metrics">
        <div className="metric metric-wide">
          <dt>Probabilidad de default</dt>
          <dd>{formatPercent(probability)}</dd>
          <RiskBand probability={probability} policy={policy} />
        </div>
        <div className="metric">
          <dt>Score de riesgo</dt>
          <dd>{decision.riskScore}</dd>
        </div>
        <div className="metric">
          <dt>Política aplicada</dt>
          <dd className="metric-text">
            aprueba ≤ {formatPercent(policy.approveMaxProbability, 0)} · rechaza ≥{" "}
            {formatPercent(policy.rejectMinProbability, 0)} · endeudamiento máx. {formatPercent(policy.maxDebtRatio, 0)}
          </dd>
        </div>
      </dl>

      <h3>Motivos</h3>
      <ul className="reasons">
        {decision.reasons.map((reason) => (
          <li key={reason}>{reason}</li>
        ))}
      </ul>

      <h3>¿Qué influyó en el riesgo?</h3>
      <p className="muted small">
        Contribución de cada variable (valores SHAP, en log-odds de default). Positivo sube el riesgo; negativo lo baja.
      </p>
      <div className="explain">
        <ShapChart
          title="Contribución de cada variable al riesgo"
          bars={contributions.map((c) => ({ feature: c.feature, value: c.shapValue }))}
        />
        <div>
          <p className="eyebrow">Las {Math.min(TOP_FACTORS, contributions.length)} que más pesaron</p>
          <ol className="factors" aria-label="Variables que más influyeron">
            {contributions.slice(0, TOP_FACTORS).map((contribution) => (
              <li key={contribution.feature} className={directionClass(contribution.direction)}>
                <strong>{featureLabel(contribution.feature)}</strong> = {formatNumber(contribution.value)}:{" "}
                {contribution.direction === "INCREASES_RISK"
                  ? "sube"
                  : contribution.direction === "DECREASES_RISK"
                    ? "baja"
                    : "no cambia"}{" "}
                el riesgo ({formatSigned(contribution.shapValue, 3)})
              </li>
            ))}
          </ol>
        </div>
      </div>
    </>
  );
}

function directionClass(direction: string): string {
  if (direction === "INCREASES_RISK") return "up";
  if (direction === "DECREASES_RISK") return "down";
  return "";
}
