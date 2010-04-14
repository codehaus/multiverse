package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.Exclude;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.transactions.readonly.ArrayReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.MapReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.MonoReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.NonTrackingReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.ArrayUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MapUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MonoUpdateAlphaTransaction;
import org.multiverse.transactional.arrays.TransactionalReferenceArray;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertInstanceOf;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class SpeculativeSizeTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenReadTracking() {
        SpeculativeSize o = new SpeculativeSize();

        o.trackingRead();
        assertEquals(3, o.transactions.size());
        assertInstanceOf(o.transactions.get(0), MonoReadonlyAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(1), ArrayReadonlyAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(2), MapReadonlyAlphaTransaction.class);

        //and try another transaction and see that the system learned
        //from he speculative executions
        SpeculativeSize o2 = new SpeculativeSize();
        o2.trackingRead();
        assertEquals(1, o2.transactions.size());
        assertInstanceOf(o2.transactions.get(0), MapReadonlyAlphaTransaction.class);
    }

    @Test
    public void whenSpeculativeSizeAndNonTrackingRead() {
        SpeculativeSize o = new SpeculativeSize();

        o.read();
        assertEquals(1, o.transactions.size());
        assertInstanceOf(o.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);

        //and try another transaction and see that the system learned
        //from he speculative executions
        SpeculativeSize o2 = new SpeculativeSize();
        o2.read();
        assertEquals(1, o2.transactions.size());
        assertInstanceOf(o2.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);
    }

    @Test
    public void whenSpeculativeSizeAndUpdateFailure_thenUpgradedToMap() {
        SpeculativeSize o = new SpeculativeSize();

        o.update();
        assertEquals("" + o.transactions, 4, o.transactions.size());
        assertInstanceOf(o.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(1), MonoUpdateAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(2), ArrayUpdateAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(3), MapUpdateAlphaTransaction.class);

        //and try another transaction and see that the system learned
        //from he speculative executions
        SpeculativeSize o2 = new SpeculativeSize();
        o2.update();
        assertEquals(1, o2.transactions.size());
        assertInstanceOf(o2.transactions.get(0), MapUpdateAlphaTransaction.class);
    }

    @TransactionalObject
    class SpeculativeSize {

        private TransactionalReferenceArray<String> array =
                new TransactionalReferenceArray<String>(1000);

        @Exclude
        private List<Transaction> transactions = new LinkedList<Transaction>();

        public void read() {
            transactions.add(getThreadLocalTransaction());
            for (int k = 0; k < array.length(); k++) {
                array.get(k);
            }
        }

        @TransactionalMethod(automaticReadTrackingEnabled = true)
        public void trackingRead() {
            transactions.add(getThreadLocalTransaction());
            for (int k = 0; k < array.length(); k++) {
                array.get(k);
            }
        }


        public void update() {
            transactions.add(getThreadLocalTransaction());
            for (int k = 0; k < array.length(); k++) {
                array.set(k, array.get(k) + "a");
            }

        }
    }
}
