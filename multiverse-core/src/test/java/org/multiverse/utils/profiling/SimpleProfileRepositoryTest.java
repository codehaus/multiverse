package org.multiverse.utils.profiling;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Peter Veentjer
 */
public class SimpleProfileRepositoryTest {

    private SimpleProfileRepository repository;

    @Before
    public void setUp() {
        repository = new SimpleProfileRepository();
    }

    // ================ getCounter(key) ===============================

    @Test(expected = NullPointerException.class)
    public void getCount_key_nullKeyFails() {
        repository.getCount(null);
    }

    @Test
    public void getCount_key_nonExisting() {
        long result = repository.getCount("somenonexistingkey");
        assertEquals(-1, result);
    }

    // ====================== incCounter(key) =========================

    @Test(expected = NullPointerException.class)
    public void incCounter_key_nullKeyFails() {
        repository.incCounter(null);
    }

    @Test
    public void incCounter_key() {
        String key = "somekey";
        repository.incCounter(key);
        assertEquals(1, repository.getCount(key));

        repository.incCounter(key);
        assertEquals(2, repository.getCount(key));
    }

    // ====================== incCounter(key) =========================

    @Test(expected = NullPointerException.class)
    public void incCounter_key_value_nullKeyFails() {
        repository.incCounter(null, 1);
    }

    @Test
    public void incCounter_key_value() {
        String key = "somekey";
        repository.incCounter(key, 10);
        assertEquals(10, repository.getCount(key));

        repository.incCounter(key, 20);
        assertEquals(30, repository.getCount(key));
    }

    // ===================== getCounter(key1,key2)===================

    @Test(expected = NullPointerException.class)
    public void getCounter_key1_key2_nullKey1Fails() {
        repository.getCount(null, "foo");
    }

    @Test(expected = NullPointerException.class)
    public void getCounter_key1_key2_nullKey2Fails() {
        repository.getCount("foo", null);
    }

    // ===================== incCounter(key1,key2)===================

    @Test
    public void incCounter_key1_key2() {
        repository.incCounter("foo", "bar");
        assertEquals(1, repository.getCount("foo", "bar"));
    }

    @Test
    public void incCounterMultipleKeys_key1_key2() {
        repository.incCounter("foo1", "bar1");
        repository.incCounter("foo1", "bar2");
        repository.incCounter("foo2", "bar1");
        repository.incCounter("foo1", "bar1");

        assertEquals(2, repository.getCount("foo1", "bar1"));
        assertEquals(1, repository.getCount("foo1", "bar2"));
        assertEquals(1, repository.getCount("foo2", "bar1"));
    }
}
