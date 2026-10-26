package com.comanda.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<CnpjValido, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
        return valor == null || valor.isBlank() || Cnpj.valido(valor);
    }
}
