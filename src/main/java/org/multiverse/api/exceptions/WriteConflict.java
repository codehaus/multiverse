package org.multiverse.api.exceptions;

/**
 * @author Peter Veentjer
 */
public class WriteConflict extends ControlFlowError {

    public static final WriteConflict INSTANCE = new WriteConflict();
}
