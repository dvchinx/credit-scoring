import type { ReactNode } from "react";
import { Link, useParams } from "react-router";
import { StatusBadge } from "../../components/Badges";
import { ErrorMessage, Loading } from "../../components/ErrorMessage";
import { formatDate, formatMoney, formatNumber, formatPercent } from "../../components/format";
import { PageHeader } from "../../components/PageHeader";
import { useEvaluateApplication } from "../decisions/api";
import { DecisionHistory } from "../decisions/DecisionHistory";
import { WhatIfPanel } from "../what-if/WhatIfPanel";
import { useApplication } from "./api";

export function ApplicationDetailPage() {
  const { id = "" } = useParams();
  const application = useApplication(id);
  const evaluate = useEvaluateApplication(id);

  if (application.isPending) return <Loading />;
  if (application.isError) return <ErrorMessage error={application.error} />;
  const data = application.data;
  const history = data.creditHistory;

  return (
    <div className="stack">
      <PageHeader
        back={{ to: "/applications", label: "Solicitudes" }}
        title={data.applicantFullName}
        badge={<StatusBadge value={data.status} size="lg" />}
        subtitle={
          <>
            Documento {data.documentId} · creada por {data.createdBy}
          </>
        }
        actions={
          <>
            <Link className="button secondary" to={`/applications/${id}/edit`}>
              Editar
            </Link>
            <button type="button" onClick={() => evaluate.mutate()} disabled={evaluate.isPending}>
              {evaluate.isPending ? "Evaluando…" : "Evaluar con el modelo activo"}
            </button>
          </>
        }
      />
      <ErrorMessage error={evaluate.error} />

      <section className="facts-groups" aria-label="Datos de la solicitud">
        <FactGroup title="Solicitante">
          <Fact label="Fecha de nacimiento" value={formatDate(data.birthDate)} />
          <Fact label="Ingreso mensual" value={formatMoney(data.monthlyIncome)} />
          <Fact label="Antigüedad laboral" value={`${formatNumber(data.employmentYears)} años`} />
          <Fact label="Dependientes" value={String(data.numberOfDependents)} />
        </FactGroup>
        <FactGroup title="Crédito solicitado">
          <Fact label="Monto solicitado" value={formatMoney(data.requestedAmount)} />
          <Fact label="Plazo" value={`${data.loanTermMonths} meses`} />
          <Fact label="Deuda mensual existente" value={formatMoney(data.existingMonthlyDebt)} />
        </FactGroup>
        <FactGroup title="Historial crediticio">
          <Fact label="Utilización de crédito rotativo" value={formatPercent(history.revolvingUtilization, 0)} />
          <Fact label="Líneas abiertas / hipotecas" value={`${history.openCreditLines} / ${history.realEstateLoans}`} />
          <Fact
            label="Moras 30-59 / 60-89 / 90+"
            value={`${history.latePayments30To59Days} / ${history.latePayments60To89Days} / ${history.latePayments90DaysOrMore}`}
          />
        </FactGroup>
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Decisiones</h2>
            <p className="muted small">
              Cada evaluación guarda la versión del modelo, la política y la explicación. Las anteriores no se modifican.
            </p>
          </div>
        </div>
        <DecisionHistory applicationId={id} />
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Simulador "¿qué pasaría si…?"</h2>
            <p className="muted small">
              Escribe solo los valores que quieras cambiar; el valor real aparece de fondo. Una simulación no es una
              decisión: no cambia la solicitud, pero queda registrada para auditoría.
            </p>
          </div>
        </div>
        <WhatIfPanel application={data} />
      </section>
    </div>
  );
}

function FactGroup({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="facts-group">
      <p className="eyebrow">{title}</p>
      <dl className="facts">{children}</dl>
    </div>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
