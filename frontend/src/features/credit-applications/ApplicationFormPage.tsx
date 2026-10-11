import { Link, useNavigate, useParams } from "react-router";
import { ErrorMessage } from "../../components/ErrorMessage";
import { useApplication, useSaveApplication } from "./api";
import { ApplicationForm } from "./ApplicationForm";

export function NewApplicationPage() {
  const navigate = useNavigate();
  const save = useSaveApplication();
  return (
    <section className="panel">
      <h1>Nueva solicitud</h1>
      <ApplicationForm
        submitLabel="Crear solicitud"
        onSubmit={async (request) => {
          const created = await save.mutateAsync(request);
          navigate(`/applications/${created.id}`);
        }}
      />
    </section>
  );
}

export function EditApplicationPage() {
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const application = useApplication(id);
  const save = useSaveApplication(id);

  if (application.isPending) return <p className="muted">Cargando…</p>;
  if (application.isError) return <ErrorMessage error={application.error} />;

  return (
    <section className="panel">
      <p>
        <Link to={`/applications/${id}`}>← Volver a la solicitud</Link>
      </p>
      <h1>Editar solicitud</h1>
      <p className="muted">
        Editar no cambia las decisiones ya tomadas: cada decisión conserva una copia de los datos que se evaluaron.
      </p>
      <ApplicationForm
        initial={application.data}
        submitLabel="Guardar cambios"
        onSubmit={async (request) => {
          await save.mutateAsync(request);
          navigate(`/applications/${id}`);
        }}
      />
    </section>
  );
}
