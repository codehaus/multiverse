package org.multiverse.transactional.collections;

import org.multiverse.annotations.TransactionalMethod;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A transactional {@link BlockingQueue} interface.
 *
 * @author Peter Veentjer.
 * @param <E>
 * @see org.multiverse.transactional.collections.TransactionalCollection
 * @see java.util.Collection
 * @see java.util.concurrent.BlockingQueue
 * @see java.util.Queue
 */
public interface TransactionalQueue<E> extends BlockingQueue<E>, TransactionalCollection<E> {

    @Override
    @TransactionalMethod(readonly = true)
    E element();

    @Override
    @TransactionalMethod(readonly = true)
    E peek();

    @Override
    boolean offer(E e);

    @Override
    void put(E e) throws InterruptedException;

    @Override
    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    E take() throws InterruptedException;

    @Override
    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    int remainingCapacity();

    @Override
    int drainTo(Collection<? super E> c);

    @Override
    int drainTo(Collection<? super E> c, int maxElements);

    @Override
    E remove();

    @Override
    E poll();
}
