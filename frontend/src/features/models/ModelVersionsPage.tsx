import { useQuery } from "@tanstack/react-query";
import type { ModelActivation, ModelVersion } from "../../api/types";
import { ErrorMessage, Loading } from "../../components/ErrorMessage";
import { formatDateTime, formatNumber, formatPercent } from "../../components/format";
import { PageHeader } from "../../components/PageHeader";
import { useApi } from "../auth/AuthContext";

export function ModelVersionsPage() {
  const api = useApi();
  const versions = useQuery({ queryKey: ["model-versions"], queryFn: () => api.get<ModelVersion[]>("/model-versions") });
  const activations = useQuery({
    queryKey: ["model-activations"],
    queryFn: () => api.get<ModelActivation[]>("/model-activations"),
  });
  const active = versions.data?.find((version) => version.active);

  return (
    <div className="stack">
      <PageHeader
        title="Versiones de modelo"
        subtitle="Versiones registradas en el model registry. La metadata viene del servicio de ML y es inmutable; el hash identifica el artefacto exacto que se sirve."
      />

      {versions.isPending && <Loading />}
      <ErrorMessage error={versions.error} />
      {active && <ActiveModel version={active} />}
      {versions.data && !active && versions.data.length > 0 && (
        <p className="notice">Hay versiones registradas pero ninguna activa: las evaluaciones responderán 409.</p>
      )}

      <section className="panel panel-flush">
        <div className="panel-header">
          <div>
            <h2>Todas las versiones</h2>
            <p className="muted small">Cada entrenamiento genera una versión nueva; ninguna se sobrescribe.</p>
          </div>
        </div>
        {versions.data && versions.data.length === 0 && (
          <div className="empty">
            <strong>No hay versiones registradas.</strong>
          </div>
        )}
        {versions.data && versions.data.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Versión</th>
                  <th className="num">ROC-AUC</th>
                  <th className="num">Brier</th>
                  <th className="num">Filas</th>
                  <th className="num">Default</th>
                  <th>Entrenado</th>
                  <th>Artefacto</th>
                </tr>
              </thead>
              <tbody>
                {versions.data.map((version) => (
                  <tr key={version.version}>
                    <td>
                      <span className="row" style={{ gap: "0.5rem" }}>
                        <span className="mono">{version.version}</span>
                        {version.active && <span className="badge badge-good">Activa</span>}
                      </span>
                    </td>
                    <td className="num">{metric(version, "roc_auc")}</td>
                    <td className="num">{metric(version, "brier_score")}</td>
                    <td className="num">{formatNumber(version.dataset.trainingRows)}</td>
                    <td className="num">{formatPercent(version.dataset.defaultRate)}</td>
                    <td className="muted">{formatDateTime(version.trainedAt)}</td>
                    <td>
                      <code title={version.artifactSha256}>{version.artifactSha256.slice(0, 12)}…</code>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Historial de activaciones</h2>
            <p className="muted small">Activar es un acto auditado con motivo; el rollback es volver a activar una versión anterior.</p>
          </div>
        </div>
        <ErrorMessage error={activations.error} />
        {activations.data && activations.data.length === 0 && (
          <div className="empty">
            <strong>Ninguna versión ha sido activada.</strong>
          </div>
        )}
        {activations.data && activations.data.length > 0 && (
          <ol className="timeline">
            {activations.data.map((activation) => (
              <li key={activation.id}>
                <div className="row" style={{ gap: "0.5rem" }}>
                  <strong className="mono">{activation.modelVersion}</strong>
                  <span className="muted small">
                    {formatDateTime(activation.activatedAt)} · {activation.activatedBy}
                  </span>
                </div>
                <p className="small">{activation.reason}</p>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  );
}

function ActiveModel({ version }: { version: ModelVersion }) {
  return (
    <section className="panel model-hero" aria-label="Modelo activo">
      <div>
        <p className="eyebrow">Modelo activo</p>
        <h2>{version.version}</h2>
        <p className="muted small">
          {version.algorithm} · entrenado {formatDateTime(version.trainedAt)} con <code>{version.dataset.source}</code>
        </p>
      </div>
      <span className="badge badge-good badge-lg">En producción</span>
      <dl className="metrics">
        <div className="metric">
          <dt>ROC-AUC</dt>
          <dd>{metric(version, "roc_auc")}</dd>
        </div>
        <div className="metric">
          <dt>Brier score</dt>
          <dd>{metric(version, "brier_score")}</dd>
        </div>
        <div className="metric">
          <dt>Filas de entrenamiento</dt>
          <dd>{formatNumber(version.dataset.trainingRows)}</dd>
        </div>
        <div className="metric">
          <dt>Tasa de default</dt>
          <dd>{formatPercent(version.dataset.defaultRate)}</dd>
        </div>
        <div className="metric metric-wide">
          <dt>SHA-256 del artefacto</dt>
          <dd className="metric-text hash">{version.artifactSha256}</dd>
        </div>
      </dl>
    </section>
  );
}

function metric(version: ModelVersion, name: string): string {
  const value = version.metrics[name];
  return value === undefined ? "—" : value.toFixed(3);
}
