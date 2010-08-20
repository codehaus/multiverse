package org.multiverse.api.exceptions;

/**
 * An {@link Error} that indicates that a load a transactional object using a transaction failed. See
 * the ControlFlowError of this error for more details about these situations.
 * <p/>
 * A ReadConflict can in most cases be solved by retrying the transaction.
 *
 * @author Peter Veentjer.
 */
public class ReadConflict extends ControlFlowError {

    public final static ReadConflict INSTANCE = new ReadConflict();

    /**
     * Creates a new ReadConflict.
     */
    public ReadConflict() {
    }

    /**
     * Creates a new ReadConflict.
     *
     * @param message the message of the ReadConflict.
     */
    public ReadConflict(String message) {
        super(message);
    }

    /**
     * Creates a new ReadConflict.
     *
     * @param message the message of the ReadConflict.
     * @param cause the cause of the ReadConflict.
     */
    public ReadConflict(String message, Throwable cause) {
        super(message, cause);
    }
}
