package org.multiverse.sensors;

/**
 * A Sensor is responsible for 'measuring' certain behavior.
 *
 * @author Peter Veentjer
 */
public interface Sensor {

    /**
     * Returns a description of the sensor (so that is it measuring).
     *
     * @return the description.
     */
    String getDescription();
}
