package com.manutex.pitstop.domain.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PlacaValidatorTest {

    private final PlacaValidator validator = new PlacaValidator();

    @ParameterizedTest
    @ValueSource(strings = {
        "ABC1234",   // padrão antigo
        "ABC-1234",  // antigo com hífen
        "ABC1D23",   // Mercosul
        "abc1d23",   // minúsculo (deve normalizar)
    })
    void deveAceitarPlacaValida(String placa) {
        assertThat(validator.isValid(placa, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "AB12345",   // formato errado
        "ABCD123",   // 4 letras
        "ABC12345",  // 5 dígitos
        "1BC1234",   // começa com número
        "",
    })
    void deveRejeitarPlacaInvalida(String placa) {
        assertThat(validator.isValid(placa, null)).isFalse();
    }
}
