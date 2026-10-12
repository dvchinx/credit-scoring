import { ErrorMessage, Loading } from "../../components/ErrorMessage";
import { useDecisions } from "./api";
import { DecisionCard } from "./DecisionCard";

export function DecisionHistory({ applicationId }: { applicationId: string }) {
  const decisions = useDecisions(applicationId);

  if (decisions.isPending) return <Loading label="Cargando decisiones…" />;
  if (decisions.isError) return <ErrorMessage error={decisions.error} />;
  if (decisions.data.length === 0) {
    return (
      <div className="empty">
        <strong>Esta solicitud todavía no se ha evaluado.</strong>
        Pulsa «Evaluar con el modelo activo» para obtener una decisión explicada.
      </div>
    );
  }

  const [latest, ...previous] = decisions.data;
  return (
    <div className="stack">
      <DecisionCard decision={latest} />
      {previous.length > 0 && (
        <details className="history">
          <summary>Decisiones anteriores ({previous.length})</summary>
          <div className="stack">
            {previous.map((decision) => (
              <DecisionCard key={decision.id} decision={decision} />
            ))}
          </div>
        </details>
      )}
    </div>
  );
}
