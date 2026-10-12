import { type FormEvent, useState } from "react";
import type { CreditApplication, WhatIfRequest, WhatIfResponse, WhatIfScenario } from "../../api/types";
import { StatusBadge } from "../../components/Badges";
import { ErrorMessage } from "../../components/ErrorMessage";
import { formatNumber, formatPercent, formatSigned } from "../../components/format";
import { featureLabel, variableLabel } from "../decisions/featureLabels";
import { ShapChart } from "../decisions/ShapChart";
import { useSimulateWhatIf } from "./api";

type TopLevelVariable = "monthlyIncome" | "requestedAmount" | "loanTermMonths" | "existingMonthlyDebt" | "numberOfDependents";
type HistoryVariable = keyof CreditApplication["creditHistory"];

const TOP_LEVEL: { name: TopLevelVariable; step: string }[] = [
  { name: "monthlyIncome", step: "0.01" },
  { name: "requestedAmount", step: "0.01" },
  { name: "loanTermMonths", step: "1" },
  { name: "existingMonthlyDebt", step: "0.01" },
  { name: "numberOfDependents", step: "1" },
];

const HISTORY: { name: HistoryVariable; step: string }[] = [
  { name: "revolvingUtilization", step: "0.01" },
  { name: "openCreditLines", step: "1" },
  { name: "realEstateLoans", step: "1" },
  { name: "latePayments30To59Days", step: "1" },
  { name: "latePayments60To89Days", step: "1" },
  { name: "latePayments90DaysOrMore", step: "1" },
];

/**
 * Solo se envían las variables que el usuario cambió; el backend evalúa ambos escenarios con el
 * modelo activo y la política vigente. Aquí no se calcula ninguna decisión.
 */
export function buildWhatIfRequest(application: CreditApplication, values: Record<string, string>): WhatIfRequest {
  const request: WhatIfRequest = {};
  const changed = (name: string, current: number) => {
    const raw = values[name]?.trim();
    if (!raw) return undefined;
    const value = Number(raw);
    return Number.isNaN(value) || value === current ? undefined : value;
  };
  for (const { name } of TOP_LEVEL) {
    const value = changed(name, application[name]);
    if (value !== undefined) request[name] = value;
  }
  for (const { name } of HISTORY) {
    const value = changed(`creditHistory.${name}`, application.creditHistory[name]);
    if (value !== undefined) request.creditHistory = { ...request.creditHistory, [name]: value };
  }
  return request;
}

export function WhatIfPanel({ application }: { application: CreditApplication }) {
  const [values, setValues] = useState<Record<string, string>>({});
  const simulate = useSimulateWhatIf(application.id);
  const request = buildWhatIfRequest(application, values);
  const changeCount = Object.keys(request).filter((key) => key !== "creditHistory").length
    + Object.keys(request.creditHistory ?? {}).length;

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (changeCount > 0) simulate.mutate(request);
  }

  function input(name: string, current: number, step: string) {
    const id = `whatif-${name}`;
    const raw = values[name]?.trim();
    const changed = !!raw && Number(raw) !== current && !Number.isNaN(Number(raw));
    return (
      <label key={name} htmlFor={id}>
        {variableLabel(name)}
        <input
          id={id}
          type="number"
          step={step}
          placeholder={String(current)}
          className={changed ? "changed" : undefined}
          value={values[name] ?? ""}
          onChange={(e) => setValues((current) => ({ ...current, [name]: e.target.value }))}
        />
      </label>
    );
  }

  return (
    <div className="stack">
      <form className="form" onSubmit={handleSubmit}>
        <div className="whatif-groups">
          <div className="whatif-group">
            <p className="eyebrow">Solicitante y crédito</p>
            <div className="grid">{TOP_LEVEL.map(({ name, step }) => input(name, application[name], step))}</div>
          </div>
          <div className="whatif-group">
            <p className="eyebrow">Historial crediticio</p>
            <div className="grid">
              {HISTORY.map(({ name, step }) => input(`creditHistory.${name}`, application.creditHistory[name], step))}
            </div>
          </div>
        </div>
        <div className="row">
          <span className="muted small">
            {changeCount === 0
              ? "Sin cambios todavía."
              : `${changeCount} ${changeCount === 1 ? "variable cambiada" : "variables cambiadas"}.`}
          </span>
          <span className="spacer" />
          <button
            type="button"
            className="secondary"
            onClick={() => {
              setValues({});
              simulate.reset();
            }}
          >
            Limpiar
          </button>
          <button type="submit" disabled={changeCount === 0 || simulate.isPending}>
            {simulate.isPending ? "Simulando…" : "Simular"}
          </button>
        </div>
      </form>
      <ErrorMessage error={simulate.error} />
      {simulate.data && <WhatIfResult result={simulate.data} />}
    </div>
  );
}

