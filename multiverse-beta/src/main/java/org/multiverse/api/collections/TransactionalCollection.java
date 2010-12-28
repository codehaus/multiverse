package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import java.util.Collection;

public interface TransactionalCollection<E> extends TransactionalIterable<E> {

    Stm getStm();

    boolean isEmpty();

    boolean isEmpty(Transaction tx);

    boolean contains(Object item);

    boolean contains(Transaction tx, Object item);

    int size();

    int size(Transaction tx);

    void clear();

    void clear(Transaction tx);

    boolean add(E item);

    boolean addAll(Collection<? extends E> c);

    boolean addAll(Transaction tx, Collection<? extends E> c);

    boolean addAll(TransactionalCollection<? extends E> c);

    boolean addAll(Transaction tx, TransactionalCollection<? extends E> c);

    boolean add(Transaction tx, E item);

    String toString(Transaction tx);
}
