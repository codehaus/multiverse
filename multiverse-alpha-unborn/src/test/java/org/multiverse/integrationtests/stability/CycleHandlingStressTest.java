package org.multiverse.integrationtests.stability;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import static org.junit.Assert.assertSame;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * A Test to see how well the MultiversionedStm deals with cycles. In the 0.2 release it was very important because
 * multiverse needed to traverse the object graphs to find the objects that need persistance. But with multiverse 0.3
 * this is not needed.
 *
 * @author Peter Veentjer.
 */
public class CycleHandlingStressTest {

    private Stm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = getGlobalStmInstance();
    }

    public Transaction startUpdateTransaction() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();
        setThreadLocalTransaction(t);
        return t;
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @TransactionalObject
    public static class SingleLinkedNode {

        private SingleLinkedNode next;

        public SingleLinkedNode() {
        }

        public SingleLinkedNode getNext() {
            return next;
        }

        public void setNext(SingleLinkedNode next) {
            this.next = next;
        }
    }

    @Test
    public void directCycle() {
        Transaction t = startUpdateTransaction();
        SingleLinkedNode node = new SingleLinkedNode();
        node.setNext(node);
        t.commit();

        setThreadLocalTransaction(null);
        assertSame(node, node.getNext());
    }

    @Test
    public void shortIndirectCycle() {
        Transaction t = startUpdateTransaction();
        SingleLinkedNode node1 = new SingleLinkedNode();
        SingleLinkedNode node2 = new SingleLinkedNode();
        node1.setNext(node2);
        node2.setNext(node1);
        t.commit();
        setThreadLocalTransaction(null);

        assertSame(node1.getNext(), node2);
        assertSame(node2.getNext(), node1);
    }

    @Test
    public void longIndirectCycleCommitsWithoutFailure() {
        Transaction t = startUpdateTransaction();
        SingleLinkedNode original = createLongChain(100000);
        t.commit();
    }

    private SingleLinkedNode createLongChain(int depth) {
        SingleLinkedNode first = new SingleLinkedNode();
        SingleLinkedNode current = first;
        for (int k = 0; k < depth; k++) {
            SingleLinkedNode newHolder = new SingleLinkedNode();
            current.setNext(newHolder);
            current = newHolder;
        }

        current.setNext(first);
        return first;
    }

}
