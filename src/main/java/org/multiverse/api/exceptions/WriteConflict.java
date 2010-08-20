package org.multiverse.api.exceptions;

/**
 * An {@link ControlFlowError} that indicates a writeconflict.
 * <p/>
 * A WriteConflict in most cases can be solved by retrying the transaction.
 * <p/>
 * An instance is available for preventing creating exception objects that are only used for control flow.
 *
 * @author Peter Veentjer
 */
public class WriteConflict extends ControlFlowError {

    public static final WriteConflict INSTANCE = new WriteConflict();
}
