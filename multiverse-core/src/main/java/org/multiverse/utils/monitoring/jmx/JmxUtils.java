package org.multiverse.utils.monitoring.jmx;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Registers MBeans.
 */
public abstract class JmxUtils {

    private static final MBeanServer PLATFORM_SERVER =
            ManagementFactory.getPlatformMBeanServer();

    /**
     * Registers an MBean. The object name is generated according to {@code <mean.package>:type=<mbean.type>}, e.g.
     * {@code org.multiverse.utils.monitoring.jmx:type=JmxUtils}.
     *
     * @param mbean the MBean to registerLifecycleListener
     * @return the name under which the MBean was registered
     */
    public static String registerMBean(Object mbean) {
        String name = getDefaultMBeanName(mbean.getClass());
        try {
            PLATFORM_SERVER.registerMBean(mbean, new ObjectName(name));
            return name;
        } catch (JMException exception) {
            throw new IllegalArgumentException(String.format(
                    "Unable to register MBean '%s' under name '%s' due to: %s",
                    mbean, name, exception.getMessage()));
        }
    }

    private static String getDefaultMBeanName(Class<?> mbeanClass) {
        String classNameWithoutPackage = mbeanClass.getSimpleName();
        return String.format("%s:type=%s",
                             mbeanClass.getName().replace("." + classNameWithoutPackage, ""),
                             classNameWithoutPackage);
    }
}
