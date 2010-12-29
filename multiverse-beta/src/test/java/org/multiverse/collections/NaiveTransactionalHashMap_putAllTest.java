package org.multiverse.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.execute;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class NaiveTransactionalHashMap_putAllTest {

    private Stm stm;
    private NaiveTransactionalHashMap<String, String> map;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
        map = new NaiveTransactionalHashMap<String, String>(stm);
    }

    @Test
    public void whenNullMap_thenNullPointerException() {
        execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                try {
                    map.putAll(null);
                    fail();
                } catch (NullPointerException expected) {
                }

                assertEquals(0, map.size());
            }
        });
    }

    @Test
    public void whenEmptyMapAdded(){

         execute(new AtomicVoidClosure() {
             @Override
             public void execute(Transaction tx) throws Exception {
                 map.putAll(new HashMap<String,String>());

                 assertEquals(0, map.size());
             }
         });
    }

    @Test
    public void whenOneOfTheItemsIsNull(){

    }

    @Test
    public void whenAllDifferentItems(){

    }

    @Test
    public void whenSomeItemsReplaced(){

    }
}
