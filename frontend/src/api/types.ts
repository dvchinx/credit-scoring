// Reflejo de los DTOs del API (records en backend/.../infrastructure/in/web). Si cambia un DTO,
// su tipo se actualiza en el mismo cambio.

export type Role = "ADMIN" | "ANALYST";

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  username: string;
  role: Role;
}

export type ApplicationStatus = "PENDING" | "IN_REVIEW" | "APPROVED" | "REJECTED" | "MANUAL_REVIEW";

export interface CreditHistory {
  revolvingUtilization: number;
  openCreditLines: number;
  realEstateLoans: number;
  latePayments30To59Days: number;
  latePayments60To89Days: number;
  latePayments90DaysOrMore: number;
}

export interface CreditApplicationRequest {
  applicantFullName: string;
  documentId: string;
  birthDate: string;
  monthlyIncome: number;
  requestedAmount: number;
  loanTermMonths: number;
  employmentYears: number;
  existingMonthlyDebt: number;
  numberOfDependents: number;
  creditHistory: CreditHistory;
}

export interface CreditApplication extends CreditApplicationRequest {
  id: string;
  status: ApplicationStatus;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export type DecisionStatus = "COMPLETED" | "FAILED";
export type DecisionOutcome = "APPROVED" | "REJECTED" | "MANUAL_REVIEW";
export type RiskDirection = "INCREASES_RISK" | "DECREASES_RISK" | "NEUTRAL";

export interface FeatureContribution {
  feature: string;
  value: number;
  shapValue: number;
  direction: RiskDirection;
}

export interface Explanation {
  baseValue: number;
  outputValue: number;
  contributions: FeatureContribution[];
}

export interface Policy {
  approveMaxProbability: number;
  rejectMinProbability: number;
  maxDebtRatio: number;
  minApplicantAge: number;
}

export interface CreditDecision {
  id: string;
  creditApplicationId: string;
  status: DecisionStatus;
  outcome: DecisionOutcome | null;
  probabilityOfDefault: number | null;
  riskScore: number | null;
  modelVersion: string | null;
  reasons: string[];
  explanation: Explanation | null;
  modelInput: Record<string, number>;
  policy: Policy;
  failureReason: string | null;
  decidedBy: string;
  decidedAt: string;
}

export interface WhatIfRequest {
  monthlyIncome?: number;
  requestedAmount?: number;
  loanTermMonths?: number;
  existingMonthlyDebt?: number;
  numberOfDependents?: number;
  creditHistory?: Partial<CreditHistory>;
}

export interface WhatIfScenario {
  outcome: DecisionOutcome;
  probabilityOfDefault: number;
  riskScore: number;
  outputValue: number;
  reasons: string[];
}

export interface FeatureImpact {
  feature: string;
  baselineValue: number;
  scenarioValue: number;
  baselineShap: number;
  scenarioShap: number;
  shapDelta: number;
  changed: boolean;
}

export interface WhatIfResponse {
  id: string;
  creditApplicationId: string;
  modelVersion: string;
  changes: { variable: string; baselineValue: number; scenarioValue: number }[];
  baseline: WhatIfScenario;
  scenario: WhatIfScenario;
  decisionChanged: boolean;
  probabilityOfDefaultDelta: number;
  riskScoreDelta: number;
  baseValue: number;
  featureImpacts: FeatureImpact[];
  policy: Policy;
  requestedBy: string;
  simulatedAt: string;
}

export interface ModelVersion {
  version: string;
  active: boolean;
  algorithm: string;
  hyperparameters: Record<string, string | number | boolean | null>;
  trainedAt: string;
  dataset: { source: string; sha256: string; trainingRows: number; testRows: number; defaultRate: number };
  metrics: Record<string, number>;
  featureNames: string[];
  artifactSha256: string;
  registeredBy: string;
  registeredAt: string;
}

export interface ModelActivation {
  id: number;
  modelVersion: string;
  reason: string;
  activatedBy: string;
  activatedAt: string;
}

export interface AuditSummary {
  completedDecisions: number;
  failedDecisions: number;
  decisionsByOutcome: Record<DecisionOutcome, number>;
  decisionsByModelVersion: {
    modelVersion: string;
    approved: number;
    rejected: number;
    manualReview: number;
    total: number;
  }[];
  recentFailures: {
    decisionId: string;
    creditApplicationId: string;
    failureReason: string;
    decidedBy: string;
    decidedAt: string;
  }[];
  simulations: { total: number; outcomeChanged: number };
}

/** Cuerpo de error del backend (GlobalExceptionHandler / ErrorResponse). */
export interface ApiErrorBody {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  details?: string[];
}
