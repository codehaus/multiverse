package org.multiverse.stms.alpha.instrumentation.metadata;

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

    public TransactionMetadata merge(TransactionMetadata other) {
        TransactionMetadata result = new TransactionMetadata();
        result.readOnly = readOnly || other.readOnly;
        result.maxRetryCount = Math.max(maxRetryCount, other.maxRetryCount);
        result.automaticReadTracking = automaticReadTracking || other.automaticReadTracking;
        result.interruptible = interruptible || other.interruptible;
        result.smartTxLengthSelector = smartTxLengthSelector || other.smartTxLengthSelector;
        result.allowWriteSkewProblem = allowWriteSkewProblem && other.allowWriteSkewProblem;
        return result;
    }
}
