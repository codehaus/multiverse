package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionTreeMap_NodeTests {

    @Test
    public void nodeWithNullRightShouldNotBeRightHeavy() {
        TransactionalTreeMap.EntryImpl node = new TransactionalTreeMap.EntryImpl(1, "one");
        assertFalse(node.isRightHeavy());
    }

    @Test
    public void nodeWithNullLeftAndNonNullRightWithHeightOneShouldBeRightHeavy() {
        TransactionalTreeMap.EntryImpl node = new TransactionalTreeMap.EntryImpl(1, "one");
        node.setRight(new TransactionalTreeMap.EntryImpl(2, "two"));
        assertTrue(node.isRightHeavy());
    }

    @Test
    public void whenHeightOfRightIsGreaterThaHeightOfLeftNodeShouldBeRightHeavy() {
        TransactionalTreeMap.EntryImpl node = new TransactionalTreeMap.EntryImpl(1, "one");
        TransactionalTreeMap.EntryImpl right = new TransactionalTreeMap.EntryImpl(2, "two");
        TransactionalTreeMap.EntryImpl left = new TransactionalTreeMap.EntryImpl(3, "three");
        TransactionalTreeMap.EntryImpl anotherNode = new TransactionalTreeMap.EntryImpl(4, "four");
        TransactionalTreeMap.EntryImpl oneMoreNode = new TransactionalTreeMap.EntryImpl(4, "four");

        anotherNode.setRight(oneMoreNode);
        right.setRight(anotherNode);
        node.setRight(right);
        node.setLeft(left);

        assertTrue(node.isRightHeavy());
    }

    @Test
    public void nodeWithNullLeftShouldNotBeLeftHeavy() {
        TransactionalTreeMap.EntryImpl node = new TransactionalTreeMap.EntryImpl(1, "one");
        assertFalse(node.isLeftHeavy());
    }

    @Test
    public void nodeWithNullRightAndNonNullLeftWithHeightOneShouldBeLeftHeavy() {
        TransactionalTreeMap.EntryImpl node = new TransactionalTreeMap.EntryImpl(1, "one");
        node.setLeft(new TransactionalTreeMap.EntryImpl(2, "two"));
        assertTrue(node.isLeftHeavy());
    }

    @Test
    public void whenHeightOfLeftIsGreaterThaHeightOfRightNodeShouldBeLeftHeavy() {
        TransactionalTreeMap.EntryImpl node = new TransactionalTreeMap.EntryImpl(1, "one");
        TransactionalTreeMap.EntryImpl right = new TransactionalTreeMap.EntryImpl(2, "two");
        TransactionalTreeMap.EntryImpl left = new TransactionalTreeMap.EntryImpl(3, "three");
        TransactionalTreeMap.EntryImpl anotherNode = new TransactionalTreeMap.EntryImpl(4, "four");
        TransactionalTreeMap.EntryImpl oneMoreNode = new TransactionalTreeMap.EntryImpl(4, "four");

        anotherNode.setLeft(oneMoreNode);
        left.setLeft(anotherNode);
        node.setRight(right);
        node.setLeft(left);

        assertTrue(node.isLeftHeavy());
    }

    @Test
    public void nodeShouldComputeBalanceFactor() {
        TransactionalTreeMap.EntryImpl node = new TransactionalTreeMap.EntryImpl(1, "one");
        TransactionalTreeMap.EntryImpl right = new TransactionalTreeMap.EntryImpl(2, "two");
        TransactionalTreeMap.EntryImpl anotherNode = new TransactionalTreeMap.EntryImpl(3, "three");

        node.setRight(right);

        assertEquals(1, node.balanceFactor());

        right.setRight(anotherNode);

        assertEquals(2, node.balanceFactor());
    }
}
