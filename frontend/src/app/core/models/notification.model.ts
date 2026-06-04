export type NotificationEvent =
  | 'OS_CRIADA' | 'OS_EM_ANDAMENTO' | 'OS_AGUARDANDO_PECAS'
  | 'OS_CONCLUIDA' | 'OS_CANCELADA' | 'DOCUMENTO_VENCENDO' | 'ORCAMENTO_DISPONIVEL';

export type NotificationChannel = 'WHATSAPP' | 'EMAIL' | 'SMS' | 'PUSH';

export type NotificationStatus = 'PENDENTE' | 'ENVIADO' | 'FALHOU' | 'REJEITADO';

export interface NotificationTemplate {
  id:      string;
  evento:  NotificationEvent;
  canal:   NotificationChannel;
  titulo:  string | null;
  corpo:   string;
  ativo:   boolean;
}

export interface NotificationTemplateUpdateRequest {
  titulo: string | null;
  corpo:  string;
  ativo:  boolean;
}

export interface NotificationLog {
  id:            string;
  manutencaoId?: string;
  evento:        NotificationEvent;
  canal:         NotificationChannel;
  destinatario:  string | null;
  status:        NotificationStatus;
  errorMessage?: string;
  enviadoEm?:    string;
  createdAt:     string;
}

export interface NotificationConfig {
  whatsappProviderUrl?:    string;
  whatsappConfigured:      boolean;
  notificationEmailFrom?:  string;
  emailConfigured:         boolean;
}

export interface NotificationConfigRequest {
  whatsappProviderUrl?:    string;
  whatsappApiToken?:       string;
  whatsappInstanceName?:   string;
  notificationEmailFrom?:  string;
}

export interface PageResponse<T> {
  content:          T[];
  totalElements:    number;
  totalPages:       number;
  number:           number;
  size:             number;
}

export const EVENT_LABELS: Record<NotificationEvent, string> = {
  OS_CRIADA:            'OS Criada',
  OS_EM_ANDAMENTO:      'OS em Andamento',
  OS_AGUARDANDO_PECAS:  'Aguardando Peças',
  OS_CONCLUIDA:         'OS Concluída',
  OS_CANCELADA:         'OS Cancelada',
  DOCUMENTO_VENCENDO:   'Documento Vencendo',
  ORCAMENTO_DISPONIVEL: 'Orçamento Disponível',
};

export const CHANNEL_LABELS: Record<NotificationChannel, string> = {
  WHATSAPP: 'WhatsApp',
  EMAIL:    'E-mail',
  SMS:      'SMS',
  PUSH:     'Push',
};

export const STATUS_COLORS: Record<NotificationStatus, string> = {
  PENDENTE:  'badge-warning',
  ENVIADO:   'badge-active',
  FALHOU:    'badge-danger',
  REJEITADO: 'badge-inactive',
};

export const TEMPLATE_VARIABLES = [
  { key: '{{cliente_nome}}',   desc: 'Nome do cliente' },
  { key: '{{veiculo_placa}}',  desc: 'Placa do veículo' },
  { key: '{{veiculo_modelo}}', desc: 'Modelo do veículo' },
  { key: '{{os_link}}',        desc: 'Link de rastreio' },
  { key: '{{status}}',         desc: 'Status da OS' },
];
