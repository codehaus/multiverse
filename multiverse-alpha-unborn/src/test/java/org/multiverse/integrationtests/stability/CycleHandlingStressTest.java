package org.multiverse.integrationtests.stability;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.transactional.DefaultTransactionalReference;

import java.util.LinkedList;
import java.util.Random;

import static org.junit.Assert.assertEquals;
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

        public void setNext(CycleHandlingStressTest.SingleLinkedNode next) {
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

    @Test
    public void complexObjectGraphWithLoadsOfCycles() {
        final int nodeCount = 100000;

        long oldVersion = stm.getVersion();

        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                ComplexNode node = createComplexGraphWithLoadsOfCycles(nodeCount);
                return null;
            }
        }.execute();

        assertEquals(oldVersion, stm.getVersion());
    }


    @Test
    public void anotherComplexObjectGraphWithLoadsOfCycles() {
        final int nodeCount = 100000;

        long oldVersion = stm.getVersion();
        //long oldWriteCount = stm.getProfiler().sumKey1("updatetransaction.individualwrite.count");

        new TransactionTemplate(stm.getTransactionFactoryBuilder().setReadonly(false).build()) {
            @Override
            public Object execute(Transaction t) throws Exception {
                ComplexNode node = createAnotherComplexGraphWithLoadsOfCycles(nodeCount);
                return null;
            }
        }.execute();

        //multiply by 3 because each complexnode needs 3 primitives
        //assertEquals(oldWriteCount + 3 * nodeCount, stm.getProfiler().sumKey1("updatetransaction.individualwrite.count"));
        assertEquals(oldVersion, stm.getVersion());
    }


    private ComplexNode createComplexGraphWithLoadsOfCycles(int nodeCount) {
        if (nodeCount <= 0) {
            throw new IllegalArgumentException("nodeCount parameter should be > 0.");
        }

        final Random rng = new Random();

        // The chance for the generation of a reference to a previous node.
        final double referenceProbability = 0.1;
        // The chance for the generation of a terminator node.
        final double nullProbability = 0.2;
        // Maximum amount of previously generated nodes stored for back referencing.
        final int maxReferenceListSize = 12;

        // The workList contains all generated ComplexNodes that have no child nodes yet
        // and are waiting for processing.
        LinkedList<ComplexNode> workList = new LinkedList<ComplexNode>();
        // The backreferences contains the list of ComplexNodes that can getGlobalStmInstance referenced by
        // a ComplexNode that is processed.
        LinkedList<ComplexNode> backreferences = new LinkedList<ComplexNode>();

        // Initialize the lists with the root node.
        ComplexNode root = new ComplexNode();
        --nodeCount;
        workList.addFirst(root);
        backreferences.addFirst(root);

        while (!workList.isEmpty() && nodeCount > 0) {
            ComplexNode current = workList.removeLast();
            ComplexNode[] children = new ComplexNode[3];

            for (int i = 0; i < 3; ++i) {
                double randomVal = rng.nextDouble();
                if (randomVal <= nullProbability) {
                    children[i] = null;
                } else if (randomVal <= (nullProbability + referenceProbability) || nodeCount <= 0) {
                    // We choose to generate a back reference when we are not allowed to allocate new ComplexNodes
                    // in order to increase the complexity.
                    children[i] = backreferences.peekLast();
                } else {
                    children[i] = new ComplexNode();
                    workList.push(children[i]);
                    backreferences.addFirst(children[i]);
                    --nodeCount;
                    if (backreferences.size() > maxReferenceListSize) {
                        backreferences.removeLast();
                    }
                }
            }

            current.setEdge1(children[0]);
            current.setEdge2(children[1]);
            current.setEdge3(children[2]);
        }

        // After the loop, it could be that the workList is not empty and still many ComplexNodes would
        // like to getGlobalStmInstance processed. We can safely ignore these nodes, as their children are initialized to null
        // (terminator node) by default.
        // Another option is that the workList is empty, but not enough nodes are generated. This could happen
        // if the null probability is set high and a lot of terminators are generated. In that case, we choose to
        // pad the tree's rightmost child that has a null child right child with a tree that has only 
        // right children. We continue this process until we end up with the correct amount of nodes.
        if (nodeCount > 0) {
            ComplexNode rightMost = root;
            while (rightMost.getEdge3() != null) {
                rightMost = rightMost.getEdge3();
            }

            ComplexNode parent = rightMost;
            while (nodeCount > 0) {
                ComplexNode child = new ComplexNode();
                --nodeCount;
                parent.setEdge3(child);
                parent = child;
            }
        }

        return root;
    }

    /*
     * Create a complex graph that can contain cycles. The maximum amount of nodes
     * allocated are controlled by the nodeCount parameter, but less could be allocated,
     * depending on the random generation of the graph.
     *
     * @param nodeCount The maximum amount of ComplexNode objects allocated.
     * @return A graph of ComplexNodes that can contain cycles.
     */

    private ComplexNode createAnotherComplexGraphWithLoadsOfCycles(int nodeCount) {
        if (nodeCount <= 0) {
            throw new IllegalArgumentException("nodeCount parameter should be > 0.");
        }

        final Random rng = new Random();

        ComplexNode[] nodes = new ComplexNode[nodeCount];
        ComplexNode root = new ComplexNode();
        nodes[0] = root;

        ComplexNode current = root;

        //createReference an initial set all linked on edge2.
        for (int k = 1; k < nodeCount; k++) {
            ComplexNode newNode = new ComplexNode();
            nodes[k] = newNode;

            current.setEdge2(newNode);
            current = newNode;
        }

        //fill edge1 and 3.
        for (int k = 0; k < nodeCount; k++) {
            ComplexNode node = nodes[k];

            int edge1Index = (int) Math.round(Math.floor(rng.nextFloat() * nodeCount));
            node.setEdge1(nodes[edge1Index]);

            int edge3Index = (int) Math.round(Math.floor(rng.nextFloat() * nodeCount));
            node.setEdge3(nodes[edge3Index]);
        }

        //introducing self cycles and null primitives
        for (int k = 0; k < nodeCount; k += 10) {
            ComplexNode node = nodes[k];
            node.setEdge1(node);
            node.setEdge3(null);
        }

        return root;
    }


    static class ComplexNode {

        final DefaultTransactionalReference<ComplexNode> edge1Ref = new DefaultTransactionalReference<ComplexNode>();
        final DefaultTransactionalReference<ComplexNode> edge2Ref = new DefaultTransactionalReference<ComplexNode>();
        final DefaultTransactionalReference<ComplexNode> edge3Ref = new DefaultTransactionalReference<ComplexNode>();

        @TransactionalMethod
        public ComplexNode getEdge1() {
            return edge1Ref.get();
        }

        @TransactionalMethod
        public void setEdge1(ComplexNode edge1) {
            edge1Ref.set(edge1);
        }

        @TransactionalMethod
        public ComplexNode getEdge2() {
            return edge2Ref.get();
        }

        @TransactionalMethod
        public void setEdge2(ComplexNode edge2) {
            edge2Ref.set(edge2);
        }

        @TransactionalMethod
        public ComplexNode getEdge3() {
            return edge3Ref.get();
        }

        @TransactionalMethod
        public void setEdge3(ComplexNode edge3) {
            edge3Ref.set(edge3);
        }
    }
}
