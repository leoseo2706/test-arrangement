package com.fiats.arrangement.validator.annotation;

import com.fiats.arrangement.constant.AccountingEntryStatusEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class AccountingEntryStatusValidator implements ConstraintValidator<AccountingEntryStatus, String> {

    private boolean isOptional;

    @Override
    public void initialize(AccountingEntryStatus constraintAnnotation) {
        this.isOptional = constraintAnnotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        boolean isValid = AccountingEntryStatusEnum.isValidStatus(value);
        return isOptional ? (isValid || value == null) : isValid;
    }
}
