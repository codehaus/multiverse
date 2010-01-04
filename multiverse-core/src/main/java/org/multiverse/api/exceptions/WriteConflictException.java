package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link CommitFailureException} that indicates that a write conflict happened while doing a {@link
 * org.multiverse.api.Transaction#commit()}.
 *
 * @author Peter Veentjer.
 */
public class WriteConflictException extends CommitFailureException
        implements RecoverableThrowable {

    private static final long serialVersionUID = 0;

    public final static WriteConflictException INSTANCE = new WriteConflictException();

    public final static boolean reuse = parseBoolean(
            getProperty(WriteConflictException.class.getName() + ".reuse", "true"));

    public WriteConflictException() {
    }

    public WriteConflictException(String message) {
        super(message);
    }

    public WriteConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public WriteConflictException(Throwable cause) {
        super(cause);
    }
}
