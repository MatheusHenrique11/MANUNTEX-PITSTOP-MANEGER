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
