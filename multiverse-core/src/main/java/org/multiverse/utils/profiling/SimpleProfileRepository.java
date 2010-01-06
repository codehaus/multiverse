package org.multiverse.utils.profiling;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple {@link ProfileRepository} implementation.
 * <p/>
 * Improvements needed:
 * <ol>
 * <li>composedkey is created even for lookup. Object creation is slow so this unwanted
 * object creation should be removed</li>
 * <li>The ConcurrentHashMap still needs locking (even though it used striped locks), so
 * perhaps a non blocking version could improve performance</li>
 * </ol>
 *
 * @author Peter Veentjer.
 */
public final class SimpleProfileRepository implements ProfileRepository {

    private final ConcurrentMap<ComposedKey, AtomicLong> map = new ConcurrentHashMap<ComposedKey, AtomicLong>();
    private final ConcurrentMap<String, AtomicLong> singleKeyMap = new ConcurrentHashMap<String, AtomicLong>();
    private final ProfileCollator collator = new SimpleProfileRepositoryCollator(map);

    @Override
    public void incCounter(String key) {
       incCounter(key, 1);
    }

    @Override
    public void incCounter(String key, int amount) {
       if(key == null){
            throw new NullPointerException();
        }

        getOrCreateCounter(key).addAndGet(amount);
    }

    private AtomicLong getOrCreateCounter(String key) {
        AtomicLong counter = singleKeyMap.get(key);
        if (counter == null) {
            counter = new AtomicLong();
            AtomicLong found = singleKeyMap.putIfAbsent(key, counter);
            if (found != null) {
                counter = found;
            }
        }

        return counter;
    }

    @Override
    public long getCount(String key){
        if(key == null){
            throw new NullPointerException();
        }

        AtomicLong counter = singleKeyMap.get(key);
        return counter == null?-1:counter.get();
    }

    @Override
    public void reset() {
        map.clear();
        singleKeyMap.clear();
    }

    @Override
    public void incCounter(String key1, String key2) {
        incCounter(key1, key2, 1);
    }

    @Override
    public void incCounter(String key1, String key2, long count) {
        AtomicLong counter = getOrCreateCounter(key1, key2);
        counter.addAndGet(count);
    }

    @Override
    public Iterator<String> getKey1Iterator() {
        throw new RuntimeException();
    }

    @Override
    public long getCount(String key1, String key2) {
        if(key1 == null || key2 == null){
            throw new NullPointerException();
        }

        AtomicLong counter = getOrCreateCounter(key1, key2);
        return counter == null ? -1 : counter.get();
    }

    @Override
    public void decCounter(String familyName, String key2) {
        incCounter(familyName, key2, -1);
    }

    @Override
    public long sumKey2(String key2) {
        long result = 0;

        for (Map.Entry<ComposedKey, AtomicLong> entry : map.entrySet()) {
            if (entry.getKey().key2.equals(key2)) {
                result += entry.getValue().get();
            }
        }

        return result;
    }

    @Override
    public long sumKey1(String key1) {
        long result = 0;

        for (Map.Entry<ComposedKey, AtomicLong> entry : map.entrySet()) {
            if (entry.getKey().key1.equals(key1)) {
                result += entry.getValue().get();
            }
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.multiverse.utils.profiling.ProfileRepository#getCollator()
     */
    @Override
    public ProfileCollator getCollator() {
        return collator;
    }

    @Override
    public String toString(){
        return map.toString() + ", " + singleKeyMap.toString();
    }
    
    private AtomicLong getOrCreateCounter(String key1, String key2) {
        ComposedKey key = new ComposedKey(key1, key2);
        AtomicLong counter = map.get(key);
        if (counter == null) {
            counter = new AtomicLong();
            AtomicLong found = map.putIfAbsent(key, counter);
            if (found != null) {
                counter = found;
            }
        }

        return counter;
    }

    //todo: this composed key is very expensive because an extra object is created everytime
    //the clock needs to be increased.
    static class ComposedKey {
        final String key1;
        final String key2;

        ComposedKey(String key1, String key2) {
            this.key1 = key1;
            this.key2 = key2;
        }

        @Override
        public String toString() {
            return key1 + "#" + key2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ComposedKey that = (ComposedKey) o;

            if (key1 != null ? !key1.equals(that.key1) : that.key1 != null) return false;
            if (key2 != null ? !key2.equals(that.key2) : that.key2 != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key1 != null ? key1.hashCode() : 0;
            result = 31 * result + (key2 != null ? key2.hashCode() : 0);
            return result;
        }
    }
}
