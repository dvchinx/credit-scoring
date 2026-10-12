import { type FormEvent, type ReactNode, useState } from "react";
import { ApiError } from "../../api/client";
import type { CreditApplicationRequest } from "../../api/types";

interface FieldSpec {
  name: string;
  label: string;
  type: "text" | "date" | "number";
  step?: string;
}

const APPLICANT_FIELDS: FieldSpec[] = [
  { name: "applicantFullName", label: "Nombre completo", type: "text" },
  { name: "documentId", label: "Documento de identidad", type: "text" },
  { name: "birthDate", label: "Fecha de nacimiento", type: "date" },
  { name: "monthlyIncome", label: "Ingreso mensual", type: "number", step: "0.01" },
  { name: "employmentYears", label: "Antigüedad laboral (años)", type: "number", step: "0.1" },
  { name: "numberOfDependents", label: "Dependientes", type: "number", step: "1" },
];

const LOAN_FIELDS: FieldSpec[] = [
  { name: "requestedAmount", label: "Monto solicitado", type: "number", step: "0.01" },
  { name: "loanTermMonths", label: "Plazo (meses)", type: "number", step: "1" },
  { name: "existingMonthlyDebt", label: "Deuda mensual existente", type: "number", step: "0.01" },
];

const HISTORY_FIELDS: FieldSpec[] = [
  { name: "creditHistory.revolvingUtilization", label: "Utilización de crédito rotativo (0.30 = 30%)", type: "number", step: "0.01" },
  { name: "creditHistory.openCreditLines", label: "Líneas de crédito abiertas", type: "number", step: "1" },
  { name: "creditHistory.realEstateLoans", label: "Créditos hipotecarios", type: "number", step: "1" },
  { name: "creditHistory.latePayments30To59Days", label: "Moras de 30-59 días", type: "number", step: "1" },
  { name: "creditHistory.latePayments60To89Days", label: "Moras de 60-89 días", type: "number", step: "1" },
  { name: "creditHistory.latePayments90DaysOrMore", label: "Moras de 90+ días", type: "number", step: "1" },
];

const NUMERIC = new Set(
  [...APPLICANT_FIELDS, ...LOAN_FIELDS, ...HISTORY_FIELDS].filter((f) => f.type === "number").map((f) => f.name),
);

type Values = Record<string, string>;

function toValues(request?: CreditApplicationRequest): Values {
  if (!request) return {};
  const { creditHistory, ...rest } = request;
  const values: Values = {};
  for (const [key, value] of Object.entries(rest)) values[key] = String(value);
  for (const [key, value] of Object.entries(creditHistory)) values[`creditHistory.${key}`] = String(value);
  return values;
}

/** Los campos vacíos se envían como null para que la validación del backend los reporte. */
function toRequest(values: Values): CreditApplicationRequest {
  const read = (name: string) => {
    const raw = values[name]?.trim() ?? "";
    if (raw === "") return null;
    return NUMERIC.has(name) ? Number(raw) : raw;
  };
  const history: Record<string, unknown> = {};
  for (const field of HISTORY_FIELDS) history[field.name.replace("creditHistory.", "")] = read(field.name);
  const request: Record<string, unknown> = { creditHistory: history };
  for (const field of [...APPLICANT_FIELDS, ...LOAN_FIELDS]) request[field.name] = read(field.name);
  return request as unknown as CreditApplicationRequest;
}

export function ApplicationForm({
  initial,
  submitLabel,
  cancel,
  onSubmit,
}: {
  initial?: CreditApplicationRequest;
  submitLabel: string;
  cancel?: ReactNode;
  onSubmit: (request: CreditApplicationRequest) => Promise<unknown>;
}) {
  const [values, setValues] = useState<Values>(() => toValues(initial));
  const [error, setError] = useState<ApiError | Error | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const fieldErrors = error instanceof ApiError ? error.fieldErrors : {};

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onSubmit(toRequest(values));
    } catch (e) {
      setError(e instanceof Error ? e : new Error("Error inesperado"));
    } finally {
      setSubmitting(false);
    }
  }

  function renderField(field: FieldSpec) {
    const id = `field-${field.name}`;
    const fieldError = fieldErrors[field.name];
    return (
      <label key={field.name} htmlFor={id}>
        {field.label}
        <input
          id={id}
          name={field.name}
          type={field.type}
          step={field.step}
          value={values[field.name] ?? ""}
          onChange={(e) => setValues((current) => ({ ...current, [field.name]: e.target.value }))}
          aria-invalid={fieldError ? true : undefined}
          aria-describedby={fieldError ? `${id}-error` : undefined}
        />
        {fieldError && (
          <span id={`${id}-error`} className="field-error">
            {fieldError}
          </span>
        )}
      </label>
    );
  }

  function section(id: string, title: string, description: string, fields: FieldSpec[]) {
    return (
      <section className="form-section">
        <div className="form-section-intro">
          <h2 id={id}>{title}</h2>
          <p>{description}</p>
        </div>
        <fieldset aria-labelledby={id}>
          <div className="grid">{fields.map(renderField)}</div>
        </fieldset>
      </section>
    );
  }

  return (
    <form className="form panel" onSubmit={handleSubmit} noValidate>
      {section("section-applicant", "Solicitante", "Datos personales e ingresos de quien pide el crédito.", APPLICANT_FIELDS)}
      {section(
        "section-loan",
        "Crédito solicitado",
        "La cuota nueva se suma a la deuda existente para calcular el endeudamiento.",
        LOAN_FIELDS,
      )}
      {section(
        "section-history",
        "Historial crediticio",
        "Comportamiento de pago reportado por los burós de crédito.",
        HISTORY_FIELDS,
      )}
      {error && (
        <p role="alert" className="error">
          {error.message}
        </p>
      )}
      <div className="form-actions">
        {cancel}
        <button type="submit" disabled={submitting}>
          {submitting ? "Guardando…" : submitLabel}
        </button>
      </div>
    </form>
  );
}
