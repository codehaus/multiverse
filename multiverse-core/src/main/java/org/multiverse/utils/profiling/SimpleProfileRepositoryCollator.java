package org.multiverse.utils.profiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.multiverse.utils.monitoring.ProfilePublisher.KeyedProfilePage;
import org.multiverse.utils.profiling.SimpleProfileRepository.ComposedKey;

/**
 * A collator for a {@link SimpleProfileRepository}.
 * <p>
 * Can produce inconsistent results as it doesn't work with an unchanging
 * snapshot of the underlying repository, which may change between e.g.
 * a call to {@code getProfileNames()} and {@code getProfilePage(profileName)}
 * 
 * @author Andrew Phillips
 */
class SimpleProfileRepositoryCollator implements ProfileCollator {
    /*
     * This class contains rather inelegant and messy code, but that's mainly
     * because it has to "adapt" to the structure of the SimpleProfileRepository.
     * In the future, the repository's internal structure should make it easier
     * to collect the type of information required here, so making this code
     * nicer isn't really a priority.
     */
    private final ConcurrentMap<ComposedKey, AtomicLong> repositoryStats;
    
    public SimpleProfileRepositoryCollator(
            ConcurrentMap<ComposedKey, AtomicLong> repositoryStats) {
        this.repositoryStats = repositoryStats;
    }

    @Override
    public Set<String> getProfileNames() {
        Set<String> profileNames = new HashSet<String>();
        
        /*
         * Two types of pages: summaries for atomic objects (key2 like 
         * "org.multiverse.TransactionalObject") or collating stats for methods
         * in atomic objects (key2 like "org/multiverse/TransactionalObject.method..."
         */
        for (ComposedKey key : repositoryStats.keySet()) {
            String candidateKey = key.key2;
            if (isAtomicObjectKey(candidateKey)) {
                profileNames.add(AtomicObjectProfilePage.PROFILE_PREFIX + candidateKey);
            } else if (isAOTransactionKey(candidateKey)) {
                profileNames.add(AOTransactionProfilePage.PROFILE_PREFIX 
                        + getAOTransactionKey(candidateKey));
            }
        }
        return profileNames;
    }
    
    private static String getAOTransactionKey(String key) {
        return key.substring(0, key.lastIndexOf('.'));    
    }

    // rather crude checks here
    private static boolean isAtomicObjectKey(String candidateKey) {
        return ((candidateKey != null) && candidateKey.contains(".")
                && !candidateKey.contains("/"));
    }

    private static boolean isAOTransactionKey(String candidateKey) {
        return ((candidateKey != null) && candidateKey.contains("/"));
    }

    @Override
    public ProfilePage getProfilePage(String profileName) {
        if (isAtomicObjectKey(profileName)) {
            return getAtomicObjectPage(profileName);
        } else if (isAOTransactionKey(profileName)) {
            return getAOTransactionPage(profileName);
        }
        
        return null;
    }

    private ProfilePage getAtomicObjectPage(String profileName) {
        String atomicObjectKey = profileName.replace(
                AtomicObjectProfilePage.PROFILE_PREFIX, "");
        SortedMap<String, Long> atomicObjectStats = new TreeMap<String, Long>();
        
        // collect the key1's, i.e. stats, for entries for this atomic object
        for (Entry<ComposedKey, AtomicLong> entry : repositoryStats.entrySet()) {
            ComposedKey key = entry.getKey();
            if (key.key2.equals(atomicObjectKey)) {
                atomicObjectStats.put(key.key1, entry.getValue().longValue());
            }
        }
        
        return (!atomicObjectStats.isEmpty() 
                ? new AtomicObjectProfilePage(profileName, 
                        new ArrayList<String>(atomicObjectStats.keySet()),
                        new ArrayList<Long>(atomicObjectStats.values()))
                : null);
    }

