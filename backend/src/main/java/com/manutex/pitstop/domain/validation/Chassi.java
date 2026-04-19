package com.manutex.pitstop.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ChassiValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Chassi {
    String message() default "Chassi inválido (padrão ISO 3779)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
