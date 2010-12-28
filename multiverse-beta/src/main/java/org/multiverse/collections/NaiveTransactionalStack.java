package org.multiverse.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.collections.TransactionalStack;
import org.multiverse.api.references.IntRef;
import org.multiverse.api.references.Ref;

import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class NaiveTransactionalStack<E> implements TransactionalStack<E> {

    private final Stm stm;
    private int capacity;
    private final Ref<Node<E>> head;
    private final IntRef size;

    public NaiveTransactionalStack(Stm stm) {
        this(stm, Integer.MAX_VALUE);
    }

    public NaiveTransactionalStack(Stm stm, int capacity) {
        if (stm == null) {
            throw new NullPointerException();
        }

        if (capacity < 0) {
            throw new IllegalArgumentException();
        }

        this.stm = stm;
        this.capacity = capacity;
        this.head = stm.getDefaultRefFactory().newRef(null);
        this.size = stm.getDefaultRefFactory().newIntRef(0);
    }

    @Override
    public Stm getStm() {
        return stm;
    }

    @Override
    public int size() {
        return size(getThreadLocalTransaction());
    }

    @Override
    public int size(Transaction tx) {
        return size.get(tx);
    }

    @Override
    public boolean isEmpty() {
        return isEmpty(getThreadLocalTransaction());
    }

    @Override
    public boolean isEmpty(Transaction tx) {
        return size.get(tx) == 0;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public void clear() {
        clear(getThreadLocalTransaction());
    }

    @Override
    public void clear(Transaction tx) {
        int s = size.get(tx);
        if (s == 0) {
            return;
        }

        size.set(tx, 0);
        head.set(tx, null);
    }

    @Override
    public boolean offer(E item) {
        return offer(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean offer(Transaction tx, E item) {
        if (capacity == size(tx)) {
            return false;
        }

        push(tx, item);
        return true;
    }

    @Override
    public E poll() {
        return poll(getThreadLocalTransaction());
    }

    @Override
    public E poll(Transaction tx) {
        if (size.get(tx) == 0) {
            return null;
        }

        return pop(tx);
    }

    @Override
    public E peek() {
        return peek(getThreadLocalTransaction());
    }

    @Override
    public E peek(Transaction tx) {
        Node<E> h = head.get(tx);
        return h == null ? null : h.value;
    }

    @Override
    public void push(E item) {
        push(getThreadLocalTransaction(), item);
    }

    @Override
    public void push(Transaction tx, E item) {
        if (item == null) {
            throw new NullPointerException();
        }

        if (size.get(tx) == capacity) {
            tx.retry();
        }

        head.set(tx, new Node<E>(head.get(tx), item));
        size.increment(tx);
    }

    @Override
    public E pop() {
        return pop(getThreadLocalTransaction());
    }

    @Override
    public E pop(Transaction tx) {
        if (size.get(tx) == 0) {
            tx.retry();
        }

        Node<E> node = head.get(tx);
        head.set(tx, node.next);
        size.decrement(tx);
        return node.value;
    }


    static class Node<E> {
        final Node<E> next;
        final E value;

        Node(Node<E> next, E value) {
            this.next = next;
            this.value = value;
        }
    }
}
