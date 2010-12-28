package org.multiverse.api.collections;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

public interface TransactionalStack<E> {

    Stm getStm();

    int getCapacity();

    int size();

    int size(Transaction tx);

    boolean isEmpty();

    boolean isEmpty(Transaction tx);

    void push(E item);

    void push(Transaction tx, E item);

    boolean offer(E item);

    boolean offer(Transaction tx, E item);

    E pop();

    E pop(Transaction tx);

    E poll();

    E poll(Transaction tx);

    E peek();

    E peek(Transaction tx);

    void clear();

    void clear(Transaction tx);

    String toString(Transaction tx);
}
