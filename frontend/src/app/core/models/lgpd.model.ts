export type ConsentType = 'PRIVACY_POLICY' | 'TERMS_OF_USE';

export type DsarType =
  | 'ACCESS'
  | 'PORTABILITY'
  | 'CORRECTION'
  | 'ERASURE'
  | 'OBJECTION'
  | 'RESTRICTION';

export type DsarStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';

export interface ConsentRequest {
  policyType: ConsentType;
  policyVersion: string;
  accepted: boolean;
}

export interface ConsentStatusResponse {
  currentPrivacyPolicyVersion: string;
  privacyPolicyAccepted: boolean;
  privacyPolicyAcceptedAt: string | null;

  currentTermsOfUseVersion: string;
  termsOfUseAccepted: boolean;
  termsOfUseAcceptedAt: string | null;

  allRequired: boolean;
}

export interface DsarRequest {
  requestType: DsarType;
  notes?: string;
}

export interface DsarResponse {
  id: string;
  requesterEmail: string;
  requestType: DsarType;
  status: DsarStatus;
  notes?: string;
  responseNotes?: string;
  requestedAt: string;
  deadlineAt: string;
  completedAt?: string;
  processedBy?: string;
}

export interface MyDataExport {
  exportDate: string;
  userData: {
    id: string;
    email: string;
    fullName: string;
    role: string;
    empresa: string;
    createdAt: string;
  };
  consentHistory: {
    type: ConsentType;
    version: string;
    accepted: boolean;
    date: string;
  }[];
  dsarHistory: {
    id: string;
    type: DsarType;
    status: DsarStatus;
    requestedAt: string;
    deadlineAt: string;
    completedAt?: string;
  }[];
}

export const DSAR_TYPE_LABELS: Record<DsarType, string> = {
  ACCESS:      'Acesso / Confirmação de tratamento',
  PORTABILITY: 'Portabilidade de dados',
  CORRECTION:  'Correção de dados',
  ERASURE:     'Eliminação / Anonimização',
  OBJECTION:   'Oposição ao tratamento',
  RESTRICTION: 'Limitação do tratamento',
};

export const DSAR_STATUS_LABELS: Record<DsarStatus, string> = {
  PENDING:     'Aguardando',
  IN_PROGRESS: 'Em andamento',
  COMPLETED:   'Concluída',
  REJECTED:    'Indeferida',
};
