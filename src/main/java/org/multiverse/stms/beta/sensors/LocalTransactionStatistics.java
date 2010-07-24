package org.multiverse.stms.beta.sensors;

import org.multiverse.api.TransactionFactory;


/**
 * Local statistics for transaction. Need to be published to the TransactionSensor for this information to
 * come available. A big advantage of using a non thread safe object to store information, is that the cost
 * of publishing is reduced.
 */
public class LocalTransactionStatistics {
    public long commitCount;
    public long abortCount;
    public long startedCount;
    public TransactionFactory factory;
}
