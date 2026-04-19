package com.manutex.pitstop.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RenavamValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Renavam {
    String message() default "RENAVAM inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
