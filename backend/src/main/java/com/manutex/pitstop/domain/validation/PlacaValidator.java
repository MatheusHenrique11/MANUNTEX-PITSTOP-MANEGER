package com.manutex.pitstop.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida placas no padrão antigo (ABC1234) e Mercosul (ABC1D23).
 * Aceita com ou sem hífen.
 */
public class PlacaValidator implements ConstraintValidator<Placa, String> {

    // Padrão antigo: 3 letras + 4 dígitos
    private static final String PADRAO_ANTIGO   = "^[A-Z]{3}[0-9]{4}$";
    // Padrão Mercosul: 3 letras + 1 dígito + 1 letra + 2 dígitos
    private static final String PADRAO_MERCOSUL = "^[A-Z]{3}[0-9][A-Z][0-9]{2}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return false;

        // Remove hífen e espaços, normaliza para maiúsculo
        String placa = value.toUpperCase().replaceAll("[\\s-]", "");

        return placa.matches(PADRAO_ANTIGO) || placa.matches(PADRAO_MERCOSUL);
    }
}
