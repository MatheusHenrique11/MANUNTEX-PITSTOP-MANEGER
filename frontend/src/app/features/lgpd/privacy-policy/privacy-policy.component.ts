import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-screen bg-surface-950 text-slate-300 px-4 py-12">
      <div class="max-w-3xl mx-auto space-y-8">

        <div>
          <a routerLink="/" class="text-petroleum-400 hover:underline text-sm">← Voltar</a>
          <h1 class="text-3xl font-bold text-white mt-4">Política de Privacidade</h1>
          <p class="text-sm text-slate-500 mt-1">PitStop Manager · Versão 1.0 · Vigência: 26/05/2026</p>
        </div>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">1. Controlador de Dados</h2>
          <p>O tratamento de dados pessoais é realizado pela <strong class="text-white">Manutex Tecnologia Ltda.</strong>,
             inscrita no CNPJ sob nº XX.XXX.XXX/0001-XX, com sede em São Paulo/SP.</p>
          <p><strong class="text-white">Encarregado (DPO):</strong> privacidade&#64;manutex.com.br</p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">2. Dados Coletados</h2>
          <ul class="list-disc list-inside space-y-2 text-sm">
            <li><strong class="text-white">Usuários da plataforma:</strong> nome completo, e-mail e senha (hash bcrypt).</li>
            <li><strong class="text-white">Clientes da oficina:</strong> nome, CPF/CNPJ, telefone e e-mail.</li>
            <li><strong class="text-white">Veículos:</strong> placa, chassi (mascarado), RENAVAM (mascarado), marca, modelo e ano.</li>
            <li><strong class="text-white">Documentos:</strong> CRLV, laudos — armazenados com criptografia AES-256-GCM.</li>
            <li><strong class="text-white">Dados fiscais:</strong> CNPJ, endereço e e-mail da empresa para emissão de NFS-e.</li>
            <li><strong class="text-white">Logs de acesso:</strong> endereço IP e data/hora de operações sobre dados pessoais.</li>
          </ul>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">3. Finalidade e Base Legal</h2>
          <div class="overflow-x-auto">
            <table class="w-full text-sm border-collapse">
              <thead>
                <tr class="text-left text-slate-400 border-b border-surface-700">
                  <th class="py-2 pr-4">Finalidade</th>
                  <th class="py-2">Base Legal (LGPD)</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-surface-800">
                <tr><td class="py-2 pr-4">Gestão de ordens de serviço</td><td class="py-2">Art. 7, V — execução de contrato</td></tr>
                <tr><td class="py-2 pr-4">Autenticação e controle de acesso</td><td class="py-2">Art. 7, V — execução de contrato</td></tr>
                <tr><td class="py-2 pr-4">Emissão de NFS-e</td><td class="py-2">Art. 7, II — cumprimento de obrigação legal</td></tr>
                <tr><td class="py-2 pr-4">Rastreabilidade de documentos</td><td class="py-2">Art. 7, II — cumprimento de obrigação legal</td></tr>
                <tr><td class="py-2 pr-4">Envio de comunicações do produto</td><td class="py-2">Art. 7, I — consentimento</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">4. Compartilhamento de Dados</h2>
          <p class="text-sm">Os dados são compartilhados exclusivamente com:</p>
          <ul class="list-disc list-inside space-y-2 text-sm">
            <li><strong class="text-white">Focus NFe</strong> — emissão de Nota Fiscal de Serviço Eletrônica.</li>
            <li><strong class="text-white">Stripe</strong> — processamento de pagamentos de assinatura (não recebe dados de clientes finais).</li>
            <li><strong class="text-white">Provedores de infraestrutura</strong> (AWS/Railway) — hospedagem com acordos de processamento de dados.</li>
          </ul>
          <p class="text-sm">Não vendemos dados a terceiros.</p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">5. Retenção de Dados</h2>
          <ul class="list-disc list-inside space-y-2 text-sm">
            <li>Dados de usuários ativos: mantidos durante a vigência do contrato.</li>
            <li>Após solicitação de exclusão: anonimizados em até 15 dias, excluídos permanentemente em 90 dias.</li>
            <li>Dados fiscais (NFS-e): mantidos por 5 anos conforme legislação tributária.</li>
            <li>Logs de auditoria: mantidos por 2 anos.</li>
          </ul>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">6. Seus Direitos (Art. 18 LGPD)</h2>
          <ul class="list-disc list-inside space-y-2 text-sm">
            <li><strong class="text-white">Acesso:</strong> Confirmar a existência de tratamento e obter cópia dos dados.</li>
            <li><strong class="text-white">Correção:</strong> Solicitar atualização de dados incompletos ou inexatos.</li>
            <li><strong class="text-white">Portabilidade:</strong> Receber seus dados em formato estruturado (JSON).</li>
            <li><strong class="text-white">Eliminação:</strong> Solicitar anonimização ou exclusão de dados desnecessários.</li>
            <li><strong class="text-white">Oposição:</strong> Opor-se ao tratamento com base em consentimento.</li>
            <li><strong class="text-white">Revogação do consentimento:</strong> A qualquer tempo, sem prejuízo ao tratamento já realizado.</li>
          </ul>
          <p class="text-sm">Para exercer seus direitos acesse <strong class="text-white">Configurações → Privacidade</strong> ou envie e-mail para privacidade&#64;manutex.com.br.</p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">7. Segurança</h2>
          <ul class="list-disc list-inside space-y-2 text-sm">
            <li>Documentos criptografados com AES-256-GCM em repouso.</li>
            <li>Senhas armazenadas com hash BCrypt (custo 12).</li>
            <li>Tokens JWT em cookies HTTP-Only + Secure + SameSite.</li>
            <li>Comunicações protegidas por TLS 1.2+.</li>
            <li>Controle de acesso por papel (RBAC): ADMIN, GERENTE, MECÂNICO.</li>
          </ul>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">8. Transferência Internacional</h2>
          <p class="text-sm">Dados podem ser processados em servidores nos EUA (AWS) sob cláusulas contratuais padrão compatíveis com o Art. 33 da LGPD.</p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">9. Atualizações</h2>
          <p class="text-sm">Esta política pode ser atualizada. Notificaremos sobre alterações relevantes com antecedência mínima de 30 dias. O uso continuado após a vigência da nova versão implica aceitação.</p>
        </section>

        <div class="text-center pt-4">
          <a routerLink="/" class="btn-primary px-8 py-2 text-sm">Voltar ao sistema</a>
        </div>
      </div>
    </div>
  `,
})
export class PrivacyPolicyComponent {}
