import { useState } from "react";
import { Link } from "react-router";
import { StatusBadge } from "../../components/Badges";
import { initials } from "../../components/Brand";
import { ErrorMessage, Loading } from "../../components/ErrorMessage";
import { formatDateTime, formatMoney } from "../../components/format";
import { PageHeader } from "../../components/PageHeader";
import { useApplicationsPage } from "./api";

const PAGE_SIZE = 10;

export function ApplicationsListPage() {
  const [page, setPage] = useState(0);
  const query = useApplicationsPage(page, PAGE_SIZE);
  const total = query.data?.page.totalElements;

  return (
    <>
      <PageHeader
        title="Solicitudes de crédito"
        subtitle={
          total === undefined
            ? "Solicitudes registradas y su estado actual."
            : `${total} ${total === 1 ? "solicitud registrada" : "solicitudes registradas"}. Abre una para evaluarla, ver su explicación o simular cambios.`
        }
        actions={
          <Link className="button" to="/applications/new">
            + Nueva solicitud
          </Link>
        }
      />
      <section className="panel panel-flush">
        {query.isPending && (
          <div className="panel-header">
            <Loading />
          </div>
        )}
        {query.error && (
          <div className="panel-header">
            <ErrorMessage error={query.error} />
          </div>
        )}
        {query.data && query.data.content.length === 0 && (
          <div className="empty">
            <strong>Todavía no hay solicitudes.</strong>
            Crea la primera o ejecuta <code>./scripts/demo.sh</code> para cargar ejemplos.
          </div>
        )}
        {query.data && query.data.content.length > 0 && (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Solicitante</th>
                    <th className="num">Monto</th>
                    <th className="num hide-sm">Plazo</th>
                    <th>Estado</th>
                    <th className="hide-sm">Creada</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data.content.map((application) => (
                    <tr key={application.id} className="clickable">
                      <td>
                        <div className="cell-person">
                          <span className="avatar hide-sm" aria-hidden="true">
                            {initials(application.applicantFullName)}
                          </span>
                          <span>
                            <Link to={`/applications/${application.id}`}>{application.applicantFullName}</Link>
                            <span className="sub">{application.documentId}</span>
                          </span>
                        </div>
                      </td>
                      <td className="num">{formatMoney(application.requestedAmount)}</td>
                      <td className="num hide-sm">{application.loanTermMonths} meses</td>
                      <td>
                        <StatusBadge value={application.status} />
                      </td>
                      <td className="muted hide-sm">{formatDateTime(application.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="pagination">
              <span className="muted">
                Página {query.data.page.number + 1} de {Math.max(1, query.data.page.totalPages)}
              </span>
              <div className="row">
                <button type="button" className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
                  Anterior
                </button>
                <button
                  type="button"
                  className="secondary"
                  disabled={page + 1 >= query.data.page.totalPages}
                  onClick={() => setPage(page + 1)}
                >
                  Siguiente
                </button>
              </div>
            </div>
          </>
        )}
      </section>
    </>
  );
}
