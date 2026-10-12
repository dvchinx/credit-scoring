import { Link, useNavigate, useParams } from "react-router";
import { ErrorMessage, Loading } from "../../components/ErrorMessage";
import { PageHeader } from "../../components/PageHeader";
import { useApplication, useSaveApplication } from "./api";
import { ApplicationForm } from "./ApplicationForm";

export function NewApplicationPage() {
  const navigate = useNavigate();
  const save = useSaveApplication();
  return (
    <>
      <PageHeader
        back={{ to: "/applications", label: "Solicitudes" }}
        title="Nueva solicitud"
        subtitle="Registra la solicitud; después podrás evaluarla con el modelo activo."
      />
      <ApplicationForm
        submitLabel="Crear solicitud"
        cancel={
          <Link className="button secondary" to="/applications">
            Cancelar
          </Link>
        }
        onSubmit={async (request) => {
          const created = await save.mutateAsync(request);
          navigate(`/applications/${created.id}`);
        }}
      />
    </>
  );
}

export function EditApplicationPage() {
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const application = useApplication(id);
  const save = useSaveApplication(id);

  if (application.isPending) return <Loading />;
  if (application.isError) return <ErrorMessage error={application.error} />;

  return (
    <>
      <PageHeader
        back={{ to: `/applications/${id}`, label: "Volver a la solicitud" }}
        title="Editar solicitud"
        subtitle="Editar no cambia las decisiones ya tomadas: cada decisión conserva una copia de los datos que se evaluaron."
      />
      <ApplicationForm
        initial={application.data}
        submitLabel="Guardar cambios"
        cancel={
          <Link className="button secondary" to={`/applications/${id}`}>
            Cancelar
          </Link>
        }
        onSubmit={async (request) => {
          await save.mutateAsync(request);
          navigate(`/applications/${id}`);
        }}
      />
    </>
  );
}
