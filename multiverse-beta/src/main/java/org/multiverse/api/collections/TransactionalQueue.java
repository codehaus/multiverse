package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

public interface TransactionalQueue<E> {

    Stm getStm();

    int size();

    int size(Transaction tx);

    boolean isEmpty();

    boolean isEmpty(Transaction tx);

    int getCapacity();

     boolean offer(E item);

    boolean offer(Transaction tx, E item);

    void put(E item);

    void put(Transaction tx, E item);

    E take();

    E take(Transaction tx);

    E poll();

    E poll(Transaction tx);

    E peek();

    E peek(Transaction tx);

    void clear();

    void clear(Transaction tx);
}
