package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public interface TransactionalList<E> extends TransactionalCollection<E>, List<E> {

    @Override
    boolean addAll(int index, Collection<? extends E> c);

    boolean addAll(Transaction tx, int index, Collection<? extends E> c);

    boolean atomicAddAll(int index, Collection<? extends E> c);

    @Override
    E get(int index);

    E get(Transaction tx, int index);

    E atomicGet(int index);

    @Override
    E set(int index, E element);

    E set(Transaction tx, int index, E element);

    E atomicSet(int index, E element);

    @Override
    void add(int index, E element);

    void add(Transaction tx, int index, E element);

    void atomicAdd(int index, E element);

    @Override
    E remove(int index);

    E remove(Transaction tx, int index);

    E atomicRemove(int index);

    @Override
    int indexOf(Object o);

    int indexOf(Transaction tx, Object o);

    int atomicIndexOf(Object o);

    @Override
    int lastIndexOf(Object o);

    int lastIndexOf(Transaction tx, Object o);

    int atomicLastIndexOf(Object o);

    @Override
    ListIterator<E> listIterator();

    ListIterator<E> listIterator(Transaction tx);

    ListIterator<E> atomicListIterator();

    @Override
    ListIterator<E> listIterator(int index);

    ListIterator<E> listIterator(Transaction tx, int index);

    ListIterator<E> atomicListIterator(int index);

    @Override
    List<E> subList(int fromIndex, int toIndex);

    List<E> subList(Transaction tx, int fromIndex, int toIndex);

    List<E> atomicSubList(int fromIndex, int toIndex);
}
