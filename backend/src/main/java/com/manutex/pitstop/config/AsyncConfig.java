package com.manutex.pitstop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool dedicado para tarefas @Async (ex: despacho de notificações).
 *
 * Dimensionamento conservador para VPS com 2 vCPUs:
 *   corePoolSize  = 2  — threads sempre disponíveis
 *   maxPoolSize   = 10 — pico em rajadas de notificações
 *   queueCapacity = 50 — buffer para não perder eventos sob carga
 *
 * Rejeição: CallerRunsPolicy — executa síncrono se fila cheia (graceful degradation).
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notif-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
