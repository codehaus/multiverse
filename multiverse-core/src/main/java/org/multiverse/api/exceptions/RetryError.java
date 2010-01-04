package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * An Error dat indicates that a retry should be done.
 * <p/>
 * It is an error because it should not be caught by some exception handler. This is a control flow regulating
 * exception. Something that normally would be a very bad thing, but adding custom control flow to a fixed language like
 * Java is otherwise almost impossible to do transparently.
 *
 * @author Peter Veentjer.
 */
public class RetryError extends Error {

    private static final long serialVersionUID = 0;

    private final static boolean reuse = parseBoolean(
            getProperty(RetryError.class.getName() + ".reuse", "true"));

    public final static RetryError INSTANCE = new RetryError();

    public static RetryError create() {
        if (reuse) {
            return RetryError.INSTANCE;
        } else {
            return new RetryError();
        }
    }
}
