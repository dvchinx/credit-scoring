import type { ApplicationStatus, DecisionOutcome } from "../api/types";

const LABELS: Record<ApplicationStatus | DecisionOutcome | "FAILED", string> = {
  PENDING: "Pendiente",
  IN_REVIEW: "En revisión",
  APPROVED: "Aprobada",
  REJECTED: "Rechazada",
  MANUAL_REVIEW: "Revisión manual",
  FAILED: "Fallida",
};

const TONES: Record<ApplicationStatus | DecisionOutcome | "FAILED", string> = {
  PENDING: "neutral",
  IN_REVIEW: "neutral",
  APPROVED: "good",
  REJECTED: "bad",
  MANUAL_REVIEW: "warn",
  FAILED: "bad",
};

export function StatusBadge({
  value,
  size,
}: {
  value: ApplicationStatus | DecisionOutcome | "FAILED";
  size?: "lg";
}) {
  return <span className={`badge badge-${TONES[value]}${size ? ` badge-${size}` : ""}`}>{LABELS[value]}</span>;
}

export function statusLabel(value: ApplicationStatus | DecisionOutcome | "FAILED"): string {
  return LABELS[value];
}
