package com.manutex.pitstop.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida o RENAVAM usando o algoritmo oficial do DENATRAN.
 * Aceita 9 ou 11 dígitos (com ou sem zeros à esquerda).
 */
public class RenavamValidator implements ConstraintValidator<Renavam, String> {

    private static final int[] WEIGHTS_11 = {3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_9  = {2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return false;

        String digits = value.replaceAll("\\D", "");

        // Normaliza para 11 dígitos com padding de zeros
        if (digits.length() == 9) {
            digits = "00" + digits;
        }
        if (digits.length() != 11) return false;

        // RENAVAM com todos os dígitos iguais a zero é inválido
        if (digits.matches("0+")) return false;

        return validateCheckDigit(digits, WEIGHTS_11);
    }

    private boolean validateCheckDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        int checkDigit = (remainder < 2) ? 0 : 11 - remainder;
        return checkDigit == Character.getNumericValue(digits.charAt(digits.length() - 1));
    }
}
