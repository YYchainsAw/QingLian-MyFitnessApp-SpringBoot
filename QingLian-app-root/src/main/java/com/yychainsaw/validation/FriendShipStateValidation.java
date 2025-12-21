package com.yychainsaw.validation;

import com.yychainsaw.anno.FRState;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FriendShipStateValidation implements ConstraintValidator<FRState, String> {
    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null) return false;

        return s.equals("PENDING") || s.equals("ACCEPTED") || s.equals("DECLINED") || s.equals("BLOCKED");
    }
}
