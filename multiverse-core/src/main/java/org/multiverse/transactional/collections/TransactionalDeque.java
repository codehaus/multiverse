package org.multiverse.transactional.collections;

import org.multiverse.annotations.TransactionalMethod;

import java.util.concurrent.BlockingDeque;

/**
 * A Transactional version of the {@link BlockingDeque} interface.
 *
 * @author Peter Veentjer.
 * @param <E>
 */
public interface TransactionalDeque<E> extends TransactionalCollection<E>, BlockingDeque<E> {

    @Override
    @TransactionalMethod(readonly = true)
    int remainingCapacity();

    @Override
    @TransactionalMethod(readonly = true)
    E element();

    @Override
    @TransactionalMethod(readonly = true)
    E peek();
}

