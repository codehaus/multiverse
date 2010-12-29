package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

public interface TransactionalSet<E> extends TransactionalCollection<E>{

    boolean contains(Object item);

    boolean contains(Transaction tx, Object o);

    boolean remove(Object item);

    boolean remove(Transaction tx, Object item);
}