function WhatIfResult({ result }: { result: WhatIfResponse }) {
  const moved = result.featureImpacts.filter((impact) => Math.abs(impact.shapDelta) > 1e-9);
  const pdDelta = result.probabilityOfDefaultDelta;
  return (
    <section className="whatif-result" aria-label="Resultado de la simulación">
      <div className={`banner ${result.decisionChanged ? "banner-good" : "banner-neutral"}`}>
        <div>
          <strong>
            {result.decisionChanged
              ? `La decisión cambiaría: de "${outcomeText(result.baseline)}" a "${outcomeText(result.scenario)}".`
              : `La decisión no cambiaría: seguiría siendo "${outcomeText(result.baseline)}".`}
          </strong>
          <span className="muted small">Ambos escenarios evaluados con el modelo {result.modelVersion} y la política vigente.</span>
        </div>
      </div>

      <div className="compare">
        <ScenarioColumn title="Real" scenario={result.baseline} />
        <div className="compare-arrow" aria-hidden="true">
          →
        </div>
        <ScenarioColumn title="Simulado" scenario={result.scenario} simulated />
      </div>

      <div className="deltas">
        <span className="muted">Diferencia:</span>
        <span className={`chip ${pdDelta < 0 ? "chip-good" : pdDelta > 0 ? "chip-bad" : ""}`}>
          Probabilidad de default {formatSigned(pdDelta * 100, 1)} puntos
        </span>
        <span className={`chip ${result.riskScoreDelta > 0 ? "chip-good" : result.riskScoreDelta < 0 ? "chip-bad" : ""}`}>
          Score {formatSigned(result.riskScoreDelta, 0)}
        </span>
      </div>

      <div>
        <h4>Cambios simulados</h4>
        <ul className="changes">
          {result.changes.map((change) => (
            <li key={change.variable}>
              {variableLabel(change.variable)}: {formatNumber(change.baselineValue)} → {formatNumber(change.scenarioValue)}
            </li>
          ))}
        </ul>
      </div>

      {moved.length > 0 && (
        <div>
          <h4>Efecto en cada variable del modelo</h4>
          <p className="muted small">Cuánto cambia la contribución de cada variable al riesgo entre el escenario real y el simulado.</p>
          <ShapChart
            title="Cambio en la contribución al riesgo"
            bars={moved.map((m) => ({ feature: m.feature, value: m.shapDelta }))}
          />
          <ul className="impacts" aria-label="Efecto por variable">
            {moved.map((impact) => (
              <li key={impact.feature}>
                {featureLabel(impact.feature)}: {formatNumber(impact.baselineValue)} → {formatNumber(impact.scenarioValue)} (
                {formatSigned(impact.shapDelta, 3)} en log-odds)
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function ScenarioColumn({ title, scenario, simulated }: { title: string; scenario: WhatIfScenario; simulated?: boolean }) {
  return (
    <div className={`scenario${simulated ? " scenario-simulated" : ""}`}>
      <div className="scenario-head">
        <span className="eyebrow">{title}</span>
        <StatusBadge value={scenario.outcome} />
      </div>
      <dl className="scenario-figures">
        <div>
          <dt>Probabilidad de default</dt>
          <dd>{formatPercent(scenario.probabilityOfDefault)}</dd>
        </div>
        <div>
          <dt>Score</dt>
          <dd>{scenario.riskScore}</dd>
        </div>
      </dl>
      <ul className="small">
        {scenario.reasons.map((reason) => (
          <li key={reason}>{reason}</li>
        ))}
      </ul>
    </div>
  );
}

function outcomeText(scenario: WhatIfScenario): string {
  return { APPROVED: "Aprobada", REJECTED: "Rechazada", MANUAL_REVIEW: "Revisión manual" }[scenario.outcome];
}
