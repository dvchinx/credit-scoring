import { ErrorMessage } from "../../components/ErrorMessage";
import { useDecisions } from "./api";
import { DecisionCard } from "./DecisionCard";

export function DecisionHistory({ applicationId }: { applicationId: string }) {
  const decisions = useDecisions(applicationId);

  if (decisions.isPending) return <p className="muted">Cargando decisiones…</p>;
  if (decisions.isError) return <ErrorMessage error={decisions.error} />;
  if (decisions.data.length === 0) {
    return <p className="muted">Esta solicitud todavía no se ha evaluado.</p>;
  }

  const [latest, ...previous] = decisions.data;
  return (
    <div className="stack">
      <DecisionCard decision={latest} />
      {previous.length > 0 && (
        <details>
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
