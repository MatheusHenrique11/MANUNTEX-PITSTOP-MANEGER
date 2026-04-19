package com.manutex.pitstop.domain.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RenavamValidatorTest {

    private RenavamValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RenavamValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "00258665590",  // 11 dígitos válido
        "258665590",    // 9 dígitos válido (mesmo RENAVAM)
        "01234567890"
    })
    void deveAceitarRenavamValido(String renavam) {
        assertThat(validator.isValid(renavam, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "00000000000",  // todos zeros
        "12345678901",  // dígito verificador errado
        "1234",         // muito curto
        "abc",          // não numérico
        ""
    })
    void deveRejeitarRenavamInvalido(String renavam) {
        assertThat(validator.isValid(renavam, null)).isFalse();
    }
}
