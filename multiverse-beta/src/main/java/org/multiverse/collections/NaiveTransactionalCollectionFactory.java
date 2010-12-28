package org.multiverse.collections;

import org.multiverse.api.collections.*;
import org.multiverse.stms.beta.BetaStm;

public final class NaiveTransactionalCollectionFactory implements TransactionalCollectionsFactory {

    private final BetaStm stm;

    public NaiveTransactionalCollectionFactory(BetaStm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
    }

    @Override
    public <E> TransactionalStack<E> newStack() {
        return new NaiveTransactionalStack<E>(stm);
    }

    @Override
    public <E> TransactionalStack<E> newStack(int capacity) {
        return new NaiveTransactionalStack<E>(stm, capacity);
    }

    @Override
    public <E> TransactionalQueue<E> newQueue() {
        return new NaiveTransactionalLinkedList<E>(stm);
    }

    @Override
    public <E> TransactionalQueue<E> newQueue(int capacity) {
        return new NaiveTransactionalLinkedList<E>(stm, capacity);
    }

    @Override
    public <E> TransactionalDeque<E> newDeque() {
        return new NaiveTransactionalLinkedList<E>(stm);
    }

    @Override
    public <E> TransactionalDeque<E> newDeque(int capacity) {
        return new NaiveTransactionalLinkedList<E>(stm, capacity);
    }

    @Override
    public <E> TransactionalSet<E> newHashSet() {
        return new NaiveTransactionalHashSet<E>(stm);
    }

    @Override
    public <K, V> TransactionalMap<K, V> newHashMap() {
        return new NaiveTransactionalHashMap<K, V>(stm);
    }
}