    private ProfilePage getAOTransactionPage(String profileName) {
        String aoTransactionsKey = profileName.replace(
                AOTransactionProfilePage.PROFILE_PREFIX, "");
        // all labels encountered
        SortedSet<String> statLabels = new TreeSet<String>();
        // map from page key (i.e. atomic object method) to named stats for that key
        Map<String, Map<String, Long>> aoTransactionsStats = 
            new HashMap<String, Map<String, Long>>();
        
        for (Entry<ComposedKey, AtomicLong> entry : repositoryStats.entrySet()) {
            ComposedKey key = entry.getKey();
            String candidateKey = key.key2;
            if (candidateKey.startsWith(aoTransactionsKey)) {
                String label = key.key1;
                statLabels.add(label);
                addStatistic(aoTransactionsStats, getMethodKey(candidateKey), label, 
                        entry.getValue().longValue());
            }
        }
        
        return (!aoTransactionsStats.isEmpty() 
                ? new AOTransactionProfilePage(profileName, new ArrayList<String>(statLabels), 
                        toStatValues(statLabels, aoTransactionsStats))
                : null);
    }

    private static String getMethodKey(String key) {
        return key.substring(key.lastIndexOf('.') + 1);
    }

    private static void addStatistic(Map<String, Map<String, Long>> statistics, 
            String key, String label, long value) {
        if (!statistics.containsKey(key)) {
            statistics.put(key, new HashMap<String, Long>());
        }
        statistics.get(key).put(label, value);
    }

    private static Map<String, List<Long>> toStatValues(SortedSet<String> statLabels, 
            Map<String, Map<String, Long>> aoTransactionsStats) {
        /*
         * aoTransactionsStats' values are maps of statLabel -> statValue, but
         * may not contains values for *all* the lables
         */
        Map<String, List<Long>> statValues = new HashMap<String, List<Long>>();
        for (Entry<String, Map<String, Long>> statMap : aoTransactionsStats.entrySet()) {
            statValues.put(statMap.getKey(), toStatList(statLabels, statMap.getValue()));
        }
        return statValues;
    }

    private static List<Long> toStatList(SortedSet<String> statLabels,
            Map<String, Long> statValueMap) {
        List<Long> statValues = new ArrayList<Long>(statLabels.size());
        for (String statLabel : statLabels) {
            // will add null for missing values, as intended 
            statValues.add(statValueMap.get(statLabel));
        }
        return statValues;
    }

    @Override
    public Set<ProfilePage> getProfilePages() {
        // XXX: horribly inefficient - can be done in one pass
        Set<ProfilePage> pages = new HashSet<ProfilePage>();
        for (String profileName : getProfileNames()) {
            pages.add(getProfilePage(profileName));
        }
        return pages;
    }
    
    /**
     * A profile page whose keys are simply the atomic objects profiled.
     */
    private static class AtomicObjectProfilePage extends KeyedProfilePage<String> {
        private static final String PROFILE_PREFIX = "atomicObject=";
        private static final String ATOMIC_OBJECT_LABEL = "TransactionalObject";
        
        private AtomicObjectProfilePage(String name, List<String> statLabels,
                List<Long> statValues) {
            // the profile name (atomic object type) *is* the key
            super(name, statLabels, singleKeyStats(
                    name.replace(PROFILE_PREFIX, ""), statValues));
        }
        
        private static Map<String, List<Long>> singleKeyStats(String key, List<Long> statValues) {
            Map<String, List<Long>> stats = new HashMap<String, List<Long>>();
            stats.put(key, statValues);
            return stats;
        }

        @Override
        protected String getFormattedKeyLabels() {
            return ATOMIC_OBJECT_LABEL;
        }

        @Override
        protected String toFormattedString(String key) {
            return truncate(key, ATOMIC_OBJECT_LABEL.length());
        }
    }
    
    /**
     * A profile page whose keys are methods of an atomic object.
     */
    private static class AOTransactionProfilePage extends KeyedProfilePage<String> {
        private static final String PROFILE_PREFIX = "aoTransactions=";
        private static final String METHOD_LABEL = "Method";
        
        public AOTransactionProfilePage(String name, List<String> statLabels,
                Map<String, List<Long>> statistics) {
            super(name, statLabels, statistics);
        }
        
        @Override
        protected String getFormattedKeyLabels() {
            return METHOD_LABEL;
        }

        @Override
        protected String toFormattedString(String key) {
            int typeMethodSeparatorIndex = key.lastIndexOf('.');
            return truncate(key.substring(typeMethodSeparatorIndex + 1), 
                            METHOD_LABEL.length());
        }
    }        
}
