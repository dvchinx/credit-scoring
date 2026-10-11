import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { AuditSummary } from "../../api/types";
import { ErrorMessage } from "../../components/ErrorMessage";
import { formatDateTime } from "../../components/format";
import { useApi } from "../auth/AuthContext";

const COLORS = { approved: "var(--good)", rejected: "var(--bad)", manualReview: "var(--warn)" };

export function AuditDashboardPage() {
  const api = useApi();
  const summary = useQuery({ queryKey: ["audit-summary"], queryFn: () => api.get<AuditSummary>("/audit/summary") });

  if (summary.isPending) return <p className="muted">Cargando…</p>;
  if (summary.isError) return <ErrorMessage error={summary.error} />;
  const data = summary.data;

  return (
    <div className="stack">
      <section className="panel">
        <h1>Auditoría de decisiones</h1>
        <div className="kpis">
          <Kpi label="Decisiones completadas" value={data.completedDecisions} />
          <Kpi label="Aprobadas" value={data.decisionsByOutcome.APPROVED} />
          <Kpi label="Rechazadas" value={data.decisionsByOutcome.REJECTED} />
          <Kpi label="Revisión manual" value={data.decisionsByOutcome.MANUAL_REVIEW} />
          <Kpi label="Intentos fallidos" value={data.failedDecisions} />
          <Kpi
            label="Simulaciones que cambiaron el resultado"
            value={`${data.simulations.outcomeChanged} de ${data.simulations.total}`}
          />
        </div>
      </section>

      <section className="panel">
        <h2>Decisiones por versión de modelo</h2>
        {data.decisionsByModelVersion.length === 0 ? (
          <p className="muted">Todavía no hay decisiones.</p>
        ) : (
          <>
            <figure className="chart" aria-label="Decisiones por versión de modelo">
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={data.decisionsByModelVersion}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="modelVersion" tick={{ fontSize: 12 }} />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="approved" name="Aprobadas" stackId="d" fill={COLORS.approved} isAnimationActive={false} />
                  <Bar dataKey="manualReview" name="Revisión manual" stackId="d" fill={COLORS.manualReview} isAnimationActive={false} />
                  <Bar dataKey="rejected" name="Rechazadas" stackId="d" fill={COLORS.rejected} isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </figure>
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
                    <td>{usage.modelVersion}</td>
                    <td className="num">{usage.approved}</td>
                    <td className="num">{usage.manualReview}</td>
                    <td className="num">{usage.rejected}</td>
                    <td className="num">{usage.total}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </section>

      <section className="panel">
        <h2>Intentos fallidos recientes</h2>
        {data.recentFailures.length === 0 ? (
          <p className="muted">No hay intentos fallidos.</p>
        ) : (
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
                  <td>{formatDateTime(failure.decidedAt)}</td>
                  <td>
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
        )}
      </section>
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="kpi">
      <span className="kpi-value">{value}</span>
      <span className="kpi-label">{label}</span>
    </div>
  );
}
