package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

public interface TransactionalList<E>{

    Stm getStm();

    boolean isEmpty();

    boolean isEmpty(Transaction tx);

    int size();

    int size(Transaction tx);

    void clear();

    void clear(Transaction tx);

    boolean add(E item);

    boolean add(Transaction tx, E item);

    E get(int index);

    E get(Transaction tx, int index);

    E remove(int index);

    E remove(Transaction tx, int index);
}
