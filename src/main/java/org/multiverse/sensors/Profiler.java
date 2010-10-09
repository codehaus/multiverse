package org.multiverse.sensors;

import org.multiverse.api.TransactionConfiguration;

/**
 * @author Peter Veentjer.
 */
public interface Profiler {

    /**
     * @param config
     * @return
     */
    TransactionSensor getTransactionSensor(TransactionConfiguration config);
}
