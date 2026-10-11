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
  const hasChanges = Object.keys(request).length > 0;

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (hasChanges) simulate.mutate(request);
  }

  function input(name: string, current: number, step: string) {
    const id = `whatif-${name}`;
    return (
      <label key={name} htmlFor={id}>
        {variableLabel(name)}
        <input
          id={id}
          type="number"
          step={step}
          placeholder={String(current)}
          value={values[name] ?? ""}
          onChange={(e) => setValues((current) => ({ ...current, [name]: e.target.value }))}
        />
      </label>
    );
  }

  return (
    <div className="stack">
      <p className="muted small">
        Escribe solo los valores que quieras cambiar (el valor real aparece de fondo). Una simulación no es una decisión:
        no cambia la solicitud, pero queda registrada para auditoría.
      </p>
      <form className="form" onSubmit={handleSubmit}>
        <div className="grid">
          {TOP_LEVEL.map(({ name, step }) => input(name, application[name], step))}
          {HISTORY.map(({ name, step }) => input(`creditHistory.${name}`, application.creditHistory[name], step))}
        </div>
        <div className="row">
          <button type="submit" disabled={!hasChanges || simulate.isPending}>
            {simulate.isPending ? "Simulando…" : "Simular"}
          </button>
          <button type="button" className="secondary" onClick={() => { setValues({}); simulate.reset(); }}>
            Limpiar
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
  return (
    <section className="whatif-result" aria-label="Resultado de la simulación">
      <p className={result.decisionChanged ? "notice" : "muted"}>
        {result.decisionChanged
          ? `La decisión cambiaría: de "${outcomeText(result.baseline)}" a "${outcomeText(result.scenario)}".`
          : `La decisión no cambiaría: seguiría siendo "${outcomeText(result.baseline)}".`}{" "}
        <span className="small">Modelo {result.modelVersion}.</span>
      </p>
      <div className="compare">
        <ScenarioColumn title="Real" scenario={result.baseline} />
        <ScenarioColumn title="Simulado" scenario={result.scenario} />
      </div>
      <p className="small">
        Probabilidad de default: {formatSigned(result.probabilityOfDefaultDelta * 100, 1)} puntos · Score:{" "}
        {formatSigned(result.riskScoreDelta, 0)}
      </p>
      <h4>Cambios simulados</h4>
      <ul>
        {result.changes.map((change) => (
          <li key={change.variable}>
            {variableLabel(change.variable)}: {formatNumber(change.baselineValue)} → {formatNumber(change.scenarioValue)}
          </li>
        ))}
      </ul>
      {moved.length > 0 && (
        <>
          <h4>Efecto en cada variable del modelo</h4>
          <ShapChart title="Cambio en la contribución al riesgo" bars={moved.map((m) => ({ feature: m.feature, value: m.shapDelta }))} />
          <ul aria-label="Efecto por variable">
            {moved.map((impact) => (
              <li key={impact.feature}>
                {featureLabel(impact.feature)}: {formatNumber(impact.baselineValue)} → {formatNumber(impact.scenarioValue)} (
                {formatSigned(impact.shapDelta, 3)} en log-odds)
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

function ScenarioColumn({ title, scenario }: { title: string; scenario: WhatIfScenario }) {
  return (
    <div className="scenario">
      <h4>{title}</h4>
      <StatusBadge value={scenario.outcome} />
      <dl className="metrics">
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
