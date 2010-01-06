package org.multiverse.templates;

/**
 * A {@link RuntimeException} that indicates that a transaction wont be committed because
 * it is aborted.
 *
 * @author Peter Veentjer
 */
public class AbortedException extends RuntimeException{

    private static final long serialVersionUID = 0;   

    public AbortedException() {
    }

    public AbortedException(Throwable cause) {
        super(cause);
    }

    public AbortedException(String message) {
        super(message);
    }

    public AbortedException(String message, Throwable cause) {
        super(message, cause);
    }
}
