package com.manutex.pitstop.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PlacaValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Placa {
    String message() default "Placa inválida (aceito: ABC1234 ou ABC1D23)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
