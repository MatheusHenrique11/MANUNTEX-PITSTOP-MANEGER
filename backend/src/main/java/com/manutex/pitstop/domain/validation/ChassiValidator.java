package com.manutex.pitstop.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida o número de chassi conforme ISO 3779 (17 caracteres alfanuméricos).
 * Regras:
 *  - Exatamente 17 caracteres
 *  - Apenas letras (A-Z exceto I, O, Q) e dígitos (0-9)
 *  - Dígito verificador (posição 9) calculado pelo algoritmo NHTSA/ISO 3779
 */
public class ChassiValidator implements ConstraintValidator<Chassi, String> {

    // Letras proibidas pelo padrão ISO 3779
    private static final String PATTERN = "^[A-HJ-NPR-Z0-9]{17}$";

    private static final int[] WEIGHTS = {8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return false;

        String vin = value.toUpperCase().trim();
        if (!vin.matches(PATTERN)) return false;

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += transliterate(vin.charAt(i)) * WEIGHTS[i];
        }

        int remainder = sum % 11;
        char checkChar = (remainder == 10) ? 'X' : Character.forDigit(remainder, 10);
        return checkChar == vin.charAt(8);
    }

    private int transliterate(char c) {
        if (Character.isDigit(c)) return Character.getNumericValue(c);
        // Mapa de transliteração ISO 3779
        return switch (c) {
            case 'A','J'      -> 1;
            case 'B','K','S'  -> 2;
            case 'C','L','T'  -> 3;
            case 'D','M','U'  -> 4;
            case 'E','N','V'  -> 5;
            case 'F','W'      -> 6;
            case 'G','P','X'  -> 7;
            case 'H','Y'      -> 8;
            case 'R','Z'      -> 9;
            default           -> 0;
        };
    }
}
