package org.multiverse.transactional.collections;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * A Transactional version of the {@link BlockingDeque} interface.
 *
 * @author Peter Veentjer.
 * @param <E>
 * @see org.multiverse.transactional.collections.TransactionalCollection
 * @see java.util.concurrent.BlockingDeque
 * @see java.util.Queue
 * @see java.util.Collection
 */
public interface TransactionalDeque<E> extends TransactionalQueue<E>, BlockingDeque<E> {

    @Override
    void addFirst(E e);

    @Override
    void addLast(E e);

    @Override
    boolean offerFirst(E e);

    @Override
    boolean offerLast(E e);

    @Override
    void putFirst(E e) throws InterruptedException;

    @Override
    void putLast(E e) throws InterruptedException;

    @Override
    boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    E takeFirst() throws InterruptedException;

    @Override
    E takeLast() throws InterruptedException;

    @Override
    E pollFirst(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    E pollLast(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    boolean removeFirstOccurrence(Object o);

    @Override
    boolean removeLastOccurrence(Object o);
}

