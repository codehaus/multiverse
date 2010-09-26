package org.multiverse.api.collections;

import java.util.Collection;

/**
 * A Factory for creating collections. The advantage of using a Factory is that there is no connection to the
 * concrete implementations. This allows each Stm implementation to returns its own collection implementations.
 *
 * @author Peter Veentjer.
 */
public interface TransactionalCollectionsFactory {

    /**
     * Creates a new committed TransactionalList that is based on linked nodes.
     *
     * @param <E> the type of the elements in the list.
     * @return the created list.
     */
    <E> TransactionalList<E> newLinkedList();

    /**
     * Creates a new committed TransactionalList that is based on linked nodes and
     * is initialized with the given collection.
     *
     * @param c   the initial collection.
     * @param <E> the type of the elements in the list.
     * @return the created list.
     * @throws NullPointerException if c is null.
     */
    <E> TransactionalList<E> newLinkedList(Collection<? extends E> c);

    /**
     * Creates a new committed TransactionalDeque based on linked nodes.
     *
     * @param <E> the type of the elements in the deque.
     * @return the created TransactionalDeque.
     */
    <E> TransactionalDeque<E> newLinkedDeque();

    /**
     * Creates a new committed TransactionalDeque based on linked nodes and is initialized
     * with the given collection.
     *
     * @param c   the initial collection
     * @param <E> the type of the elements in the deque.
     * @return the created TransactionalDeque.
     * @throws NullPointerException if c is null.
     */
    <E> TransactionalDeque<E> newLinkedDeque(Collection<? extends E> c);

    /**
     * Creates a new committed TransactionalBlockingDeque based on linked nodes.
     *
     * @param <E> the type of the elements in the deque.
     * @return the created TransactionalBlockingDeque.
     */
    <E> TransactionalBlockingDeque<E> newLinkedBlockingDeque();

    /**
     * Creates a new committed TransactionalBlockingDeque based on linked nodes which has a bounded
     * capacity.
     *
     * @param capacity the maximum capacity of the TransactionalBlockingDeque.
     * @param <E>      the type of the elements in the deque.
     * @return the created TransactionalBlockingDeque.
     * @throws IllegalArgumentException if capacity is smaller than zero.
     */
    <E> TransactionalBlockingDeque<E> newLinkedBlockingDeque(int capacity);

    /**
     * Creates  a new committed TransactionalBlockingDeque based on linked nodes initialized with the given
     * collection.
     *
     * @param c   the initial collection to
     * @param <E> the type of the elements in the deque.
     * @return the created TransactionalBlockingDeque
     * @throws NullPointerException if c is null.
     */
    <E> TransactionalBlockingDeque<E> newLinkedBlockingDeque(Collection<? extends E> c);

    /**
     * Creates a new committed TransactionalQueue based on linked nodes.
     *
     * @param <E> the type of the elements in the queue.
     * @return the created TransactionalQueue.
     */
    <E> TransactionalQueue<E> newLinkedQueue();

    /**
     * Creates a new committed TransactionalQueue based on linked nodes and is initialized with the given
     * collection.
     *
     * @param c   the initial collection to place in the queue.
     * @param <E> the type of the elements in the queue
     * @return the created TransactionalQueue
     * @throws NullPointerException if c is null.
     */
    <E> TransactionalQueue<E> newLinkedQueue(Collection<? extends E> c);

    /**
     * Creates a new committed TransactionalBlockingQueue based on linked nodes.
     *
     * @param <E> the type of the elements in the queue.
     * @return the created TransactionalBlockingQueue.
     */
    <E> TransactionalBlockingQueue<E> newLinkedBlockingQueue();

    /**
     * Creates a new committed TransactionalBLockingQueue based on linked nodes and a maximum capacity.
     *
     * @param capacity the maximum capacity of the queue.
     * @param <E>      the type of the elements in the queue
     * @return the created TransactionalBlockingQueue
     * @throws IllegalArgumentException if capacity smaller than zero.
     */
    <E> TransactionalBlockingQueue<E> newLinkedBlockingQueue(int capacity);

    /**
     * Creates a new TransactionalBLockingQueue based on linked nodes and initialized with the given
     * collection.
     *
     * @param c   the initial collection to be placed in the queue
     * @param <E> the type of the elements in the queue
     * @return the created TransactionalBlockingQueue.
     */
    <E> TransactionalBlockingQueue<E> newLinkedBlockingQueue(Collection<? extends E> c);
}
