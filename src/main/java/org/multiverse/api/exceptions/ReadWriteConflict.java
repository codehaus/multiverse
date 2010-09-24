package org.multiverse.api.exceptions;

/**
 * An {@link Error} that indicates that a load a transactional object using a transaction failed. See
 * the ControlFlowError of this error for more details about these situations.
 * <p/>
 * A ReadWriteConflict can in most cases be solved by retrying the transaction.
 *
 * @author Peter Veentjer.
 */
public class ReadWriteConflict extends ControlFlowError {

    public final static ReadWriteConflict INSTANCE = new ReadWriteConflict();

    /**
     * Creates a new ReadWriteConflict.
     */
    public ReadWriteConflict() {
    }

    /**
     * Creates a new ReadWriteConflict.
     *
     * @param message the message of the ReadWriteConflict.
     */
    public ReadWriteConflict(String message) {
        super(message);
    }

    /**
     * Creates a new ReadWriteConflict.
     *
     * @param message the message of the ReadWriteConflict.
     * @param cause   the cause of the ReadWriteConflict.
     */
    public ReadWriteConflict(String message, Throwable cause) {
        super(message, cause);
    }
}
