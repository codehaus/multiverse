package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

import java.util.Collection;
import java.util.Iterator;

public interface TransactionalCollection<E> extends Collection<E> {

    @Override
    int size();

    int size(Transaction tx);

    int atomicSize();

    @Override
    boolean isEmpty();

    boolean isEmpty(Transaction tx);

    boolean atomicIsEmpty();

    @Override
    boolean contains(Object o);

    boolean contains(Transaction tx, Object o);

    boolean atomicContains(Object o);

    @Override
    Iterator<E> iterator();

    Iterator<E> iterator(Transaction tx);

    Iterator<E> atomicIterator();

    @Override
    Object[] toArray();

    Object[] toArray(Transaction tx);

    Object[] atomicToArray();

    @Override
    <T> T[] toArray(T[] a);

    <T> T[] toArray(Transaction tx, T[] a);

    <T> T[] atomicToArray(T[] a);

    @Override
    boolean add(E e);

    boolean add(Transaction tx, E e);

    boolean atomicAdd(E e);

    @Override
    boolean remove(Object o);

    boolean remove(Transaction tx, Object o);

    boolean atomicRemove(Object o);

    @Override
    boolean containsAll(Collection<?> c);

    boolean containsAll(Transaction tx, Collection<?> c);

    boolean atomicContainsAll(Collection<?> c);

    @Override
    boolean addAll(Collection<? extends E> c);

    boolean addAll(Transaction tx, Collection<? extends E> c);

    boolean atomicAddAll(Collection<? extends E> c);

    @Override
    boolean removeAll(Collection<?> c);

    boolean removeAll(Transaction tx, Collection<?> c);

    boolean atomicRemoveAll(Collection<?> c);

    @Override
    boolean retainAll(Collection<?> c);

    boolean retainAll(Transaction tx, Collection<?> c);

    boolean atomicRetainAll(Collection<?> c);

    @Override
    void clear();

    void clear(Transaction tx);

    void atomicClear();

    @Override
    boolean equals(Object o);

    boolean equals(Transaction tx, Object o);

    boolean atomicEquals(Object o);

    @Override
    int hashCode();

    int hashCode(Transaction tx);

    int atomicHashCode();

    @Override
    String toString();

    String toString(Transaction tx);

    String atomicToString();
}
