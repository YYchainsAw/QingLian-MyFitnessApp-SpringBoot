package com.yychainsaw.anno;

import com.yychainsaw.validation.FriendShipStateValidation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(
        validatedBy = {FriendShipStateValidation.class}
)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FRState {

    String message() default "FriendshipState must be either 'PENDING' or 'ACCEPTED' or 'DECLINED' or 'BLOCKED'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}