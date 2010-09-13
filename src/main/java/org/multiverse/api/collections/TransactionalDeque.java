package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

import java.util.Deque;
import java.util.Iterator;

public interface TransactionalDeque<E> extends TransactionalQueue<E>, Deque<E> {

    @Override
    void addFirst(E e);

    void addFirst(Transaction tx, E e);

    void atomicAddFirst(E e);

    @Override
    void addLast(E e);

    void addLast(Transaction tx, E e);

    void atomicAddLast(E e);

    @Override
    boolean offerFirst(E e);

    boolean offerFirst(Transaction tx, E e);

    boolean atomicOfferFirst(E e);

    @Override
    boolean offerLast(E e);

    boolean offerLast(Transaction tx, E e);

    boolean atomicOfferLast(E e);

    @Override
    E removeFirst();

    E removeFirst(Transaction tx);

    E atomicRemoveFirst();

    @Override
    E removeLast();

    E removeLast(Transaction tx);

    E atomicRemoveLast();

    @Override
    E pollFirst();

    E pollFirst(Transaction tx);

    E atomicPollFirst();

    @Override
    E pollLast();

    E pollLast(Transaction tx);

    E atomicPollLast();

    @Override
    E getFirst();

    E getFirst(Transaction tx);

    E atomicGetFirst();

    @Override
    E getLast();

    E getLast(Transaction tx);

    E atomicGetLast();

    @Override
    E peekFirst();

    E peekFirst(Transaction tx);

    E atomicPeekFirst();

    @Override
    E peekLast();

    E peekLast(Transaction tx);

    E atomicPeekLast();

    @Override
    boolean removeFirstOccurrence(Object o);

    boolean removeFirstOccurrence(Transaction tx, Object o);

    boolean atomicRemoveFirstOccurrence(Object o);

    @Override
    boolean removeLastOccurrence(Object o);

    boolean removeLastOccurrence(Transaction tx, Object o);

    boolean atomicRemoveLastOccurrence(Object o);

    @Override
    void push(E e);

    void push(Transaction tx, E e);

    void atomicPush(E e);

    @Override
    E pop();

    E pop(Transaction tx);

    E atomicPop();

    @Override
    Iterator<E> descendingIterator();

    Iterator<E> descendingIterator(Transaction tx);

    Iterator<E> atomicDescendingIterator();
}
