package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionTreeMap_NodeTests {

    @Test
    public void nodeWithNullRightShouldNotBeRightHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        assertFalse(node.isRightHeavy());
    }

    @Test
    public void nodeWithNullLeftAndNonNullRightWithHeightOneShouldBeRightHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        node.setRight(new TransactionalTreeMap.Node(2, "two"));
        assertTrue(node.isRightHeavy());
    }

    @Test
    public void whenHeightOfRightIsGreaterThaHeightOfLeftNodeShouldBeRightHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        TransactionalTreeMap.Node right = new TransactionalTreeMap.Node(2, "two");
        TransactionalTreeMap.Node left = new TransactionalTreeMap.Node(3, "three");
        TransactionalTreeMap.Node anotherNode = new TransactionalTreeMap.Node(4, "four");
        TransactionalTreeMap.Node oneMoreNode = new TransactionalTreeMap.Node(4, "four");

        anotherNode.setRight(oneMoreNode);
        right.setRight(anotherNode);
        node.setRight(right);
        node.setLeft(left);

        assertTrue(node.isRightHeavy());
    }

    @Test
    public void nodeWithNullLeftShouldNotBeLeftHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        assertFalse(node.isLeftHeavy());
    }

    @Test
    public void nodeWithNullRightAndNonNullLeftWithHeightOneShouldBeLeftHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        node.setLeft(new TransactionalTreeMap.Node(2, "two"));
        assertTrue(node.isLeftHeavy());
    }

    @Test
    public void whenHeightOfLeftIsGreaterThaHeightOfRightNodeShouldBeLeftHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        TransactionalTreeMap.Node right = new TransactionalTreeMap.Node(2, "two");
        TransactionalTreeMap.Node left = new TransactionalTreeMap.Node(3, "three");
        TransactionalTreeMap.Node anotherNode = new TransactionalTreeMap.Node(4, "four");
        TransactionalTreeMap.Node oneMoreNode = new TransactionalTreeMap.Node(4, "four");

        anotherNode.setLeft(oneMoreNode);
        left.setLeft(anotherNode);
        node.setRight(right);
        node.setLeft(left);

        assertTrue(node.isLeftHeavy());
    }

    @Test
    public void nodeShouldComputeBalanceFactor() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        TransactionalTreeMap.Node right = new TransactionalTreeMap.Node(2, "two");
        TransactionalTreeMap.Node anotherNode = new TransactionalTreeMap.Node(3, "three");

        node.setRight(right);

        assertEquals(1, node.balanceFactor());

        right.setRight(anotherNode);

        assertEquals(2, node.balanceFactor());
    }
}
