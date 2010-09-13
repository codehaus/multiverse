package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public interface TransactionalBlockingQueue<E> extends TransactionalQueue<E>, BlockingQueue<E> {

    @Override
    void put(E e) throws InterruptedException;

    void put(Transaction tx, E e);

    void atomicPut()throws InterruptedException;

    //todo: all methods needs to be transformed.

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
}
