package org.multiverse.sensors;

import org.junit.Test;
import org.multiverse.api.TransactionConfiguration;

import java.util.concurrent.ConcurrentHashMap;

public class SimpleProfiler implements Profiler {

    private final ConcurrentHashMap<TransactionConfiguration, TransactionSensor> sensorMap
            = new ConcurrentHashMap<TransactionConfiguration, TransactionSensor>();
    private final PrintThread printThread = new PrintThread();

    @Override
    public TransactionSensor getTransactionSensor(TransactionConfiguration configuration) {
        if (configuration == null) {
            throw new NullPointerException();
        }

        TransactionSensor sensor = sensorMap.get(configuration);
        if (sensor == null) {
            TransactionSensor newCounter = new TransactionSensor(configuration);
            TransactionSensor existingCounter = sensorMap.putIfAbsent(configuration, newCounter);
            sensor = existingCounter != null ? existingCounter : newCounter;
        }

        return sensor;
    }

    @Test
    public void startPrintingDaemon() {
        printThread.start();
    }

    class PrintThread extends Thread {
        public PrintThread() {
            super("PrintThread");
            setDaemon(true);
        }

        public void run() {
            while (true) {
                System.out.println("------------------ Profiling ----------------------------");
                for (TransactionSensor sensor : sensorMap.values()) {
                    System.out.println(sensor);
                }

                try {
                    sleep(1000);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }
}
