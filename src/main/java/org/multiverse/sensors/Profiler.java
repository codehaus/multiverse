package org.multiverse.sensors;

import org.multiverse.api.TransactionConfiguration;

public interface Profiler {

    TransactionSensor getTransactionSensor(TransactionConfiguration config);
}
