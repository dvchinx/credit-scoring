import { useQuery } from "@tanstack/react-query";
import type { ModelActivation, ModelVersion } from "../../api/types";
import { ErrorMessage } from "../../components/ErrorMessage";
import { formatDateTime, formatNumber, formatPercent } from "../../components/format";
import { useApi } from "../auth/AuthContext";

export function ModelVersionsPage() {
  const api = useApi();
  const versions = useQuery({ queryKey: ["model-versions"], queryFn: () => api.get<ModelVersion[]>("/model-versions") });
  const activations = useQuery({
    queryKey: ["model-activations"],
    queryFn: () => api.get<ModelActivation[]>("/model-activations"),
  });

  return (
    <div className="stack">
      <section className="panel">
        <h1>Versiones de modelo</h1>
        <p className="muted small">
          Versiones registradas en el model registry. La metadata viene del servicio de ML y es inmutable; el hash
          identifica el artefacto exacto que se sirve.
        </p>
        {versions.isPending && <p className="muted">Cargando…</p>}
        <ErrorMessage error={versions.error} />
        {versions.data && versions.data.length === 0 && <p className="muted">No hay versiones registradas.</p>}
        {versions.data && versions.data.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Versión</th>
                <th>Algoritmo</th>
                <th className="num">ROC-AUC</th>
                <th className="num">Brier</th>
                <th className="num">Filas de entrenamiento</th>
                <th className="num">Tasa de default</th>
                <th>Entrenado</th>
                <th>Artefacto</th>
              </tr>
            </thead>
            <tbody>
              {versions.data.map((version) => (
                <tr key={version.version}>
                  <td>
                    {version.version} {version.active && <span className="badge badge-good">Activa</span>}
                  </td>
                  <td className="small">{version.algorithm}</td>
                  <td className="num">{metric(version, "roc_auc")}</td>
                  <td className="num">{metric(version, "brier_score")}</td>
                  <td className="num">{formatNumber(version.dataset.trainingRows)}</td>
                  <td className="num">{formatPercent(version.dataset.defaultRate)}</td>
                  <td>{formatDateTime(version.trainedAt)}</td>
                  <td>
                    <code title={version.artifactSha256}>{version.artifactSha256.slice(0, 12)}…</code>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <h2>Historial de activaciones</h2>
        <ErrorMessage error={activations.error} />
        {activations.data && activations.data.length === 0 && <p className="muted">Ninguna versión ha sido activada.</p>}
        {activations.data && activations.data.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Versión</th>
                <th>Motivo</th>
                <th>Activada por</th>
              </tr>
            </thead>
            <tbody>
              {activations.data.map((activation) => (
                <tr key={activation.id}>
                  <td>{formatDateTime(activation.activatedAt)}</td>
                  <td>{activation.modelVersion}</td>
                  <td>{activation.reason}</td>
                  <td>{activation.activatedBy}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}

function metric(version: ModelVersion, name: string): string {
  const value = version.metrics[name];
  return value === undefined ? "—" : value.toFixed(3);
}
