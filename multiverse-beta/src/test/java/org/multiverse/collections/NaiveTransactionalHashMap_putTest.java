package org.multiverse.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.execute;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class NaiveTransactionalHashMap_putTest {

    private Stm stm;
    private NaiveTransactionalHashMap<String, String> map;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
        map = new NaiveTransactionalHashMap<String, String>(stm);
    }

    @Test
    public void whenEmpty() {
        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                String result = map.put("key", "value");

                assertNull(result);
                assertEquals(1, map.size());
                //todo: tostring
            }
        });
    }

    @Test
    public void whenReplacingExistingKey() {
        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                map.put("1", "a");
                map.put("2", "b");
                map.put("3", "c");

                String result = map.put("2", "B");

                assertEquals("b", result);
                assertEquals("B", map.get("2"));
                assertEquals(3, map.size());
                //todo: tostring
            }
        });
    }

    @Test
    public void whenNullKey_thenNullPointerException() {
        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                try {
                    map.put(null, "foo");
                    fail();
                } catch (NullPointerException expected) {
                }

                assertEquals(0, map.size());
                assertEquals("[]", map.toString());
                //todo: tostring
            }
        });
    }
}
