package io.abc.shared.di;

import java.io.Serial;

public class BeanCreationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2158884403377231877L;

    public BeanCreationException(String message, Exception exp) {
        super(message, exp);
    }

    public BeanCreationException(String message) {
        super(message);
    }
}
