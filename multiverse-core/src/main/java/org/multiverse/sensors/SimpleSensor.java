package org.multiverse.sensors;

/**
 * @author Peter Veentjer
 */
public final class SimpleSensor implements Sensor {

    private final String description;

    public SimpleSensor(String description) {
        if (description == null) {
            throw new NullPointerException();
        }
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
