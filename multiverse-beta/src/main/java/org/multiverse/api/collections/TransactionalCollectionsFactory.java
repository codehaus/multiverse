package org.multiverse.api.collections;

import org.multiverse.api.Stm;

public interface TransactionalCollectionsFactory {

    Stm getStm();

    <E> TransactionalStack<E> newStack();

    <E> TransactionalStack<E> newStack(int capacity);

    <E> TransactionalQueue<E> newQueue();

    <E> TransactionalQueue<E> newQueue(int capacity);

    <E> TransactionalDeque<E> newDeque();

    <E> TransactionalDeque<E> newDeque(int capacity);

    <E> TransactionalSet<E> newHashSet();

    <K, V> TransactionalMap<K, V> newHashMap();

    <E> TransactionalList<E> newLinkedList();
}
