package org.multiverse.api.exceptions;

/**
 * An {@link Error} that indicates a failure while doing a {@link org.multiverse.api.Transaction#commit()}.
 * <p/>
 * A WriteConflict in most cases can be solved by retrying the transaction.
 *
 * @author Peter Veentjer
 */
public class WriteConflict extends StmControlFlowError {

    private static final long serialVersionUID = 0;

    public WriteConflict() {
    }

    public WriteConflict(String message) {
        super(message);
    }

    public WriteConflict(String message, Throwable cause) {
        super(message, cause);
    }

    public WriteConflict(Throwable cause) {
        super(cause);
    }
}
