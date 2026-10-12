import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { AuditSummary } from "../../api/types";
import { ErrorMessage, Loading } from "../../components/ErrorMessage";
import { formatDateTime, formatPercent } from "../../components/format";
import { PageHeader } from "../../components/PageHeader";
import { useApi } from "../auth/AuthContext";

const COLORS = { approved: "var(--good)", rejected: "var(--bad)", manualReview: "var(--warn)" };
const TICK = { fill: "var(--muted)", fontSize: 12 };

export function AuditDashboardPage() {
  const api = useApi();
  const summary = useQuery({ queryKey: ["audit-summary"], queryFn: () => api.get<AuditSummary>("/audit/summary") });

  if (summary.isPending) return <Loading />;
  if (summary.isError) return <ErrorMessage error={summary.error} />;
  const data = summary.data;
  const completed = data.completedDecisions;
  const share = (count: number) => (completed > 0 ? formatPercent(count / completed, 0) : "—");
  const outcomes = [
    { label: "Aprobadas", count: data.decisionsByOutcome.APPROVED, color: COLORS.approved },
    { label: "Revisión manual", count: data.decisionsByOutcome.MANUAL_REVIEW, color: COLORS.manualReview },
    { label: "Rechazadas", count: data.decisionsByOutcome.REJECTED, color: COLORS.rejected },
  ];
  const simulations = data.simulations;

  return (
    <div className="stack">
      <PageHeader
        title="Auditoría de decisiones"
        subtitle="Qué decidió el sistema, con qué versión de modelo y cuántas evaluaciones fallaron."
      />

      <div className="kpis">
        <Kpi label="Decisiones completadas" value={completed} />
        <Kpi label="Aprobadas" value={data.decisionsByOutcome.APPROVED} hint={share(data.decisionsByOutcome.APPROVED)} tone="good" />
        <Kpi label="Rechazadas" value={data.decisionsByOutcome.REJECTED} hint={share(data.decisionsByOutcome.REJECTED)} tone="bad" />
        <Kpi
          label="Revisión manual"
          value={data.decisionsByOutcome.MANUAL_REVIEW}
          hint={share(data.decisionsByOutcome.MANUAL_REVIEW)}
          tone="warn"
        />
        <Kpi label="Intentos fallidos" value={data.failedDecisions} tone={data.failedDecisions > 0 ? "bad" : "neutral"} />
        <Kpi
          label="Simulaciones que cambiaron el resultado"
          value={`${simulations.outcomeChanged} de ${simulations.total}`}
          tone="neutral"
        />
      </div>

      <div className="dashboard-grid">
        <section className="panel">
          <h2>Distribución de resultados</h2>
          <p className="muted small">Sobre las decisiones completadas.</p>
          <div className="distribution" role="img" aria-label="Distribución de resultados">
            {completed > 0 &&
              outcomes.map((o) => (
                <span key={o.label} style={{ width: `${(o.count / completed) * 100}%`, background: o.color }} />
              ))}
          </div>
          <div className="legend">
            {outcomes.map((o) => (
              <span key={o.label}>
                <i style={{ background: o.color }} />
                {o.label} · {o.count} ({share(o.count)})
              </span>
            ))}
          </div>
        </section>
        <section className="panel">
          <h2>Simulaciones what-if</h2>
          <p className="muted small">Un volumen alto de simulaciones que cambian el resultado puede indicar intentos de "gaming".</p>
          <div className="distribution" role="img" aria-label="Simulaciones que cambiaron el resultado">
            {simulations.total > 0 && (
              <span
                style={{ width: `${(simulations.outcomeChanged / simulations.total) * 100}%`, background: "var(--accent)" }}
              />
            )}
          </div>
          <div className="legend">
            <span>
              <i style={{ background: "var(--accent)" }} />
              Cambiaron el resultado · {simulations.outcomeChanged}
            </span>
            <span>
              <i style={{ background: "var(--neutral-soft)" }} />
              Sin cambio · {simulations.total - simulations.outcomeChanged}
            </span>
          </div>
        </section>
      </div>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Decisiones por versión de modelo</h2>
            <p className="muted small">Cada decisión registra la versión exacta del modelo que la produjo.</p>
          </div>
        </div>
        {data.decisionsByModelVersion.length === 0 ? (
          <div className="empty">
            <strong>Todavía no hay decisiones.</strong>
          </div>
        ) : (
          <>
            <figure className="chart" aria-label="Decisiones por versión de modelo">
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={data.decisionsByModelVersion} barCategoryGap="30%" maxBarSize={72}>
                  <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="modelVersion" tick={TICK} axisLine={false} tickLine={false} />
                  <YAxis allowDecimals={false} tick={TICK} axisLine={false} tickLine={false} width={32} />
                  <Tooltip
                    cursor={{ fill: "var(--surface-2)" }}
                    contentStyle={{
                      background: "var(--surface)",
                      border: "1px solid var(--border)",
                      borderRadius: 8,
                      color: "var(--text)",
                    }}
                  />
                  <Bar dataKey="approved" name="Aprobadas" stackId="d" fill={COLORS.approved} isAnimationActive={false} />
                  <Bar dataKey="manualReview" name="Revisión manual" stackId="d" fill={COLORS.manualReview} isAnimationActive={false} />
                  <Bar
                    dataKey="rejected"
                    name="Rechazadas"
                    stackId="d"
                    fill={COLORS.rejected}
                    radius={[6, 6, 0, 0]}
                    isAnimationActive={false}
                  />
                </BarChart>
              </ResponsiveContainer>
            </figure>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Versión</th>
                    <th className="num">Aprobadas</th>
                    <th className="num">Revisión manual</th>
                    <th className="num">Rechazadas</th>
                    <th className="num">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {data.decisionsByModelVersion.map((usage) => (
                    <tr key={usage.modelVersion}>
                      <td className="mono">{usage.modelVersion}</td>
                      <td className="num">{usage.approved}</td>
                      <td className="num">{usage.manualReview}</td>
                      <td className="num">{usage.rejected}</td>
                      <td className="num">
                        <strong>{usage.total}</strong>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Intentos fallidos recientes</h2>
            <p className="muted small">Evaluaciones en las que el servicio de ML no pudo puntuar o explicar.</p>
          </div>
        </div>
        {data.recentFailures.length === 0 ? (
          <div className="empty">
            <strong>No hay intentos fallidos.</strong>
            Todas las evaluaciones terminaron con una decisión explicada.
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Solicitud</th>
                  <th>Motivo</th>
                  <th>Usuario</th>
                </tr>
              </thead>
              <tbody>
                {data.recentFailures.map((failure) => (
                  <tr key={failure.decisionId}>
                    <td className="muted">{formatDateTime(failure.decidedAt)}</td>
                    <td className="mono">
                      <Link to={`/applications/${failure.creditApplicationId}`}>
                        {failure.creditApplicationId.slice(0, 8)}…
                      </Link>
                    </td>
                    <td>{failure.failureReason}</td>
                    <td>{failure.decidedBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function Kpi({
  label,
  value,
  hint,
  tone,
}: {
  label: string;
  value: number | string;
  hint?: string;
  tone?: "good" | "bad" | "warn" | "neutral";
}) {
  return (
    <div className={`kpi${tone ? ` kpi-${tone}` : ""}`}>
      <span className="kpi-label">{label}</span>
      <span className="kpi-value">{value}</span>
      {hint && <span className="kpi-hint">{hint} del total</span>}
    </div>
  );
}
