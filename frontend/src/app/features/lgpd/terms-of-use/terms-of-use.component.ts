import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-terms-of-use',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-screen bg-surface-950 text-slate-300 px-4 py-12">
      <div class="max-w-3xl mx-auto space-y-8">

        <div>
          <a routerLink="/" class="text-petroleum-400 hover:underline text-sm">← Voltar</a>
          <h1 class="text-3xl font-bold text-white mt-4">Termos de Uso</h1>
          <p class="text-sm text-slate-500 mt-1">PitStop Manager · Versão 1.0 · Vigência: 26/05/2026</p>
        </div>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">1. Aceite</h2>
          <p class="text-sm">Ao acessar ou usar o PitStop Manager você concorda com estes Termos. Se não concordar, não utilize o serviço.</p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">2. Descrição do Serviço</h2>
          <p class="text-sm">O PitStop Manager é uma plataforma SaaS de gestão de ordens de serviço para oficinas mecânicas, fornecida pela <strong class="text-white">Manutex Tecnologia Ltda.</strong></p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">3. Uso Permitido</h2>
          <ul class="list-disc list-inside space-y-2 text-sm">
            <li>Utilizar o serviço apenas para gestão legítima de uma oficina mecânica.</li>
            <li>Manter as credenciais de acesso em sigilo.</li>
            <li>Inserir apenas dados verdadeiros e autorizados pelos titulares.</li>
          </ul>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">4. Uso Proibido</h2>
          <ul class="list-disc list-inside space-y-2 text-sm">
            <li>Compartilhar credenciais com terceiros não autorizados.</li>
            <li>Tentar acessar dados de outros tenants (isolamento multi-tenant).</li>
            <li>Realizar engenharia reversa, scraping ou uso automatizado não autorizado.</li>
            <li>Inserir dados falsos, fraudulentos ou que violem direitos de terceiros.</li>
            <li>Usar o serviço para atividades ilegais ou que violem a LGPD.</li>
          </ul>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">5. Responsabilidades</h2>
          <p class="text-sm"><strong class="text-white">Você é responsável por:</strong> a veracidade dos dados inseridos, o acesso de seus colaboradores e o cumprimento da LGPD em relação aos dados dos seus clientes.</p>
          <p class="text-sm"><strong class="text-white">A Manutex é responsável por:</strong> a segurança da infraestrutura, a disponibilidade do serviço (SLA 99,5%) e o tratamento adequado dos dados conforme a LGPD.</p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">6. Assinatura e Pagamento</h2>
          <ul class="list-disc list-inside space-y-2 text-sm">
            <li>O serviço é fornecido mediante assinatura mensal nos planos Starter, Professional ou Enterprise.</li>
            <li>O período de teste (Trial) tem duração de 14 dias sem cobrança.</li>
            <li>O não pagamento resultará na suspensão do acesso após 7 dias de atraso.</li>
            <li>Cancelamento pode ser feito a qualquer momento com efeito no fim do período pago.</li>
          </ul>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">7. Propriedade Intelectual</h2>
          <p class="text-sm">O software, marca e conteúdo são propriedade da Manutex. Os dados inseridos por você são de sua propriedade e podem ser exportados a qualquer tempo.</p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">8. Rescisão</h2>
          <p class="text-sm">Qualquer parte pode rescindir o contrato com 30 dias de aviso. Em caso de violação grave, a Manutex pode encerrar imediatamente. Os dados serão disponibilizados para exportação por 30 dias após a rescisão, depois removidos conforme a LGPD.</p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">9. Limitação de Responsabilidade</h2>
          <p class="text-sm">A Manutex não se responsabiliza por danos indiretos, lucros cessantes ou perda de dados decorrentes de uso indevido do serviço ou de eventos fora do controle razoável.</p>
        </section>

        <section class="space-y-4">
          <h2 class="text-lg font-semibold text-white border-b border-surface-700 pb-2">10. Foro e Lei Aplicável</h2>
          <p class="text-sm">Estes Termos são regidos pelas leis brasileiras. Fica eleito o foro da Comarca de São Paulo/SP para dirimir eventuais conflitos.</p>
        </section>

        <div class="text-center pt-4">
          <a routerLink="/" class="btn-primary px-8 py-2 text-sm">Voltar ao sistema</a>
        </div>
      </div>
    </div>
  `,
})
export class TermsOfUseComponent {}
