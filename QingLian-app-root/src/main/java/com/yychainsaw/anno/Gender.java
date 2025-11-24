package com.yychainsaw.anno;

import com.yychainsaw.validation.GenderValidation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(
        validatedBy = {GenderValidation.class}
)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Gender {

    String message() default "Gender must be either '男' or '女'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
