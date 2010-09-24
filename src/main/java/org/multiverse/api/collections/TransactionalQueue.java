package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

import java.util.Queue;

public interface TransactionalQueue<E> extends TransactionalCollection<E>, Queue<E> {

    @Override
    boolean offer(E e);

    boolean offer(Transaction tx, E e);

    boolean atomicOffer(E e);

    @Override
    E remove();

    E remove(Transaction tx);

    E atomicRemove();

    @Override
    E poll();

    E poll(Transaction tx);

    E atomicPoll();

    @Override
    E element();

    E element(Transaction tx);

    E atomicElement();

    @Override
    E peek();

    E peek(Transaction tx);

    E atomicPeek();
}
