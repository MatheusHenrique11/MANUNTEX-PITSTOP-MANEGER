package com.manutex.pitstop.domain.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ChassiValidatorTest {

    private ChassiValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ChassiValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "9BWZZZ372VT004251",   // VIN brasileiro válido (dígito verificador '2' na pos 8)
        "1HGBH41JXMN109186",   // VIN norte-americano válido
        "WBA3A5G59DNP26082",   // VIN alemão válido (BMW)
    })
    void deveAceitarChassiValido(String chassi) {
        assertThat(validator.isValid(chassi, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "9BWZZZ377VT00425",    // 16 caracteres (curto demais)
        "9BWZZZ377VT0042512",  // 18 caracteres (longo demais)
        "9BWZZZ377VT00425I",   // contém 'I' (proibido ISO 3779)
        "9BWZZZ377VT00425O",   // contém 'O' (proibido ISO 3779)
        "9BWZZZ377VT00425Q",   // contém 'Q' (proibido ISO 3779)
        "9BWZZZ377VT004250",   // dígito verificador errado
        "",
    })
    void deveRejeitarChassiInvalido(String chassi) {
        assertThat(validator.isValid(chassi, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "9bwzzz372vt004251",   // minúsculo deve ser aceito (normaliza para maiúsculo)
        "9BWZZZ372vt004251",   // misto
    })
    void deveAceitarChassiEmMinusculo(String chassi) {
        assertThat(validator.isValid(chassi, null)).isTrue();
    }
}
