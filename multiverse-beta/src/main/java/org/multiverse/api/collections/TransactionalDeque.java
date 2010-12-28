package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

public interface TransactionalDeque<E> extends TransactionalQueue<E> {

    boolean offerFirst(E e);

    boolean offerFirst(Transaction tx, E e);

    E pollFirst();

    E pollFirst(Transaction tx);

    E peekFirst();

    E peekFirst(Transaction tx);

    void putFirst(E item);

    void putFirst(Transaction tx, E item);

    E takeFirst();

    E takeFirst(Transaction tx);

    boolean offerLast(E e);

    boolean offerLast(Transaction tx, E e);

    E pollLast();

    E pollLast(Transaction tx);

    E peekLast();

    E peekLast(Transaction tx);

    void putLast(E item);

    void putLast(Transaction tx, E item);

    E takeLast();

    E takeLast(Transaction tx);
}
