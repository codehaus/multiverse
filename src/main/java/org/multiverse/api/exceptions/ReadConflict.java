package org.multiverse.api.exceptions;

/**
 * @author Peter Veentjer
 */
public class ReadConflict extends ControlFlowError {

    public final static ReadConflict INSTANCE = new ReadConflict();
}
