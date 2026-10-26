package com.comanda.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

// RN07: anotação de Bean Validation. Uso: @CnpjValido String cnpj (no DTO). Valor nulo passa (use @NotBlank junto).
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CnpjValidator.class)
@Documented
public @interface CnpjValido {
    String message() default "CNPJ inválido (confira os dígitos)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
