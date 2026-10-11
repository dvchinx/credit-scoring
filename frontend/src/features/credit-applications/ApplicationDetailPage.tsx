import { Link, useParams } from "react-router";
import { StatusBadge } from "../../components/Badges";
import { ErrorMessage } from "../../components/ErrorMessage";
import { formatDate, formatMoney, formatNumber, formatPercent } from "../../components/format";
import { useEvaluateApplication } from "../decisions/api";
import { DecisionHistory } from "../decisions/DecisionHistory";
import { WhatIfPanel } from "../what-if/WhatIfPanel";
import { useApplication } from "./api";

export function ApplicationDetailPage() {
  const { id = "" } = useParams();
  const application = useApplication(id);
  const evaluate = useEvaluateApplication(id);

  if (application.isPending) return <p className="muted">Cargando…</p>;
  if (application.isError) return <ErrorMessage error={application.error} />;
  const data = application.data;
  const history = data.creditHistory;

  return (
    <div className="stack">
      <p>
        <Link to="/applications">← Solicitudes</Link>
      </p>
      <section className="panel">
        <div className="panel-header">
          <div>
            <h1>{data.applicantFullName}</h1>
            <p className="muted">
              Documento {data.documentId} · creada por {data.createdBy}
            </p>
          </div>
          <StatusBadge value={data.status} />
        </div>
        <dl className="facts">
          <Fact label="Fecha de nacimiento" value={formatDate(data.birthDate)} />
          <Fact label="Ingreso mensual" value={formatMoney(data.monthlyIncome)} />
          <Fact label="Monto solicitado" value={formatMoney(data.requestedAmount)} />
          <Fact label="Plazo" value={`${data.loanTermMonths} meses`} />
          <Fact label="Deuda mensual existente" value={formatMoney(data.existingMonthlyDebt)} />
          <Fact label="Antigüedad laboral" value={`${formatNumber(data.employmentYears)} años`} />
          <Fact label="Dependientes" value={String(data.numberOfDependents)} />
          <Fact label="Utilización de crédito rotativo" value={formatPercent(history.revolvingUtilization, 0)} />
          <Fact label="Líneas abiertas / hipotecas" value={`${history.openCreditLines} / ${history.realEstateLoans}`} />
          <Fact
            label="Moras 30-59 / 60-89 / 90+"
            value={`${history.latePayments30To59Days} / ${history.latePayments60To89Days} / ${history.latePayments90DaysOrMore}`}
          />
        </dl>
        <div className="row">
          <button type="button" onClick={() => evaluate.mutate()} disabled={evaluate.isPending}>
            {evaluate.isPending ? "Evaluando…" : "Evaluar con el modelo activo"}
          </button>
          <Link className="button secondary" to={`/applications/${id}/edit`}>
            Editar
          </Link>
        </div>
        <ErrorMessage error={evaluate.error} />
      </section>

      <section className="panel">
        <h2>Decisiones</h2>
        <DecisionHistory applicationId={id} />
      </section>

      <section className="panel">
        <h2>Simulador "¿qué pasaría si…?"</h2>
        <WhatIfPanel application={data} />
      </section>
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
