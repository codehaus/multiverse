package org.multiverse.api.exceptions;

/**
 * @author Peter Veentjer
 */
public class SpeculativeConfigurationError extends ControlFlowError {
    public final static SpeculativeConfigurationError INSTANCE = new SpeculativeConfigurationError();
}
