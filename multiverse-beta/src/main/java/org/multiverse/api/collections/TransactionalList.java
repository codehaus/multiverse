package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

public interface TransactionalList<E> extends TransactionalDeque<E>{

    E get(int index);

    E get(Transaction tx, int index);
}
