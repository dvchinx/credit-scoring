import type { CreditDecision } from "../../api/types";
import { StatusBadge } from "../../components/Badges";
import { formatDateTime, formatNumber, formatPercent, formatSigned } from "../../components/format";
import { featureLabel } from "./featureLabels";
import { ShapChart } from "./ShapChart";

const TOP_FACTORS = 5;

export function DecisionCard({ decision }: { decision: CreditDecision }) {
  const failed = decision.status === "FAILED";
  return (
    <article className="decision" aria-label={`Decisión del ${formatDateTime(decision.decidedAt)}`}>
      <header className="decision-header">
        <StatusBadge value={failed ? "FAILED" : decision.outcome!} />
        <span className="muted">
          {formatDateTime(decision.decidedAt)} · {decision.decidedBy}
          {decision.modelVersion && <> · modelo {decision.modelVersion}</>}
        </span>
      </header>

      {failed ? (
        <p className="error">
          <strong>Intento fallido:</strong> {decision.failureReason}. La solicitud no cambió de estado.
        </p>
      ) : (
        <CompletedDecision decision={decision} />
      )}
    </article>
  );
}

function CompletedDecision({ decision }: { decision: CreditDecision }) {
  const contributions = decision.explanation?.contributions ?? [];
  const policy = decision.policy;
  return (
    <>
      <dl className="metrics">
        <div>
          <dt>Probabilidad de default</dt>
          <dd>{formatPercent(decision.probabilityOfDefault ?? 0)}</dd>
        </div>
        <div>
          <dt>Score de riesgo</dt>
          <dd>{decision.riskScore}</dd>
        </div>
        <div>
          <dt>Política aplicada</dt>
          <dd className="small">
            aprueba ≤ {formatPercent(policy.approveMaxProbability, 0)} · rechaza ≥{" "}
            {formatPercent(policy.rejectMinProbability, 0)} · endeudamiento máx. {formatPercent(policy.maxDebtRatio, 0)}
          </dd>
        </div>
      </dl>

      <h3>Motivos</h3>
      <ul>
        {decision.reasons.map((reason) => (
          <li key={reason}>{reason}</li>
        ))}
      </ul>

      <h3>¿Qué influyó en el riesgo?</h3>
      <p className="muted small">
        Contribución de cada variable (valores SHAP, en log-odds de default). Positivo sube el riesgo; negativo lo baja.
      </p>
      <ShapChart title="Contribución de cada variable al riesgo" bars={contributions.map((c) => ({ feature: c.feature, value: c.shapValue }))} />
      <ol className="factors" aria-label="Variables que más influyeron">
        {contributions.slice(0, TOP_FACTORS).map((contribution) => (
          <li key={contribution.feature}>
            <strong>{featureLabel(contribution.feature)}</strong> = {formatNumber(contribution.value)}:{" "}
            {contribution.direction === "INCREASES_RISK" ? "sube" : contribution.direction === "DECREASES_RISK" ? "baja" : "no cambia"}{" "}
            el riesgo ({formatSigned(contribution.shapValue, 3)})
          </li>
        ))}
      </ol>
    </>
  );
}
