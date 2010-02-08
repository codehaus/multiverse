package org.benchy;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A testcase contains all the variable parameters for a benchmark.
 * <p/>
 * The same testcase can be used with different benchmarks.
 *
 * @author Peter Veentjer.
 */
public class TestCase {

    private final Properties properties = new Properties();

    public TestCase() {
        setProperty("runCount", 1);
        setProperty("warmupRunCount", 1);
    }

    public int getRunCount() {
        return getIntProperty("runCount");
    }

    public void setRunCount(int runCount) {
        setProperty("runCount", runCount);
    }

    public int getWarmupRunCount() {
        return getIntProperty("warmupRunCount");
    }

    public void setWarmupRunCount(int warmupRunCount) {
        setProperty("warmupRunCount", warmupRunCount);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperty(String name, Object value) {
        properties.put(name, value.toString());
    }

    public String getProperty(String name) {
        return (String) properties.get(name);
    }

    public String getProperty(String name, String defaultValue) {
        String result = (String) properties.get(name);
        return result == null ? defaultValue : name;
    }

    public String getPropertiesDescription() {
        Map<String, String> map = new HashMap<String, String>();

        for (String name : properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }

        return map.toString();
    }

    public int getIntProperty(String name) {
        String value = properties.getProperty(name);

        if (value == null) {
            throw new IllegalArgumentException("property with name " + name + " is not found");
        }

        return Integer.parseInt(value);
    }

    public int getIntProperty(String name, int defaultValue) {
        String value = properties.getProperty(name, "" + defaultValue);
        return Integer.parseInt(value);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = properties.getProperty(name, "" + defaultValue);
        return Boolean.parseBoolean(value);
    }

    public long getLongProperty(String name, long defaultValue) {
        String value = properties.getProperty(name, "" + defaultValue);
        return Long.parseLong(value);
    }

    public long getLongProperty(String name) {
        String value = properties.getProperty(name);

        if (value == null) {
            throw new IllegalArgumentException("property with name " + name + " is not found");
        }

        return Long.parseLong(value);
    }

    public boolean getBooleanProperty(String name) {
        String value = properties.getProperty(name);

        if (value == null) {
            throw new IllegalArgumentException("property with name " + name + " is not found");
        }

        return Boolean.parseBoolean(value);
    }
}
