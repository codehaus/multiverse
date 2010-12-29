package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

public interface TransactionalList<E> extends TransactionalCollection<E>{

    E get(int index);

    E get(Transaction tx, int index);

    E set(int index, E element);

    E set(Transaction tx, int index, E element);

    E remove(int index);

    E remove(Transaction tx, int index);
}
