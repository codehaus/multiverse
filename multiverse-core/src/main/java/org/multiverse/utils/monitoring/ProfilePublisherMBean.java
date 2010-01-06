package org.multiverse.utils.monitoring;

import org.multiverse.utils.profiling.ProfileCollator;

/**
 * Exposes some of the information made available via the {@link ProfileCollator}
 * via JMX.
 *  
 * @author Andrew Phillips
 */
public interface ProfilePublisherMBean {
    /**
     * @return an array of the names of all the available profiles
     */
    String[] getProfileNames();
    
    /**
     * @param profileName the name of the desired profile
     * @return a printable representation of the named profile
     */
    String getStatistics(String profileName);
}