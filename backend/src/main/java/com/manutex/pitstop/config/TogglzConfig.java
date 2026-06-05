package com.manutex.pitstop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.repository.jdbc.JDBCStateRepository;

import javax.sql.DataSource;

/**
 * Configura o Togglz para usar JDBCStateRepository apontando para a tabela
 * TOGGLZ_FEATURE_STATE (criada pela migration Flyway V11).
 *
 * Sem este @Bean, togglz-spring-boot-autoconfigure 4.4.0 usa exclusivamente
 * InMemoryStateRepository — estados não persistem entre restarts do container.
 *
 * createTable=false  : tabela gerenciada pelo Flyway (V11).
 * noCommit=true      : usa a transação Spring existente em vez de fazer commit manual.
 * usePostgresTextColumns=true : converte VARCHAR para TEXT no PostgreSQL,
 *                               evitando truncamento e consistente com o SchemaUpdater.
 */
@Configuration
@RequiredArgsConstructor
public class TogglzConfig {

    private final DataSource dataSource;

    @Bean
    public StateRepository stateRepository() {
        return JDBCStateRepository.newBuilder(dataSource)
            .tableName("TOGGLZ_FEATURE_STATE")
            .createTable(false)
            .noCommit(true)
            .usePostgresTextColumns(true)
            .build();
    }
}
