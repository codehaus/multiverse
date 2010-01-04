package org.multiverse.transactional.collections;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.utils.TodoException;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    protected abstract E doRemove();

    protected abstract void doAdd(E item);

    protected abstract boolean isFull();

    @Override
    @TransactionalMethod
    public boolean offer(E e) {
        if (isFull()) {
            return false;
        }

        doAdd(e);
        return true;
    }

    @Override
    @TransactionalMethod
    public void put(E e) throws InterruptedException {
        if (isFull()) {
            retry();
        }

        doAdd(e);
    }

    @Override
    @TransactionalMethod
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod
    public E take() throws InterruptedException {
        if (isEmpty()) {
            retry();
        }

        return doRemove();
    }

    @Override
    @TransactionalMethod
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod
    public int drainTo(Collection<? super E> c) {
        int drainSize = size();

        if (drainSize == 0) {
            return 0;
        }

        for (E item : this) {
            c.add(item);
        }

        return drainSize;
    }

    @Override
    @TransactionalMethod
    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod
    public E poll() {
        if (isEmpty()) {
            return null;
        }

        return doRemove();
    }
}
