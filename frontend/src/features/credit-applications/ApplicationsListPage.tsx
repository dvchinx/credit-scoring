import { useState } from "react";
import { Link } from "react-router";
import { StatusBadge } from "../../components/Badges";
import { ErrorMessage } from "../../components/ErrorMessage";
import { formatDateTime, formatMoney } from "../../components/format";
import { useApplicationsPage } from "./api";

const PAGE_SIZE = 10;

export function ApplicationsListPage() {
  const [page, setPage] = useState(0);
  const query = useApplicationsPage(page, PAGE_SIZE);

  return (
    <section className="panel">
      <div className="panel-header">
        <h1>Solicitudes de crédito</h1>
        <Link className="button" to="/applications/new">
          Nueva solicitud
        </Link>
      </div>
      {query.isPending && <p className="muted">Cargando…</p>}
      <ErrorMessage error={query.error} />
      {query.data && query.data.content.length === 0 && <p className="muted">Todavía no hay solicitudes.</p>}
      {query.data && query.data.content.length > 0 && (
        <>
          <table>
            <thead>
              <tr>
                <th>Solicitante</th>
                <th>Documento</th>
                <th className="num">Monto</th>
                <th className="num">Plazo</th>
                <th>Estado</th>
                <th>Creada</th>
              </tr>
            </thead>
            <tbody>
              {query.data.content.map((application) => (
                <tr key={application.id}>
                  <td>
                    <Link to={`/applications/${application.id}`}>{application.applicantFullName}</Link>
                  </td>
                  <td>{application.documentId}</td>
                  <td className="num">{formatMoney(application.requestedAmount)}</td>
                  <td className="num">{application.loanTermMonths} m</td>
                  <td>
                    <StatusBadge value={application.status} />
                  </td>
                  <td>{formatDateTime(application.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pagination">
            <button type="button" className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Anterior
            </button>
            <span>
              Página {query.data.page.number + 1} de {Math.max(1, query.data.page.totalPages)} ·{" "}
              {query.data.page.totalElements} solicitudes
            </span>
            <button
              type="button"
              className="secondary"
              disabled={page + 1 >= query.data.page.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Siguiente
            </button>
          </div>
        </>
      )}
    </section>
  );
}
