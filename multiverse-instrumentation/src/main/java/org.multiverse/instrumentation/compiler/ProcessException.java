package org.multiverse.instrumentation.compiler;

/**
 * A {@link RuntimeException} that is thrown when the postprocessing of a Clazz fails.
 *
 * @author Peter Veentjer
 */
public class ProcessException extends RuntimeException {

    public ProcessException() {
    }

    public ProcessException(String message) {
        super(message);
    }

    public ProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessException(Throwable cause) {
        super(cause);
    }
}
