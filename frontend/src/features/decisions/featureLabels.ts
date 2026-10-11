/** Etiquetas legibles para las variables del modelo (solo presentación). */
const FEATURE_LABELS: Record<string, string> = {
  revolving_utilization_of_unsecured_lines: "Utilización de crédito rotativo",
  age: "Edad",
  number_of_time_30_59_days_past_due_not_worse: "Moras de 30-59 días",
  debt_ratio: "Endeudamiento (con la cuota nueva)",
  monthly_income: "Ingreso mensual",
  number_of_open_credit_lines_and_loans: "Líneas de crédito abiertas",
  number_of_times_90_days_late: "Moras de 90+ días",
  number_real_estate_loans_or_lines: "Créditos hipotecarios",
  number_of_time_60_89_days_past_due_not_worse: "Moras de 60-89 días",
  number_of_dependents: "Dependientes",
};

const VARIABLE_LABELS: Record<string, string> = {
  monthlyIncome: "Ingreso mensual",
  requestedAmount: "Monto solicitado",
  loanTermMonths: "Plazo (meses)",
  existingMonthlyDebt: "Deuda mensual existente",
  numberOfDependents: "Dependientes",
  "creditHistory.revolvingUtilization": "Utilización de crédito rotativo",
  "creditHistory.openCreditLines": "Líneas de crédito abiertas",
  "creditHistory.realEstateLoans": "Créditos hipotecarios",
  "creditHistory.latePayments30To59Days": "Moras de 30-59 días",
  "creditHistory.latePayments60To89Days": "Moras de 60-89 días",
  "creditHistory.latePayments90DaysOrMore": "Moras de 90+ días",
};

export const featureLabel = (feature: string) => FEATURE_LABELS[feature] ?? feature;
export const variableLabel = (variable: string) => VARIABLE_LABELS[variable] ?? variable;
