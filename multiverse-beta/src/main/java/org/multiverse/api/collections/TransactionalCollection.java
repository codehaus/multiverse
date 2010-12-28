package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

public interface TransactionalCollection<E> {

    Stm getStm();

    boolean isEmpty();

    boolean isEmpty(Transaction tx);

    int size();

    int size(Transaction tx);

    void clear();

    void clear(Transaction tx);

    boolean add(E item);

    boolean add(Transaction tx, E item);

     String toString(Transaction tx);
}
