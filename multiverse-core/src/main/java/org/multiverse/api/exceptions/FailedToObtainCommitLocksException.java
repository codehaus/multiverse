package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link CommitFailureException} that indicates that the locks could not be acquired while doing a {@link
 * org.multiverse.api.Transaction#commit}.
 *
 * @author Peter Veentjer
 */
public class FailedToObtainCommitLocksException extends CommitFailureException
        implements RecoverableThrowable {

    private static final long serialVersionUID = 0;

    public final static FailedToObtainCommitLocksException INSTANCE = new FailedToObtainCommitLocksException();

    public final static boolean reuse = parseBoolean(getProperty(
            FailedToObtainCommitLocksException.class.getName() + ".reuse", "true"));

    public FailedToObtainCommitLocksException() {
    }

    public FailedToObtainCommitLocksException(String message) {
        super(message);
    }

    public FailedToObtainCommitLocksException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedToObtainCommitLocksException(Throwable cause) {
        super(cause);
    }


}
