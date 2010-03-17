package org.multiverse.instrumentation.metadata;

import java.util.concurrent.TimeUnit;

/**
 * A container for all parameters passed to an transactional method.
 *
 * @author Peter Veentjer.
 */
public class TransactionMetadata {

    public boolean readOnly;

    public String familyName;

    public int maxRetryCount;

    public boolean automaticReadTracking;

    public boolean interruptible;

    public boolean smartTxLengthSelector;

    public boolean allowWriteSkewProblem;

    public long timeout;

    public TimeUnit timeoutTimeUnit;
}
