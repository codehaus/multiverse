package org.multiverse.api.collections;

import java.util.Collection;

/**
 * A Factory for creating collections. The advantage of using a Factory is that there is no connection to the
 * concrete implementations. 
 */
public interface CollectionsFactory {

    <E> TransactionalList<E> newLinkedList();

    <E> TransactionalList<E> newLinkedList(Collection<? extends E> c);

    <E> TransactionalDeque<E> newLinkedDeque();

    <E> TransactionalDeque<E> newLinkedDeque(Collection<? extends E> c);

    <E> TransactionalBlockingDeque<E> newLinkedBlockingDeque();

    <E> TransactionalBlockingDeque<E> newLinkedBlockingDeque(int capacity);

    <E> TransactionalBlockingDeque<E> newLinkedBlockingDeque(Collection<? extends E> c);

    <E> TransactionalQueue<E> newLinkedQueue();

    <E> TransactionalQueue<E> newLinkedQueue(Collection<? extends E> c);

    <E> TransactionalBlockingQueue<E> newLinkedBlockingQueue();

    <E> TransactionalBlockingQueue<E> newLinkedBlockingQueue(int capacity);

    <E> TransactionalBlockingQueue<E> newLinkedBlockingQueue(Collection<? extends E> c);
}
