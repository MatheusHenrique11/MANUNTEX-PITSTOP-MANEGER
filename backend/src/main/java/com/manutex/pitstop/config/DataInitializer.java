package com.manutex.pitstop.config;

import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final boolean enabled;
    private final String seedEmail;
    private final String seedPassword;

    DataInitializer(
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${app.seed.enabled:false}") boolean enabled,
            @Value("${app.seed.email:turbofastgarage@pitstop.com}") String seedEmail,
            @Value("${app.seed.password:#{null}}") String seedPassword) {
        this.users = users;
        this.encoder = encoder;
        this.enabled = enabled;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;

        if (seedPassword == null || seedPassword.isBlank()) {
            log.warn("[DataInitializer] app.seed.password não configurado — seed ignorado.");
            return;
        }

        if (users.existsByEmail(seedEmail)) {
            log.info("[DataInitializer] Usuário de seed '{}' já existe.", seedEmail);
            return;
        }

        User admin = User.builder()
                .email(seedEmail)
                .passwordHash(encoder.encode(seedPassword))
                .fullName("Turbo Fast Garage Admin")
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .build();

        users.save(admin);
        log.info("[DataInitializer] Usuário admin '{}' criado com sucesso.", seedEmail);
    }
}
