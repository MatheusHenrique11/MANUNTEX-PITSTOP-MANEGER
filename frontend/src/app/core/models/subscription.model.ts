export type SubscriptionStatus = 'TRIAL' | 'ACTIVE' | 'CANCELED' | 'PAST_DUE' | 'SUSPENDED';
export type SubscriptionPlan   = 'STARTER' | 'PROFESSIONAL' | 'ENTERPRISE';

export interface AssinaturaResponse {
  id: string;
  plano: SubscriptionPlan;
  status: SubscriptionStatus;
  gatewaySubscriptionId?: string;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  trialEnd?: string;
  createdAt: string;
}

export interface FaturaNfeResponse {
  id: string;
  gatewayInvoiceId?: string;
  nfeId?: string;
  nfeStatus?: string;
  valor: number;
  pdfUrl?: string;
  xmlUrl?: string;
  issueDate: string;
}

export interface CheckoutRequest {
  plano: SubscriptionPlan;
}

export interface CheckoutResponse {
  checkoutUrl: string;
}

export const PLAN_LABELS: Record<SubscriptionPlan, string> = {
  STARTER:      'Starter',
  PROFESSIONAL: 'Profissional',
  ENTERPRISE:   'Enterprise',
};

export const STATUS_LABELS: Record<SubscriptionStatus, string> = {
  TRIAL:     'Período de teste',
  ACTIVE:    'Ativa',
  CANCELED:  'Cancelada',
  PAST_DUE:  'Pagamento pendente',
  SUSPENDED: 'Suspensa',
};

export const ACTIVE_STATUSES: SubscriptionStatus[] = ['TRIAL', 'ACTIVE'];

// ── Plan Usage ──────────────────────────────────────────────────────────────

export interface UsageMetric {
  used:      number;
  limit:     number;
  unlimited: boolean;
}

export interface StorageMetric {
  usedBytes:  number;
  limitBytes: number;
  unlimited:  boolean;
}

export interface FeatureStatus {
  name:           string;
  label:          string;
  active:         boolean;
  includedInPlan: boolean;
}

export interface PlanUsageResponse {
  plano:     SubscriptionPlan;
  status:    SubscriptionStatus;
  os:        UsageMetric;
  mecanicos: UsageMetric;
  storage:   StorageMetric;
  features:  FeatureStatus[];
}

export const PLAN_PRICES: Record<SubscriptionPlan, string> = {
  STARTER:      'R$ 89/mês',
  PROFESSIONAL: 'R$ 179/mês',
  ENTERPRISE:   'R$ 349/mês',
};
