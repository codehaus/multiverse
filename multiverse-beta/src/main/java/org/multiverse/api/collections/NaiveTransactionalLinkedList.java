package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.references.IntRef;
import org.multiverse.api.references.Ref;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A LinkedList implementation that also acts as a TransactionalQueue, TransactionalDeque.
 *
 * @param <E>
 */
public class NaiveTransactionalLinkedList<E> implements TransactionalQueue<E>, TransactionalList<E> {

    private final Stm stm;
    private final int capacity;
    private final IntRef size;
    private final Ref<Node<E>> head;
    private final Ref<Node<E>> tail;

    public NaiveTransactionalLinkedList(Stm stm) {
        this(stm, Integer.MAX_VALUE);
    }

    public NaiveTransactionalLinkedList(Stm stm, int capacity) {
        if (stm == null) {
            throw new NullPointerException();
        }

        if (capacity < 0) {
            throw new IllegalArgumentException();
        }

        this.stm = stm;
        this.capacity = capacity;
        this.size = stm.getDefaultRefFactory().newIntRef(0);
        this.head = stm.getDefaultRefFactory().newRef(null);
        this.tail = stm.getDefaultRefFactory().newRef(null);
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
        return size(tx) == 0;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean offer(E item) {
        return offer(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean offer(Transaction tx, E item) {
        if (size.get(tx) == capacity) {
            return false;
        }

        put(tx, item);
        return true;
    }

    @Override
    public E peek() {
        return peek(getThreadLocalTransaction());
    }

    @Override
    public E peek(Transaction tx) {
        Node<E> t = tail.get();
        return t == null ? null : t.value;
    }

    @Override
    public E poll() {
        return poll(getThreadLocalTransaction());
    }

    @Override
    public E poll(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public E get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }

        throw new TodoException();
    }

    @Override
    public E get(Transaction tx, int index) {
        throw new TodoException();
    }

    @Override
    public void clear() {
        clear(getThreadLocalTransaction());
    }

    @Override
    public void clear(Transaction tx) {
        if (size.get(tx) == 0) {
            return;
        }

        size.set(tx, 0);
        head.set(tx, null);
        tail.set(tx, null);
    }


    @Override
    public void put(E item) {
        put(getThreadLocalTransaction(), item);
    }

    @Override
    public void put(Transaction tx, E item) {
        if (item == null) {
            throw new NullPointerException();
        }

        int s = size.get(tx);

        if (s == capacity) {
            tx.retry();
        }

        Node<E> node = new Node<E>(stm, item);
        if (s == 0) {
            head.set(tx, node);
            tail.set(tx, node);
        } else {
            node.next.set(tx, head.get(tx));
            head.get(tx).previous.set(tx, node);
            head.set(tx, node);
        }
        size.increment(tx);
    }

    @Override
    public E take() {
        return take(getThreadLocalTransaction());
    }

    @Override
    public E take(Transaction tx) {
        int s = size.get(tx);

        if (s == 0) {
            tx.retry();
        }

        E item;
        if (s == 1) {
            item = head.get(tx).value;
            head.set(tx, null);
            tail.set(tx, null);
        } else {
            Node<E> oldTail = tail.get(tx);
            item = oldTail.value;
            Node<E> newTail = oldTail.previous.get(tx);
            tail.set(tx, newTail);
            newTail.next.set(tx, null);
        }
        size.decrement(tx);
        return item;
    }

    static class Node<E> {
        private final Ref<Node<E>> next;
        private final Ref<Node<E>> previous;
        private final E value;

        Node(Stm stm, E value) {
            this.next = stm.getDefaultRefFactory().newRef(null);
            this.previous = stm.getDefaultRefFactory().newRef(null);
            this.value = value;
        }
    }
}
