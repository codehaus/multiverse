package org.multiverse.stms.alpha.instrumentation.integrationtest;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.annotations.TransactionalObject;

/**
 * @author Peter Veentjer
 */
@TransactionalObject
public final class Stack<E> {

    private int size;
    private Node<E> head;

    public Stack() {
        head = null;
        size = 0;
    }

    public void clear() {
        size = 0;
        head = null;
    }

    public void push(E item) {
        if (item == null) {
            throw new NullPointerException();
        }

        head = new Node<E>(head, item);
        size++;
    }

    public E pop() {
        if (size == 0) {
            retry();
        }

        size--;
        Node<E> oldHead = head;
        head = oldHead.next;
        return oldHead.value;
    }

    @TransactionalMethod(readonly = true)
    public boolean isEmpty() {
        return size == 0;
    }

    @TransactionalMethod(readonly = true)
    public int size() {
        return size;
    }
}

class Node<E> {
    public final Node<E> next;
    public final E value;

    Node(Node<E> next, E value) {
        this.next = next;
        this.value = value;
    }
}

