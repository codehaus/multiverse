package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.functions.Function;

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

    E foldLeft(Function<E> function);

    E foldLeft(Transaction tx, Function<E> function);

    E foldRight(Function<E> function);

    E foldRight(Transaction tx, Function<E> function);

    TransactionalCollection<E> map(Function<E> function);

    TransactionalCollection<E> map(Transaction tx, Function<E> function);

    TransactionalCollection<E> flatMap();

    TransactionalCollection<E> flatMap(Transaction tx);

    TransactionalCollection<E> filter();

    TransactionalCollection<E> filter(Transaction tx);

    TransactionalCollection<E> foreach();

    TransactionalCollection<E> foreach(Transaction tx);
}
