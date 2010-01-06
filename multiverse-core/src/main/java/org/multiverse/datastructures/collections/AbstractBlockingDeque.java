package org.multiverse.datastructures.collections;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.utils.TodoException;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBlockingDeque<E> extends AbstractCollection<E> implements BlockingDeque<E> {

    @Override
    @AtomicMethod
    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @AtomicMethod
    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @AtomicMethod
    public boolean addAll(Collection<? extends E> c) {
        return super.addAll(c);
    }

    @Override
    @AtomicMethod
    public void addFirst(E e) {
        if (hasNoStorageCapacity()) {
            throw new IllegalStateException();
        }

        doAddFirst(e);
    }

    protected abstract void doAddFirst(E e);

    @Override
    @AtomicMethod
    public void addLast(E e) {
        if (hasNoStorageCapacity()) {
            throw new IllegalStateException();
        }

        doAddLast(e);
    }

    @Override
    @AtomicMethod
    public boolean offerFirst(E e) {
        if (hasNoStorageCapacity()) {
            return false;
        }

        doAddFirst(e);
        return true;
    }

    @Override
    @AtomicMethod
    public boolean offerLast(E e) {
        if (hasNoStorageCapacity()) {
            return false;
        }

        doAddLast(e);
        return true;
    }

    abstract protected void doAddLast(E e);

    @Override
    @AtomicMethod
    public void putFirst(E e) throws InterruptedException {
        if (hasNoStorageCapacity()) {
            retry();
        }

        doAddFirst(e);
    }

    @Override
    @AtomicMethod
    public void putLast(E e) throws InterruptedException {
        if (hasNoStorageCapacity()) {
            retry();
        }

        doAddLast(e);
    }

    @Override
    @AtomicMethod
    public E takeFirst() throws InterruptedException {
        if (isEmpty()) {
            retry();
        }

        return doRemoveFirst();
    }

    protected abstract E doRemoveFirst();

    @Override
    @AtomicMethod
    public E takeLast() throws InterruptedException {
        if (isEmpty()) {
            retry();
        }

        return doRemoveLast();
    }

    protected abstract E doRemoveLast();

    @Override
    @AtomicMethod
    public E removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return doRemoveFirst();
    }

    @Override
    @AtomicMethod
    public E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return doRemoveLast();
    }

    @Override
    @AtomicMethod
    public boolean removeFirstOccurrence(Object o) {
        throw new TodoException();
    }

    @Override
    @AtomicMethod
    public boolean removeLastOccurrence(Object o) {
        throw new TodoException();
    }

    @Override
    @AtomicMethod
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    @AtomicMethod
    protected boolean hasNoStorageCapacity() {
        return remainingCapacity() == 0;
    }

    @Override
    @AtomicMethod
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    @AtomicMethod
    public void put(E e) throws InterruptedException {
        putLast(e);
    }

    @Override
    @AtomicMethod
    public E remove() {
        return removeFirst();
    }

    @Override
    @AtomicMethod
    public E poll() {
        return pollFirst();
    }

    @Override
    @AtomicMethod
    public E pollFirst() {
        if (isEmpty()) {
            return null;
        }

        return doRemoveFirst();
    }

    @Override
    @AtomicMethod
    public E pollLast() {
        if (isEmpty()) {
            return null;
        }

        return doRemoveLast();
    }

    @Override
    @AtomicMethod
    public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @AtomicMethod
    public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @AtomicMethod
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @AtomicMethod
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    @AtomicMethod
    public E take() throws InterruptedException {
        return takeFirst();
    }

    @Override
    @AtomicMethod(readonly = true)
    public E element() {
        return getFirst();
    }

    @Override
    @AtomicMethod(readonly = true)
    public E peek() {
        return peekFirst();
    }

    @Override
    @AtomicMethod
    public void push(E e) {
        addFirst(e);
    }

    @Override
    @AtomicMethod
    public int drainTo(Collection<? super E> c) {
        for (E item : this) {
            c.add(item);
        }

        int oldSize = size();
        clear();
        return oldSize;
    }

    @Override
    @AtomicMethod
    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new TodoException();
    }

    @Override
    @AtomicMethod
    public E pop() {
        return removeFirst();
    }

    @Override
    @AtomicMethod(readonly = true)
    public E getFirst() {
        if (size() == 0) {
            throw new NoSuchElementException();
        }

        return peekFirst();
    }

    @Override
    @AtomicMethod(readonly = true)
    public E getLast() {
        if (size() == 0) {
            throw new NoSuchElementException();
        }

        return peekLast();
    }
}
