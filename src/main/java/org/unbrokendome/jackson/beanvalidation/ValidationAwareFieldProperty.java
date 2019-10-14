package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.FieldProperty;

import javax.validation.Validator;


public class ValidationAwareFieldProperty extends AbstractValidationAwareProperty<FieldProperty> {

    ValidationAwareFieldProperty(SettableBeanProperty src, Validator validator) {
        super(src, validator);
    }

    @Override
    protected SettableBeanProperty withDelegate(SettableBeanProperty delegate) {
        return new ValidationAwareFieldProperty(delegate, validator);
    }
}
