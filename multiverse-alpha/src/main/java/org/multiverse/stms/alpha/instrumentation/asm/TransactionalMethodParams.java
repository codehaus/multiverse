package org.multiverse.stms.alpha.instrumentation.asm;

/**
 * A container for all parameters passed to an transactional method.
 *
 * @author Peter Veentjer.
 */
public class TransactionalMethodParams {

    public boolean readOnly;

    public String familyName;

    public int retryCount;

    public boolean automaticReadTracking;

    public boolean interruptible;

    public boolean smartTxLengthSelector;

    public boolean detectWriteSkew;
}
