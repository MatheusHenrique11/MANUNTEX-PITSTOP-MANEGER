package com.manutex.pitstop.config;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.SubscriptionPlan;
import com.manutex.pitstop.domain.enums.SubscriptionStatus;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final EmpresaRepository empresas;
    private final PasswordEncoder encoder;
    private final boolean enabled;
    private final String seedEmail;
    private final String seedPassword;
    private final String seedFullName;
    private final String seedEmpresaNome;
    private final String seedEmpresaCnpj;
    private final String seedEmpresaCidade;
    private final String seedEmpresaUf;

    DataInitializer(
            UserRepository users,
            EmpresaRepository empresas,
            PasswordEncoder encoder,
            @Value("${app.seed.enabled:false}")           boolean enabled,
            @Value("${app.seed.email:demo@managerpitstop.com.br}") String seedEmail,
            @Value("${app.seed.password:}")               String seedPassword,
            @Value("${app.seed.full-name:Super Admin Demo}")       String seedFullName,
            @Value("${app.seed.empresa-nome:Manager PitStop Demo}") String seedEmpresaNome,
            @Value("${app.seed.empresa-cnpj:11.222.333/0001-81}")  String seedEmpresaCnpj,
            @Value("${app.seed.empresa-cidade:São Paulo}")         String seedEmpresaCidade,
            @Value("${app.seed.empresa-uf:SP}")                    String seedEmpresaUf) {
        this.users          = users;
        this.empresas       = empresas;
        this.encoder        = encoder;
        this.enabled        = enabled;
        this.seedEmail      = seedEmail;
        this.seedPassword   = seedPassword;
        this.seedFullName   = seedFullName;
        this.seedEmpresaNome  = seedEmpresaNome;
        this.seedEmpresaCnpj  = seedEmpresaCnpj;
        this.seedEmpresaCidade = seedEmpresaCidade;
        this.seedEmpresaUf    = seedEmpresaUf;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;

        if (seedPassword.isBlank()) {
            log.warn("[DataInitializer] app.seed.password não configurado — seed ignorado.");
            return;
        }

        if (users.existsByEmail(seedEmail)) {
            log.info("[DataInitializer] Usuário demo '{}' já existe — nenhuma ação.", seedEmail);
            return;
        }

        // 1. Criar (ou reusar) a empresa demo
        Empresa empresa = empresas.findByCnpj(seedEmpresaCnpj)
                .orElseGet(() -> {
                    Empresa e = Empresa.builder()
                            .nome(seedEmpresaNome)
                            .cnpj(seedEmpresaCnpj)
                            .razaoSocial(seedEmpresaNome)
                            .nomeFantasia(seedEmpresaNome)
                            .cidade(seedEmpresaCidade)
                            .uf(seedEmpresaUf)
                            .ativo(true)
                            .subscriptionStatus(SubscriptionStatus.ACTIVE)
                            .subscriptionPlan(SubscriptionPlan.ENTERPRISE)
                            .build();
                    @SuppressWarnings("null")
                    Empresa saved = empresas.save(e);
                    log.info("[DataInitializer] Empresa demo '{}' criada (CNPJ: {}).",
                            saved.getNome(), saved.getCnpj());
                    return saved;
                });

        // 2. Criar o usuário super admin vinculado à empresa demo
        User admin = User.builder()
                .email(seedEmail)
                .passwordHash(encoder.encode(seedPassword))
                .fullName(seedFullName)
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .empresa(empresa)
                .build();

        @SuppressWarnings("null")
        User saved = users.save(admin);
        log.info("[DataInitializer] Super Admin demo '{}' criado com sucesso (empresa: {}).",
                saved.getEmail(), empresa.getNome());
    }
}
