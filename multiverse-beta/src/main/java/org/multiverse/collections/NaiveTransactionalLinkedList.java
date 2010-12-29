package org.multiverse.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.collections.TransactionalDeque;
import org.multiverse.api.collections.TransactionalIterator;
import org.multiverse.api.collections.TransactionalList;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.references.IntRef;
import org.multiverse.api.references.Ref;
import org.multiverse.api.references.RefFactory;

import static org.multiverse.api.ThreadLocalTransaction.getRequiredThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A LinkedList implementation that also acts as a TransactionalQueue, TransactionalDeque.
 *
 * @param <E>
 */
public final class NaiveTransactionalLinkedList<E> extends AbstractTransactionalCollection<E>
        implements TransactionalDeque<E>, TransactionalList<E> {

    private final int capacity;
    private final IntRef size;
    private final Ref<Entry<E>> head;
    private final Ref<Entry<E>> tail;

    public NaiveTransactionalLinkedList(Stm stm) {
        this(stm, Integer.MAX_VALUE);
    }

    public NaiveTransactionalLinkedList(Stm stm, int capacity) {
        super(stm);

        if (capacity < 0) {
            throw new IllegalArgumentException();
        }

        this.capacity = capacity;
        this.size = stm.getDefaultRefFactory().newIntRef(0);
        this.head = stm.getDefaultRefFactory().newRef(null);
        this.tail = stm.getDefaultRefFactory().newRef(null);
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean add(Transaction tx, E item) {
        if (!offer(tx, item)) {
            throw new IllegalStateException("NaiveTransactionalLinkedList full");
        }

        return true;
    }

    @Override
    public E remove(int index) {
        return remove(getThreadLocalTransaction(), index);
    }

    @Override
    public E remove(Transaction tx, int index) {
        Entry entry = entry(tx, index);
        throw new UnsupportedOperationException();
    }

    @Override
    public E set(int index, E element) {
        return set(getThreadLocalTransaction(), index, element);
    }

    @Override
    public E set(Transaction tx, int index, E element) {
        return entry(tx,index).value.getAndSet(element);
    }

    @Override
    public int size(Transaction tx) {
        return size.get(tx);
    }

    @Override
    public E get(int index) {
        return get(getThreadLocalTransaction(), index);
    }

    @Override
    public E get(Transaction tx, int index) {
        return entry(tx, index).value.get(tx);
    }

    private Entry<E> entry(Transaction tx, int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }

        int s = size.get(tx);
        if (index >= s) {
            throw new IndexOutOfBoundsException();
        }

        if (index < (s >> 1)) {
            int i = 0;
            Entry<E> node = head.get(tx);
            while (true) {
                if (i == index) {
                    return node;
                }
                node = node.next.get(tx);
                i++;
            }
        } else {
            int i = s - 1;
            Entry<E> node = tail.get(tx);
            while (true) {
                if (i == index) {
                    return node;
                }
                node = node.previous.get(tx);
                i--;
            }
        }
    }

    @Override
    public boolean contains(Transaction tx, Object item) {
        throw new TodoException();
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

    // ================ puts ==========================

    @Override
    public void putFirst(E item) {
        putFirst(getThreadLocalTransaction(), item);
    }

    @Override
    public void putFirst(Transaction tx, E item) {
        if (!offerFirst(tx, item)) {
            tx.retry();
        }
    }

    @Override
    public void put(E item) {
        put(getThreadLocalTransaction(), item);
    }

    @Override
    public void put(Transaction tx, E item) {
        putLast(tx, item);
    }

    @Override
    public void putLast(E item) {
        putLast(getRequiredThreadLocalTransaction(), item);
    }

    @Override
    public void putLast(Transaction tx, E item) {
        if (!offerLast(tx, item)) {
            tx.retry();
        }
    }

    // ================== takes ===============================

    @Override
    public E take() {
        return take(getThreadLocalTransaction());
    }

    @Override
    public E take(Transaction tx) {
        return takeLast(tx);
    }

    @Override
    public E takeFirst() {
        return takeFirst(getThreadLocalTransaction());
    }

    @Override
    public E takeFirst(Transaction tx) {
        E item = pollFirst(tx);
        if (item == null) {
            tx.retry();
        }
        return item;
    }

    @Override
    public E takeLast() {
        return takeLast(getThreadLocalTransaction());
    }

    @Override
    public E takeLast(Transaction tx) {
        E item = pollLast(tx);
        if (item == null) {
            tx.retry();
        }

        return item;
    }

    // ================== offers ========================

    @Override
    public boolean offerFirst(E e) {
        return offerFirst(getThreadLocalTransaction(), e);
    }

    @Override
    public boolean offerFirst(Transaction tx, E item) {
        if (item == null) {
            throw new NullPointerException();
        }

        int s = size.get();
        if (s == capacity) {
            return false;
        }

        Entry<E> node = new Entry<E>(defaultRefFactory, item);
        if (s == 0) {
            head.set(tx, node);
            tail.set(tx, node);
        } else {
            node.next.set(tx, head.get(tx));
            head.get(tx).previous.set(tx, node);
            head.set(tx, node);
        }
        size.increment(tx);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        return offerLast(getThreadLocalTransaction(), e);
    }

    @Override
    public boolean offerLast(Transaction tx, E item) {
        if (item == null) {
            throw new NullPointerException();
        }

        int s = size.get();
        if (s == capacity) {
            return false;
        }

        Entry<E> node = new Entry<E>(defaultRefFactory, item);
        if (s == 0) {
            head.set(tx, node);
            tail.set(tx, node);
        } else {
            node.previous.set(tx, tail.get(tx));
            tail.get(tx).next.set(tx, node);
            tail.set(tx, node);
        }
        size.increment(tx);
        return true;
    }

    @Override
    public boolean offer(E item) {
        return offer(getThreadLocalTransaction(), item);
    }

    @Override
    public boolean offer(Transaction tx, E item) {
        return offerLast(tx, item);
    }

    // ================ polls =======================

    @Override
    public E pollFirst() {
        return pollFirst(getThreadLocalTransaction());
    }

    @Override
    public E pollFirst(Transaction tx) {
        int s = size.get(tx);

        if (s == 0) {
            return null;
        }

        E item;
        if (s == 1) {
            item = tail.get(tx).value.get(tx);
            head.set(tx, null);
            tail.set(tx, null);
        } else {
            Entry<E> oldHead = head.get(tx);
            item = oldHead.value.get(tx);
            Entry<E> newHead = oldHead.next.get(tx);
            head.set(tx, newHead);
            newHead.previous.set(tx, null);
        }
        size.decrement(tx);
        return item;
    }

    @Override
    public E pollLast() {
        return pollLast(getThreadLocalTransaction());
    }

    @Override
    public E pollLast(Transaction tx) {
        int s = size.get(tx);

        if (s == 0) {
            return null;
        }

        E item;
        if (s == 1) {
            item = head.get(tx).value.get(tx);
            head.set(tx, null);
            tail.set(tx, null);
        } else {
            Entry<E> oldTail = tail.get(tx);
            item = oldTail.value.get(tx);
            Entry<E> newTail = oldTail.previous.get(tx);
            tail.set(tx, newTail);
            newTail.next.set(tx, null);
        }
        size.decrement(tx);
        return item;
    }

    @Override
    public E poll() {
        return poll(getThreadLocalTransaction());
    }

    @Override
    public E poll(Transaction tx) {
        return pollLast(tx);
    }

    // =============== peeks =================

    @Override
    public E peekFirst() {
        return peekFirst(getThreadLocalTransaction());
    }

    @Override
    public E peekFirst(Transaction tx) {
        Entry<E> h = head.get(tx);
        return h == null ? null : h.value.get(tx);
    }

    @Override
    public E peekLast() {
        return peekLast(getThreadLocalTransaction());
    }

    @Override
    public E peekLast(Transaction tx) {
        Entry<E> t = tail.get(tx);
        return t == null ? null : t.value.get(tx);
    }

    @Override
    public E peek() {
        return peek(getThreadLocalTransaction());
    }

    @Override
    public E peek(Transaction tx) {
        return peekLast(tx);
    }

    // ================ misc ==========================

    @Override
    public TransactionalIterator<E> iterator(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public String toString(Transaction tx) {
        int s = size(tx);
        if (s == 0) {
            return "[]";
        }

        StringBuffer sb = new StringBuffer();
        sb.append('[');
        Entry<E> node = head.get(tx);
        do {
            sb.append(node.value.get(tx));
            node = node.next.get(tx);
            if (node != null) {
                sb.append(", ");
            }
        } while (node != null);
        sb.append(']');
        return sb.toString();
    }

    static class Entry<E> {
        private final Ref<Entry<E>> next;
        private final Ref<Entry<E>> previous;
        private final Ref<E> value;

        Entry(RefFactory refFactory, E value) {
            this.next = refFactory.newRef(null);
            this.previous = refFactory.newRef(null);
            this.value = refFactory.newRef(value);
        }
    }
}
