package org.multiverse.utils.profiling;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for preparing and exposing the information in {@link ProfileRepository ProfileRepositories} in a readable
 * form.
 *
 * @author Andrew Phillips
 */
public interface ProfileCollator {

    /**
     * @return the names of all available profiles
     */
    Set<String> getProfileNames();

    /**
     * Profile data for transactions in a family.
     */
    public interface ProfilePage {

        /**
         * @return the name of the profile page
         */
        String getName();

        /**
         * @return the labels (names) of the collected statistics
         */
        List<String> getStatLabels();

        /**
         * @return a map from keys to the statistics for that key. What a key is will depend on the type of profile
         *         page; an example might be a transaction ID. The number and sequence of the values of the statistics
         *         must match the number and sequence of the labels returned by {@link #getStatLabels()}
         */
        Map<Object, List<Long>> getStatistics();
    }

    /**
     * @param profileName the name for the profile page to be retrieved
     * @return the named profile page, or {@code null} if no profile of that name exists
     *
     * @see #getProfilePages()
     */
    ProfilePage getProfilePage(String profileName);

    /**
     * @return all known profile pages
     *
     * @see #getProfilePage(String)
     */
    Set<ProfilePage> getProfilePages();
}
