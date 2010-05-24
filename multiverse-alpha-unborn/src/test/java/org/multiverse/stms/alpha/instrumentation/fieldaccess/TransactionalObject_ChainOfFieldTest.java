package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_ChainOfFieldTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void fieldX() {
        FieldIsTxObjectChain ref = new FieldIsTxObjectChain(null);
        assertNull(ref.getNext());

        FieldIsTxObjectChain next = new FieldIsTxObjectChain(null);
        ref.setNext(next);

        assertSame(next, ref.getNext());
        assertNull(ref.getNextNext());
    }

    @Test
    public void fieldLongRef() {
        FieldIsTxObjectChain item1 = new FieldIsTxObjectChain(null);
        FieldIsTxObjectChain item2 = new FieldIsTxObjectChain(item1);
        FieldIsTxObjectChain item3 = new FieldIsTxObjectChain(item2);
        FieldIsTxObjectChain item4 = new FieldIsTxObjectChain(item3);
        FieldIsTxObjectChain item5 = new FieldIsTxObjectChain(item4);

        assertSame(item1, item5.getNextNextNextNext());
        assertNull(item4.getNextNextNextNext());
    }

    @TransactionalObject
    private static class FieldIsTxObjectChain {
        FieldIsTxObjectChain next;

        private FieldIsTxObjectChain(FieldIsTxObjectChain next) {
            this.next = next;
        }

        public FieldIsTxObjectChain getNext() {
            return next;
        }

        public void setNext(FieldIsTxObjectChain next) {
            this.next = next;
        }

        public FieldIsTxObjectChain getNextNext() {
            return next.next;
        }

        public FieldIsTxObjectChain getNextNextNextNext() {
            return next.next.next.next;
        }
    }

    @Test
    public void fieldThatAreNotAtomicObjects() {
        Node node1 = new Node(null);
        Node node2 = new Node(node1);
        Node node3 = new Node(node2);
        FieldIsNotTxObjectChain root = new FieldIsNotTxObjectChain(node3);

        assertSame(node3, root.getNode());
        assertSame(node2, root.getNodeNext());
        assertSame(node1, root.getNodeNextNext());
        assertNull(root.getNodeNextNextNext());
    }

    @TransactionalObject
    private static class FieldIsNotTxObjectChain {
        Node node;

        private FieldIsNotTxObjectChain(Node node) {
            this.node = node;
        }

        public Node getNode() {
            return node;
        }

        public Node getNodeNext() {
            return node.next;
        }

        public Node getNodeNextNext() {
            return node.next.next;
        }

        public Node getNodeNextNextNext() {
            return node.next.next.next;
        }
    }

    class Node {

        Node next;

        Node(Node next) {
            this.next = next;
        }
    }

    @Test
    public void fieldOfTypeTxObjectIsNotNull() {
        IntRef ref = new IntRef(1);
        FieldWithOtherTypeTxObject o = new FieldWithOtherTypeTxObject(null);

        assertNull(o.getRef());
    }

    @Test
    public void fieldOfTypeAtomicObjectThatIsNull() {
        //force the load of the intref, will be fixed in the future.
        IntRef ref = new IntRef(0);

        FieldWithOtherTypeTxObject o = new FieldWithOtherTypeTxObject(ref);

        assertSame(ref, o.getRef());
        assertEquals(1, o.inc());
        assertEquals(1, ref.get());
    }

    @TransactionalObject
    private static class FieldWithOtherTypeTxObject {
        private IntRef ref;

        private FieldWithOtherTypeTxObject(IntRef ref) {
            this.ref = ref;
        }

        public IntRef getRef() {
            return ref;
        }

        public void setRef(IntRef ref) {
            this.ref = ref;
        }

        public int inc() {
            ref.inc();
            return ref.get();
        }
    }
}
