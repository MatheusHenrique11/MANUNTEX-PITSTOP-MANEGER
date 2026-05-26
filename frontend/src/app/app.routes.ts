import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
import { roleGuard } from '@core/guards/role.guard';
import { featureFlagGuard } from '@core/guards/feature-flag.guard';
import { subscriptionGuard } from '@core/guards/subscription.guard';
import { consentGuard } from '@core/guards/consent.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },

  {
    path: 'signup',
    loadComponent: () =>
      import('./features/auth/signup/signup.component').then(m => m.SignupComponent),
  },

  // ── LGPD — páginas públicas (sem autenticação) ────────────────────────────
  {
    path: 'politica-privacidade',
    loadComponent: () =>
      import('./features/lgpd/privacy-policy/privacy-policy.component')
        .then(m => m.PrivacyPolicyComponent),
  },
  {
    path: 'termos-de-uso',
    loadComponent: () =>
      import('./features/lgpd/terms-of-use/terms-of-use.component')
        .then(m => m.TermsOfUseComponent),
  },

  // ── LGPD — página de consentimento (autenticado, sem consentGuard) ────────
  {
    path: 'consent',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lgpd/consent/consent.component').then(m => m.ConsentComponent),
  },

  // ── Rastreio público (sem autenticação) ───────────────────
  {
    path: 'rastreio/:token',
    loadComponent: () =>
      import('./features/rastreio/rastreio.component').then(m => m.RastreioComponent),
  },

  {
    path: '',
    loadComponent: () =>
      import('./shared/components/layout/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard, consentGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },

      {
        path: 'dashboard',
        canActivate: [subscriptionGuard],
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },

      // ── Módulo: Billing / Assinatura ──────────────────────────
      {
        path: 'billing',
        children: [
          {
            path: 'pricing',
            loadComponent: () =>
              import('./features/billing/pricing/pricing.component')
                .then(m => m.PricingComponent),
          },
          {
            path: 'dashboard',
            canActivate: [roleGuard],
            data: { roles: ['ROLE_ADMIN', 'ROLE_GERENTE'] },
            loadComponent: () =>
              import('./features/billing/dashboard/billing-dashboard.component')
                .then(m => m.BillingDashboardComponent),
          },
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
        ],
      },

      // ── Módulo: Gestão de Veículos ─────────────────────────────
      {
        path: 'veiculos',
        canActivate: [featureFlagGuard],
        data: { feature: 'VEHICLE_MANAGEMENT' },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/veiculos/lista/veiculo-lista.component')
                .then(m => m.VeiculoListaComponent),
          },
          {
            path: 'novo',
            loadComponent: () =>
              import('./features/veiculos/form/veiculo-form.component')
                .then(m => m.VeiculoFormComponent),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/veiculos/form/veiculo-form.component')
                .then(m => m.VeiculoFormComponent),
          },
        ],
      },

      // ── Módulo: Cofre de Documentos ───────────────────────────
      {
        path: 'documentos',
        canActivate: [featureFlagGuard],
        data: { feature: 'DOCUMENT_VAULT' },
        children: [
          {
            path: 'upload',
            loadComponent: () =>
              import('./features/documentos/upload/documento-upload.component')
                .then(m => m.DocumentoUploadComponent),
          },
          {
            path: ':id/view',
            loadComponent: () =>
              import('./features/documentos/viewer/documento-viewer.component')
                .then(m => m.DocumentoViewerComponent),
          },
        ],
      },

      // ── Admin: Gestão de Usuários ──────────────────────────────
      {
        path: 'admin/usuarios',
        canActivate: [roleGuard],
        data: { roles: ['ROLE_ADMIN'] },
        loadComponent: () =>
          import('./features/admin/usuarios/usuarios.component')
            .then(m => m.UsuariosComponent),
      },

      // ── Admin: Controle de Módulos (Feature Flags) ─────────────
      {
        path: 'admin/modulos',
        canActivate: [roleGuard],
        data: { roles: ['ROLE_ADMIN'] },
        loadComponent: () =>
          import('./features/admin/feature-flags/feature-flags.component')
            .then(m => m.FeatureFlagsComponent),
      },

      // ── Módulo: Manutenções ───────────────────────────────────
      {
        path: 'manutencoes',
        canActivate: [subscriptionGuard, featureFlagGuard],
        data: { feature: 'MAINTENANCE_MODULE' },
        loadComponent: () =>
          import('./features/manutencoes/manutencoes.component')
            .then(m => m.ManutencoesComponent),
      },

      // ── Módulo: Financeiro ────────────────────────────────────
      {
        path: 'financeiro',
        canActivate: [subscriptionGuard, featureFlagGuard],
        data: { feature: 'FINANCIAL_MODULE' },
        loadComponent: () =>
          import('./features/financeiro/financeiro.component')
            .then(m => m.FinanceiroComponent),
      },

      // ── Módulo: Relatórios ────────────────────────────────────
      {
        path: 'relatorios',
        canActivate: [subscriptionGuard, featureFlagGuard],
        data: { feature: 'ANALYTICS_DASHBOARD' },
        loadComponent: () =>
          import('./features/relatorios/relatorios.component')
            .then(m => m.RelatoriosComponent),
      },

      // ── Módulo: Metas por Mecânico ────────────────────────────
      {
        path: 'metas',
        canActivate: [featureFlagGuard],
        data: { feature: 'GOALS_MODULE' },
        children: [
          {
            path: 'mecanico',
            canActivate: [roleGuard],
            data: { roles: ['ROLE_MECANICO', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
            loadComponent: () =>
              import('./features/metas/mecanico/minhas-metas.component')
                .then(m => m.MinhasMetasComponent),
          },
          {
            path: 'gerente',
            canActivate: [roleGuard],
            data: { roles: ['ROLE_GERENTE', 'ROLE_ADMIN'] },
            loadComponent: () =>
              import('./features/metas/gerente/metas-gerente.component')
                .then(m => m.MetasGerenteComponent),
          },
          {
            path: 'gerente/mecanico/:mecanicoId',
            canActivate: [roleGuard],
            data: { roles: ['ROLE_GERENTE', 'ROLE_ADMIN'] },
            loadComponent: () =>
              import('./features/metas/gerente/detalhe-mecanico.component')
                .then(m => m.DetalheMecanicoComponent),
          },
          { path: '', redirectTo: 'gerente', pathMatch: 'full' },
        ],
      },

      // ── Privacidade / LGPD (painel do titular) ────────────────
      {
        path: 'privacidade',
        loadComponent: () =>
          import('./features/lgpd/panel/lgpd-panel.component')
            .then(m => m.LgpdPanelComponent),
      },

      // ── Erro 403 ──────────────────────────────────────────────
      {
        path: '403',
        loadComponent: () =>
          import('./features/errors/forbidden/forbidden.component')
            .then(m => m.ForbiddenComponent),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
