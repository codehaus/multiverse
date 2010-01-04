package org.multiverse.utils.monitoring;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.multiverse.utils.profiling.ProfileCollator;
import org.multiverse.utils.profiling.ProfileCollator.ProfilePage;

public class ProfilePublisher implements ProfilePublisherMBean {
    private static final String UNKNOWN_PROFILE_TEMPLATE = "<no profile named '%s' found>";
    
    private final ProfileCollator collator;
    
    public ProfilePublisher(ProfileCollator collator) {
        this.collator = collator;
    }
    
    @Override
    public String[] getProfileNames() {
        // collator.getProfileNames should never return null
        return collator.getProfileNames().toArray(new String[0]);
    }

    @Override
    public String getStatistics(String profileName) {
        ProfilePage page = collator.getProfilePage(profileName);
        return ((page == null) ? String.format(UNKNOWN_PROFILE_TEMPLATE, profileName)
                               : getStatistics(page));
    }
    
    private String getStatistics(ProfilePage page) {
        return page.toString();
    }
    
    /**
     * A profile page that will display as follows:
     * <pre>
     * '&lt;profileName&gt;'
     * &lt;key&gt; &lt;key&gt; ... | &lt;hdr&gt; &lt;hdr&gt; &lt;hdr&gt; ...
     * ---------------------------------
     * &lt;id&gt;   &lt;id&gt; ... | &lt;val&gt; &lt;val&gt; &lt;val&gt; ...
     * ...
     * 
     * &lt;hdr&gt; = &lt;full header name&gt;
     * ...
     * </pre>
     * Implementing classes are responsible for appropriately identifying and
     * displaying the key.
     * 
     * @param <K> the type of the key
     */
    public abstract static class KeyedProfilePage<K> implements ProfilePage {
        private static final String NULL_VALUE_SYMBOL = "-";
        private static final int DEFAULT_VALUE_COLUMN_WIDTH = 6;
        private final String name;
        private final List<String> statLabels;
        private final Map<K, List<Long>> statistics;
        private final int valueColumnWidth;
        
        public KeyedProfilePage(String name, List<String> statLabels,
                Map<K, List<Long>> statistics) {
            this(name, statLabels, statistics, DEFAULT_VALUE_COLUMN_WIDTH);
        }

        public KeyedProfilePage(String name, List<String> statLabels,
                Map<K, List<Long>> statistics, int valueColumnWidth) {
            this.name = name;
            this.statLabels = statLabels;
            this.statistics = statistics;
            this.valueColumnWidth = valueColumnWidth;
        }
        
        @Override
        public String getName() {
            return name;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<Object, List<Long>> getStatistics() {
            return (Map<Object, List<Long>>) statistics;
        }

        @Override
        public List<String> getStatLabels() {
            return statLabels;
        }

        protected abstract String getFormattedKeyLabels();
        protected abstract String toFormattedString(K key);

        @Override
        public String toString() {
            StringBuilder stats = new StringBuilder();
            stats.append("'").append(name).append("'\n");
            
            // <key> <key> ... | <hdr> <hdr> <hdr> ...
            String keyLabels = getFormattedKeyLabels();
            stats.append(keyLabels).append(" | ");
            final int keyLabelColumnWidth = keyLabels.length();
            // not forgetting the " | "!
            int headerRowWidth = keyLabelColumnWidth + 3;
            
            for (String statLabel : statLabels) {
                stats.append(String.format("%" + valueColumnWidth + "s ", 
                             truncate(statLabel, valueColumnWidth)));
                // label and a space
                headerRowWidth += valueColumnWidth + 1; 
            }
            stats.append("\n");
            
            // ---------------------------------
            for (int i = 0; i < headerRowWidth; i++) {
                stats.append('-');
            }
            stats.append("\n");
            
            /* 
             *  <id>  <id> ... | <val> <val> <val> ...
             * ...
             */
            for (Entry<K, List<Long>> statValues : statistics.entrySet()) {
                stats.append(toFormattedString(statValues.getKey())).append(" | ");
                for (Long statValue : statValues.getValue()) {
                    stats.append(String.format("%" + valueColumnWidth + "s ",
                            (statValue == null) 
                            ? NULL_VALUE_SYMBOL 
                            : truncate(statValue.toString(), valueColumnWidth)));                
                }
                stats.append("\n");
            }
            stats.append("\n");
            
            /*
             * <hdr> = <full header name>
             * ...
             */
            for (String statLabel : statLabels) {
                stats.append(String.format("%" + valueColumnWidth + "s = %s%n", 
                        truncate(statLabel,valueColumnWidth), statLabel));            
            }
            return stats.toString();
        }

        protected static String truncate(String original, int length) {
            return ((original.length() <= length) ? original 
                                                  : original.substring(0, length));
        }
    }
    
    /**
     * Writes statistics for all transaction families to a stream.
     * 
     * @param stream the stream to write to
     */
    public void writeStatisticsToStream(OutputStream stream) {
        PrintStream printStream = new PrintStream(stream);
        for (ProfilePage page : collator.getProfilePages()) {
            printStream.println(getStatistics(page));
        }
    }
}
