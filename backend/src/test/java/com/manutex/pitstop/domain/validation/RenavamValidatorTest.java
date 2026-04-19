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
        "00258665599",  // 11 dígitos válido (dígito verificador=9)
        "258665599",    // 9 dígitos válido (mesmo RENAVAM sem zeros à esquerda)
        "01234567897"   // 11 dígitos válido (dígito verificador=7)
    })
    void deveAceitarRenavamValido(String renavam) {
        assertThat(validator.isValid(renavam, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "00000000000",  // todos zeros — inválido
        "12345678901",  // dígito verificador errado (correto seria 0)
        "1234",         // muito curto
        "abc",          // não numérico
        ""
    })
    void deveRejeitarRenavamInvalido(String renavam) {
        assertThat(validator.isValid(renavam, null)).isFalse();
    }
}
