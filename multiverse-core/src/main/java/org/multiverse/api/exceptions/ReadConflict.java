package org.multiverse.api.exceptions;

/**
 * An {@link Error} that indicates that a load a transactional object using a transaction failed. See
 * the subclasses of this error for more details about these situations.
 * <p/>
 * A WriteConflict can in most cases be solved by retrying the transaction.
 *
 * @author Peter Veentjer.
 */
public abstract class ReadConflict extends ControlFlowError {

    private static final long serialVersionUID = 0;

    public ReadConflict() {
    }

    public ReadConflict(String message) {
        super(message);
    }

    public ReadConflict(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadConflict(Throwable cause) {
        super(cause);
    }
}
