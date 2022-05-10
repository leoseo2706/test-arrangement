package com.fiats.arrangement.validator.annotation;

import com.fiats.arrangement.payload.filter.ArrangementFilterStatusEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ArrangementFilterStatusValidator implements ConstraintValidator<ArrangementFilterStatus, Integer> {

    private boolean isOptional;

    @Override
    public void initialize(ArrangementFilterStatus constraintAnnotation) {
        this.isOptional = constraintAnnotation.optional();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        boolean isValid = ArrangementFilterStatusEnum.lookForMetaModel(value) != null;
        return isOptional ? (isValid || value == null) : isValid;
    }
}
