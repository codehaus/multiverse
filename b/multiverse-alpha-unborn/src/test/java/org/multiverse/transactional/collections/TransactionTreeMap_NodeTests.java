package org.multiverse.transactional.collections;

import static org.junit.Assert.*;
import org.junit.Test;

public class TransactionTreeMap_NodeTests {

    @Test
    public void nodeWithNullRightShouldNotBeRightHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        assertFalse(node.isRightHeavy());
    }

    @Test
    public void nodeWithNullLeftAndNonNullRightWithHeightOneShouldBeRightHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        node.right = new TransactionalTreeMap.Node(2, "two");
        assertTrue(node.isRightHeavy());
    }

    @Test
    public void whenHeightOfRightIsGreaterThaHeightOfLeftNodeShouldBeRightHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        TransactionalTreeMap.Node right = new TransactionalTreeMap.Node(2, "two");
        TransactionalTreeMap.Node left = new TransactionalTreeMap.Node(3, "three");
        TransactionalTreeMap.Node anotherNode = new TransactionalTreeMap.Node(4, "four");
        TransactionalTreeMap.Node oneMoreNode = new TransactionalTreeMap.Node(4, "four");

        anotherNode.right = oneMoreNode;
        right.right = anotherNode;
        node.right = right;
        node.left = left;

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
        node.left = new TransactionalTreeMap.Node(2, "two");
        assertTrue(node.isLeftHeavy());
    }

    @Test
    public void whenHeightOfLeftIsGreaterThaHeightOfRightNodeShouldBeLeftHeavy() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        TransactionalTreeMap.Node right = new TransactionalTreeMap.Node(2, "two");
        TransactionalTreeMap.Node left = new TransactionalTreeMap.Node(3, "three");
        TransactionalTreeMap.Node anotherNode = new TransactionalTreeMap.Node(4, "four");
        TransactionalTreeMap.Node oneMoreNode = new TransactionalTreeMap.Node(4, "four");

        anotherNode.left = oneMoreNode;
        left.left = anotherNode;
        node.right = right;
        node.left = left;

        assertTrue(node.isLeftHeavy());
    }

    @Test
    public void nodeShouldComputeBalanceFactor() {
        TransactionalTreeMap.Node node = new TransactionalTreeMap.Node(1, "one");
        TransactionalTreeMap.Node right = new TransactionalTreeMap.Node(2, "two");
        TransactionalTreeMap.Node anotherNode = new TransactionalTreeMap.Node(3, "three");

        node.right = right;

        assertEquals(1, node.balanceFactor());

        right.right = anotherNode;

        assertEquals(2, node.balanceFactor());
    }
}
