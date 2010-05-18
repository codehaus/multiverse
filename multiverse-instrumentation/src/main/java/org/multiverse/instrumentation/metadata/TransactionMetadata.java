package org.multiverse.instrumentation.metadata;

import org.multiverse.api.LogLevel;

/**
 * A container for all parameters passed to an transactional method.
 *
 * @author Peter Veentjer.
 */
public class TransactionMetadata {

    /**
     * Indicates if a Transaction is readonly.
     * <p/>
     * True: Readonly
     * False: Not readonly
     * Null: Speculative readonly
     */
    public Boolean readOnly;

    //todo: needs to be transformed to Object boolean
    public boolean writeSkew;

    public Boolean trackReads;

    public Boolean interruptible;

    public String familyName;

    public int maxRetries;

    public boolean speculativeConfigurationEnabled;

    public long timeoutNs;

    public LogLevel logLevel;
}
