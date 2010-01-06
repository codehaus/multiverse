package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.datastructures.refs.IntRef;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class AtomicObject_ChainOfFieldTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void fieldX() {
        FieldIsAtomicObjectChain ref = new FieldIsAtomicObjectChain(null);
        assertNull(ref.getNext());

        FieldIsAtomicObjectChain next = new FieldIsAtomicObjectChain(null);
        ref.setNext(next);

        assertSame(next, ref.getNext());
        assertNull(ref.getNextNext());
    }

    @Test
    public void fieldLongRef() {
        FieldIsAtomicObjectChain item1 = new FieldIsAtomicObjectChain(null);
        FieldIsAtomicObjectChain item2 = new FieldIsAtomicObjectChain(item1);
        FieldIsAtomicObjectChain item3 = new FieldIsAtomicObjectChain(item2);
        FieldIsAtomicObjectChain item4 = new FieldIsAtomicObjectChain(item3);
        FieldIsAtomicObjectChain item5 = new FieldIsAtomicObjectChain(item4);

        assertSame(item1, item5.getNextNextNextNext());
        assertNull(item4.getNextNextNextNext());
    }

    @AtomicObject
    private static class FieldIsAtomicObjectChain {
        FieldIsAtomicObjectChain next;

        private FieldIsAtomicObjectChain(FieldIsAtomicObjectChain next) {
            this.next = next;
        }

        public FieldIsAtomicObjectChain getNext() {
            return next;
        }

        public void setNext(FieldIsAtomicObjectChain next) {
            this.next = next;
        }

        public FieldIsAtomicObjectChain getNextNext() {
            return next.next;
        }

        public FieldIsAtomicObjectChain getNextNextNextNext() {
            return next.next.next.next;
        }
    }

    @Test
    public void fieldThatAreNotAtomicObjects() {
        Node node1 = new Node(null);
        Node node2 = new Node(node1);
        Node node3 = new Node(node2);
        FieldIsNotAtomicObjectChain root = new FieldIsNotAtomicObjectChain(node3);

        assertSame(node3, root.getNode());
        assertSame(node2, root.getNodeNext());
        assertSame(node1, root.getNodeNextNext());
        assertNull(root.getNodeNextNextNext());
    }

    @AtomicObject
    private static class FieldIsNotAtomicObjectChain {
        Node node;

        private FieldIsNotAtomicObjectChain(Node node) {
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
    public void fieldOfTypeAtomicObjectIsNotNull() {
        IntRef ref = new IntRef(1);
        FieldWithOtherTypeAtomicObject o = new FieldWithOtherTypeAtomicObject(null);

        assertNull(o.getRef());
    }

    @Test
    public void fieldOfTypeAtomicObjectThatIsNull() {
        //force the load of the intref, will be fixed in the future.
        IntRef intRef = new IntRef(0);

        FieldWithOtherTypeAtomicObject o = new FieldWithOtherTypeAtomicObject(intRef);

        assertSame(intRef, o.getRef());
        assertEquals(1, o.inc());
        assertEquals(1, intRef.get());
    }

    @AtomicObject
    private static class FieldWithOtherTypeAtomicObject {
        private IntRef ref;

        private FieldWithOtherTypeAtomicObject(IntRef ref) {
            this.ref = ref;
        }

        public IntRef getRef() {
            return ref;
        }

        public void setRef(IntRef ref) {
            this.ref = ref;
        }

        public int inc() {
            return ref.inc();
        }
    }
}
