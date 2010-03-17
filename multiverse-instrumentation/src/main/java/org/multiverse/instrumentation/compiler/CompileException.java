package org.multiverse.instrumentation.compiler;

/**
 * A {@link RuntimeException} that is thrown when the postprocessing of a Clazz fails.
 *
 * @author Peter Veentjer
 */
public class CompileException extends RuntimeException {

    public CompileException() {
    }

    public CompileException(String message) {
        super(message);
    }

    public CompileException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompileException(Throwable cause) {
        super(cause);
    }
}
