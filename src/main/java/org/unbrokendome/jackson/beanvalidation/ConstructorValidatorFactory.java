package org.unbrokendome.jackson.beanvalidation;

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Constructor;

public interface ConstructorValidatorFactory {
    Validator getValidator(ValidatorFactory validatorFactory, Constructor constructor, Object[] parameterValues);
}
