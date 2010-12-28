package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

public interface TransactionalSet<E> {

    Stm getStm();

    boolean add(E item);

    boolean add(Transaction tx, E item);

    boolean contains(Object item);

    boolean contains(Transaction tx, Object item);

    boolean remove(Object item);

    boolean remove(Transaction tx, Object item);

    int size();

    int size(Transaction tx);

    boolean isEmpty();

    boolean isEmpty(Transaction tx);

    void clear();

    void clear(Transaction tx);

    String toString(Transaction tx);
}
