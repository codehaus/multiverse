package org.multiverse.stms.beta.collections;

import org.multiverse.api.collections.CollectionsFactory;

import java.util.Collection;

public interface BetaCollectionsFactory extends CollectionsFactory {

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedList();

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedList(Collection<? extends E> c);

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedDeque();

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedDeque(Collection<? extends E> c);

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedBlockingDeque();

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedBlockingDeque(int capacity);

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedBlockingDeque(Collection<? extends E> c);

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedQueue();

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedQueue(Collection<? extends E> c);

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedBlockingQueue();

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedBlockingQueue(int capacity);

    @Override
    <E> BetaTransactionalLinkedList<E> newLinkedBlockingQueue(Collection<? extends E> c);
}
