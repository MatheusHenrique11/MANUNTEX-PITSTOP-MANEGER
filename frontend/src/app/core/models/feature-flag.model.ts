export type FeatureName =
  | 'VEHICLE_MANAGEMENT'
  | 'DOCUMENT_VAULT'
  | 'MAINTENANCE_MODULE'
  | 'ANALYTICS_DASHBOARD'
  | 'NOTIFICATIONS'
  | 'FINANCIAL_MODULE'
  | 'DETRAN_INTEGRATION'
  | 'GOALS_MODULE';

export interface FeatureFlag {
  active: boolean;
  label: string;
}

export type FeatureFlagsMap = Record<FeatureName, FeatureFlag>;

export const DISABLED_MODULE_PARAM = 'modulo_desativado' as const;
