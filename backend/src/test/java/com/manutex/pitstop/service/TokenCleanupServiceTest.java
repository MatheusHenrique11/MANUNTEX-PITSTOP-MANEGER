package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    TokenCleanupService tokenCleanupService;

    @Test
    void deveChamarDeleteComInstantCorreto() {
        when(refreshTokenRepository.deleteExpiredAndRevoked(any())).thenReturn(3);

        Instant antes = Instant.now();
        tokenCleanupService.cleanupExpiredTokens();
        Instant depois = Instant.now();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteExpiredAndRevoked(captor.capture());

        Instant usado = captor.getValue();
        assertThat(usado).isBetween(antes, depois);
    }

    @Test
    void deveRetornarZeroQuandoNaoHaTokensParaRemover() {
        when(refreshTokenRepository.deleteExpiredAndRevoked(any())).thenReturn(0);

        tokenCleanupService.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredAndRevoked(any(Instant.class));
    }

    @Test
    void deveConcluirSemExcecaoComNumerosGrandesDeRemocoes() {
        when(refreshTokenRepository.deleteExpiredAndRevoked(any())).thenReturn(10_000);

        tokenCleanupService.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredAndRevoked(any(Instant.class));
    }
}
