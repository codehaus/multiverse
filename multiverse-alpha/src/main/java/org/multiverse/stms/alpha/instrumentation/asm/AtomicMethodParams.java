package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.api.PropagationLevel;

/**
 * A container for all parameters passed to an atomic method.
 *
 * @author Peter Veentjer.
 */
public class AtomicMethodParams {

    public boolean readOnly;

    public String familyName;

    public int retryCount;

    public PropagationLevel propagationLevel;
}
