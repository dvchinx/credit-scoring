import type { Policy } from "../../api/types";
import { formatPercent } from "../../components/format";

/**
 * Ubica la probabilidad de default frente a los umbrales de la política con la que se decidió.
 * Solo presenta datos del backend: la decisión puede venir además de reglas duras (edad, endeudamiento).
 */
export function RiskBand({ probability, policy }: { probability: number; policy: Policy }) {
  const approve = policy.approveMaxProbability * 100;
  const reject = policy.rejectMinProbability * 100;
  const position = Math.min(100, Math.max(0, probability * 100));
  return (
    <div
      className="risk-band"
      role="img"
      aria-label={`Probabilidad de default ${formatPercent(probability)} frente a los umbrales: aprueba hasta ${formatPercent(policy.approveMaxProbability, 0)}, rechaza desde ${formatPercent(policy.rejectMinProbability, 0)}`}
    >
      <div className="risk-band-track">
        <span className="zone-good" style={{ width: `${approve}%` }} />
        <span className="zone-warn" style={{ width: `${reject - approve}%` }} />
        <span className="zone-bad" style={{ width: `${100 - reject}%` }} />
        <span className="risk-band-marker" style={{ left: `${position}%` }} />
      </div>
      <div className="risk-band-scale" aria-hidden="true">
        <span style={{ left: 0 }}>0%</span>
        <span style={{ left: `${approve}%` }}>{formatPercent(policy.approveMaxProbability, 0)}</span>
        <span style={{ left: `${reject}%` }}>{formatPercent(policy.rejectMinProbability, 0)}</span>
        <span style={{ left: "100%" }}>100%</span>
      </div>
    </div>
  );
}
