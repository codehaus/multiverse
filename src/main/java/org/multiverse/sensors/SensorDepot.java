package org.multiverse.sensors;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In the SensorDepot all sensor information is stored.
 *
 * @author Peter Veentjer
 */
public final class SensorDepot {

    private final ConcurrentMap<Sensor, Object> sensorMap = new ConcurrentHashMap<Sensor, Object>();

    public void register(Sensor sensor) {
        if (sensor == null) {
            throw new NullPointerException();
        }

        sensorMap.put(sensor, null);
    }

    class PrintStatisticsThread extends Thread {

        public PrintStatisticsThread() {
            super("PrintStatistics");
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
        }

        public void run() {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                //ignore
            }

            for (Sensor sensor : sensorMap.keySet()) {
                System.out.println(sensor);
            }
        }
    }
}
