package org.multiverse.transactional.collections;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.utils.TodoException;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBlockingDeque<E> extends AbstractCollection<E> implements BlockingDeque<E> {

    @Override
    @TransactionalMethod(interruptible = true)
    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod(interruptible = true)
    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod
    public boolean addAll(Collection<? extends E> c) {
        return super.addAll(c);
    }

    @Override
    @TransactionalMethod
    public void addFirst(E e) {
        if (hasNoStorageCapacity()) {
            throw new IllegalStateException();
        }

        doAddFirst(e);
    }

    protected abstract void doAddFirst(E e);

    @Override
    @TransactionalMethod
    public void addLast(E e) {
        if (hasNoStorageCapacity()) {
            throw new IllegalStateException();
        }

        doAddLast(e);
    }

    @Override
    @TransactionalMethod
    public boolean offerFirst(E e) {
        if (hasNoStorageCapacity()) {
            return false;
        }

        doAddFirst(e);
        return true;
    }

    @Override
    @TransactionalMethod
    public boolean offerLast(E e) {
        if (hasNoStorageCapacity()) {
            return false;
        }

        doAddLast(e);
        return true;
    }

    abstract protected void doAddLast(E e);

    @Override
    @TransactionalMethod(interruptible = true)
    public void putFirst(E e) throws InterruptedException {
        if (hasNoStorageCapacity()) {
            retry();
        }

        doAddFirst(e);
    }

    @Override
    @TransactionalMethod(interruptible = true)
    public void putLast(E e) throws InterruptedException {
        if (hasNoStorageCapacity()) {
            retry();
        }

        doAddLast(e);
    }

    @Override
    @TransactionalMethod(interruptible = true)
    public E takeFirst() throws InterruptedException {
        if (isEmpty()) {
            retry();
        }

        return doRemoveFirst();
    }

    protected abstract E doRemoveFirst();

    @Override
    @TransactionalMethod(interruptible = true)
    public E takeLast() throws InterruptedException {
        if (isEmpty()) {
            retry();
        }

        return doRemoveLast();
    }

    protected abstract E doRemoveLast();

    @Override
    @TransactionalMethod
    public E removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return doRemoveFirst();
    }

    @Override
    @TransactionalMethod
    public E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return doRemoveLast();
    }

    @Override
    @TransactionalMethod
    public boolean removeFirstOccurrence(Object o) {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod
    public boolean removeLastOccurrence(Object o) {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    @TransactionalMethod
    protected boolean hasNoStorageCapacity() {
        return remainingCapacity() == 0;
    }

    @Override
    @TransactionalMethod
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    @TransactionalMethod
    public void put(E e) throws InterruptedException {
        putLast(e);
    }

    @Override
    @TransactionalMethod
    public E remove() {
        return removeFirst();
    }

    @Override
    @TransactionalMethod
    public E poll() {
        return pollFirst();
    }

    @Override
    @TransactionalMethod
    public E pollFirst() {
        if (isEmpty()) {
            return null;
        }

        return doRemoveFirst();
    }

    @Override
    @TransactionalMethod
    public E pollLast() {
        if (isEmpty()) {
            return null;
        }

        return doRemoveLast();
    }

    @Override
    @TransactionalMethod(interruptible = true)
    public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod(interruptible = true)
    public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod(interruptible = true)
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod(interruptible = true)
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod(interruptible = true)
    public E take() throws InterruptedException {
        return takeFirst();
    }

    @Override
    @TransactionalMethod(readonly = true)
    public E element() {
        return getFirst();
    }

    @Override
    @TransactionalMethod(readonly = true)
    public E peek() {
        return peekFirst();
    }

    @Override
    @TransactionalMethod
    public void push(E e) {
        addFirst(e);
    }

    @Override
    @TransactionalMethod
    public int drainTo(Collection<? super E> c) {
        for (E item : this) {
            c.add(item);
        }

        int oldSize = size();
        clear();
        return oldSize;
    }

    @Override
    @TransactionalMethod
    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new TodoException();
    }

    @Override
    @TransactionalMethod
    public E pop() {
        return removeFirst();
    }

    @Override
    @TransactionalMethod(readonly = true)
    public E getFirst() {
        if (size() == 0) {
            throw new NoSuchElementException();
        }

        return peekFirst();
    }

    @Override
    @TransactionalMethod(readonly = true)
    public E getLast() {
        if (size() == 0) {
            throw new NoSuchElementException();
        }

        return peekLast();
    }
}
