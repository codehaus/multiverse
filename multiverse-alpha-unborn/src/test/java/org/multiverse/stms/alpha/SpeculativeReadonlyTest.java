package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.NonTransactional;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.transactions.readonly.NonTrackingReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MonoUpdateAlphaTransaction;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertInstanceOf;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A test for the speculative behavior (like spulative starting with readonly, or not tracking
 * reads etc). We want to make sure that the system really works.
 *
 * @author Peter Veentjer
 */
public class SpeculativeReadonlyTest {


    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenSpeculativeReadonlyAndRead_thenSpeculationSuccessful() {
        SpeculativeReadonly o = new SpeculativeReadonly();
        o.read();

        assertEquals(1, o.transactions.size());
        assertInstanceOf(o.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);

        //test that the other tests still are able to use readonly
        SpeculativeReadonly o2 = new SpeculativeReadonly();
        o2.read();

        assertEquals(1, o2.transactions.size());
        assertInstanceOf(o2.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);
    }

    @Test
    public void whenSpeculativeReadonlyAndWrite_thenSpeculationSuccessfulAndUpgradedToWrite() {
        SpeculativeReadonly o = new SpeculativeReadonly();
        o.update();
        assertEquals(2, o.transactions.size());
        assertInstanceOf(o.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(1), MonoUpdateAlphaTransaction.class);

        //test that the second time the readonly version isn't selected anymore
        SpeculativeReadonly o2 = new SpeculativeReadonly();
        o2.update();

        assertEquals(1, o2.transactions.size());
        assertInstanceOf(o2.transactions.get(0), MonoUpdateAlphaTransaction.class);
    }

    @TransactionalObject
    class SpeculativeReadonly {

        private int foo;

        @NonTransactional
        private List<Transaction> transactions = new LinkedList<Transaction>();

        public void read() {
            transactions.add(getThreadLocalTransaction());
            System.out.println("Reading:" + foo);
        }

        public void update() {
            transactions.add(getThreadLocalTransaction());
            foo++;
        }
    }

}
