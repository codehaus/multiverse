package org.multiverse.api.exceptions;

/**
 * An {@link ControlFlowError} that indicates that a load a transactional object using a transaction failed.
 * See the ControlFlowError of this error for more details about these situations.
 * <p/>
 * A ReadWriteConflict can in most cases be solved by retrying the transaction.
 *
 * @author Peter Veentjer.
 */
public class ReadWriteConflict extends ControlFlowError {

    private static final long serialVersionUID = 0;

    public final static ReadWriteConflict INSTANCE = new ReadWriteConflict(false);

    private final boolean fillStackTrace;

    /**
     * Creates a new ReadWriteConflict.
     */
    public ReadWriteConflict(boolean fillStackTrace) {
        this.fillStackTrace = fillStackTrace;
    }

    /**
     * Creates a new ReadWriteConflict.
     *
     * @param message the message of the ReadWriteConflict.
     */
    public ReadWriteConflict(String message) {
        super(message);
        fillStackTrace = true;
    }

    /**
     * Creates a new ReadWriteConflict.
     *
     * @param message the message of the ReadWriteConflict.
     * @param cause   the cause of the ReadWriteConflict.
     */
    public ReadWriteConflict(String message, Throwable cause) {
        super(message, cause);
        fillStackTrace = true;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        if (fillStackTrace) {
            return super.getStackTrace();
        } else {
            return new StackTraceElement[0];
        }
    }
}
