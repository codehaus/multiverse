package org.multiverse.utils.profiling;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.multiverse.utils.profiling.SimpleProfileRepository.ComposedKey;

public class SimpleProfileRepositoryPrinter {

    public static String toPrettyString(SimpleProfileRepository repository) {
        StringBuilder pretty = new StringBuilder("Profiler data\n=============\n");
        Comparator<Entry<String, AtomicLong>> sortOnKey = 
            new Comparator<Entry<String,AtomicLong>>() {
                @Override
                public int compare(Entry<String, AtomicLong> o1,
                        Entry<String, AtomicLong> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                };        
        appendPrettyString(pretty, getSingleKeys(repository), sortOnKey, "Single keys");
        Comparator<Entry<ComposedKey, AtomicLong>> sortOnKey2ThenKey1 = 
            new Comparator<Entry<ComposedKey,AtomicLong>>() {
                @Override
                public int compare(Entry<ComposedKey, AtomicLong> o1,
                        Entry<ComposedKey, AtomicLong> o2) {
                        int compareKey2s = o1.getKey().key2.compareTo(o2.getKey().key2);
                        return (compareKey2s != 0) ? compareKey2s 
                                                   : o1.getKey().key1.compareTo(o2.getKey().key1);
                    }
                };
        appendPrettyString(pretty, getComposedKeys(repository), sortOnKey2ThenKey1, "Composed keys", 
                new ToStringTransformer<ComposedKey>() {
                    @Override
                    public String toString(ComposedKey composed) {
                        return String.format("(%s) %s", composed.key2, composed.key1);
                    }
                });
        return pretty.toString();
    }
    
    private static interface ToStringTransformer<T> {
        String toString(T obj);
    }
    
    private static <K, V> StringBuilder appendPrettyString(StringBuilder builder, Map<K, V> map, 
            Comparator<Entry<K, V>> entryComparator, String title) {
        return appendPrettyString(builder, map, entryComparator, title, 
                new ToStringTransformer<K>() {
                    @Override
                    public String toString(K obj) {
                        return obj.toString();
                    }
                });
    }
    
    private static <K, V> StringBuilder appendPrettyString(StringBuilder builder, Map<K, V> map, 
            Comparator<Entry<K, V>> entryComparator, String title, ToStringTransformer<K> keyToString) {
        if (map.isEmpty()) {
            builder.append("<no ").append(title.toLowerCase()).append(">\n");
        } else {
            builder.append(title).append("\n").append(title.replaceAll(".", "-")).append("\n");
            List<Entry<K, V>> sortedEntries = new ArrayList<Entry<K, V>>(map.entrySet()); 
            Collections.sort(sortedEntries, entryComparator);
            for (Entry<K, V> entry : sortedEntries) {
                builder.append(keyToString.toString(entry.getKey())).append(" = ")
                .append(entry.getValue()).append("\n");
            }
        }
        return builder.append("\n");
    }
    
    private static Map<String, AtomicLong> getSingleKeys(SimpleProfileRepository repository) {
        return getFieldValue(repository, "singleKeyMap");
    }
    
    private static Map<ComposedKey, AtomicLong> getComposedKeys(SimpleProfileRepository repository) {
        return getFieldValue(repository, "map");
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(SimpleProfileRepository repository, String fieldName) {
        try {
            Field field = SimpleProfileRepository.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(repository);
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                        "Unable to get value for field %s of target %s due to: ", fieldName, repository),
                    exception);
        }
    }
}
