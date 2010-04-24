package org.multiverse.transactional.collections;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.utils.TodoException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.multiverse.api.StmUtils.retry;

public abstract class AbstractTransactionalDeque<E>
        implements TransactionalDeque<E> {

    @Override
    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public void addFirst(E e) {
        if (hasNoStorageCapacity()) {
            throw new IllegalStateException();
        }

        doAddFirst(e);
    }

    protected abstract void doAddFirst(E e);

    @Override
    public void addLast(E e) {
        if (hasNoStorageCapacity()) {
            throw new IllegalStateException();
        }

        doAddLast(e);
    }

    @Override
    public boolean offerFirst(E e) {
        if (hasNoStorageCapacity()) {
            return false;
        }

        doAddFirst(e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        if (hasNoStorageCapacity()) {
            return false;
        }

        doAddLast(e);
        return true;
    }

    abstract protected void doAddLast(E e);

    @Override
    public void putFirst(E e) throws InterruptedException {
        if (hasNoStorageCapacity()) {
            //force load or the size to listen on that field
            size();
            retry();
        }

        doAddFirst(e);
    }

    @Override
    public void putLast(E e) throws InterruptedException {
        if (hasNoStorageCapacity()) {
            //force load or the size to listen on that field
            size();
            retry();
        }

        doAddLast(e);
    }

    @Override
    public E takeFirst() throws InterruptedException {
        if (isEmpty()) {
            retry();
        }

        return doRemoveFirst();
    }

    protected abstract E doRemoveFirst();

    @Override
    public E takeLast() throws InterruptedException {
        if (isEmpty()) {
            retry();
        }

        return doRemoveLast();
    }

    protected abstract E doRemoveLast();

    @Override
    public E removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return doRemoveFirst();
    }

    @Override
    public E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        return doRemoveLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        throw new TodoException();
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        throw new TodoException();
    }

    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    protected boolean hasNoStorageCapacity() {
        return remainingCapacity() == 0;
    }

    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    public void put(E e) throws InterruptedException {
        putLast(e);
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E pollFirst() {
        if (isEmpty()) {
            return null;
        }

        return doRemoveFirst();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public E pollLast() {
        if (isEmpty()) {
            return null;
        }

        return doRemoveLast();
    }

    @Override
    public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public E take() throws InterruptedException {
        return takeFirst();
    }

    public E takeUninterruptible() {
        if (isEmpty()) {
            retry();
        }

        return doRemoveFirst();
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
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        if (isEmpty()) {
            return 0;
        }

        for (E item : this) {
            c.add(item);
        }

        int oldSize = size();
        clear();
        return oldSize;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new TodoException();
    }

    @Override
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

    /**
     * Returns an iterator over the elements contained in this collection.
     *
     * @return an iterator over the elements contained in this collection
     */
    public abstract Iterator<E> iterator();

    public abstract int size();

    public boolean contains(Object o) {
        Iterator<E> e = iterator();
        if (o == null) {
            while (e.hasNext()) {
                if (e.next() == null) {
                    return true;
                }
            }
        } else {
            while (e.hasNext()) {
                if (o.equals(e.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Object[] toArray() {
        // Estimate size of array; be prepared to see more or fewer elements
        Object[] r = new Object[size()];
        Iterator<E> it = iterator();
        for (int i = 0; i < r.length; i++) {
            if (!it.hasNext()) {   // fewer elements than expected
                return Arrays.copyOf(r, i);
            }
            r[i] = it.next();
        }
        return it.hasNext() ? finishToArray(r, it) : r;
    }

    public <T> T[] toArray(T[] a) {
        // Estimate size of array; be prepared to see more or fewer elements
        int size = size();
        T[] r = a.length >= size ? a :
                (T[]) java.lang.reflect.Array
                        .newInstance(a.getClass().getComponentType(), size);
        Iterator<E> it = iterator();

        for (int i = 0; i < r.length; i++) {
            if (!it.hasNext()) { // fewer elements than expected
                if (a != r)
                    return Arrays.copyOf(r, i);
                r[i] = null; // null-terminate
                return r;
            }
            r[i] = (T) it.next();
        }
        return it.hasNext() ? finishToArray(r, it) : r;
    }

    private static <T> T[] finishToArray(T[] r, Iterator<?> it) {
        int i = r.length;
        while (it.hasNext()) {
            int cap = r.length;
            if (i == cap) {
                int newCap = ((cap / 2) + 1) * 3;
                if (newCap <= cap) { // integer overflow
                    if (cap == Integer.MAX_VALUE)
                        throw new OutOfMemoryError
                                ("Required array size too large");
                    newCap = Integer.MAX_VALUE;
                }
                r = Arrays.copyOf(r, newCap);
            }
            r[i++] = (T) it.next();
        }
        // trim if overallocated
        return (i == r.length) ? r : Arrays.copyOf(r, i);
    }

    // Modification Operations

    public boolean remove(Object o) {
        Iterator<E> e = iterator();
        if (o == null) {
            while (e.hasNext()) {
                if (e.next() == null) {
                    e.remove();
                    return true;
                }
            }
        } else {
            while (e.hasNext()) {
                if (o.equals(e.next())) {
                    e.remove();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        Iterator<?> e = c.iterator();
        while (e.hasNext()) {
            if (!contains(e.next())) {
                return false;
            }
        }
        return true;
    }

    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        Iterator<?> e = iterator();
        while (e.hasNext()) {
            if (c.contains(e.next())) {
                e.remove();
                modified = true;
            }
        }
        return modified;
    }

    public boolean retainAll(Collection<?> c) {
        boolean modified = false;
        Iterator<E> e = iterator();
        while (e.hasNext()) {
            if (!c.contains(e.next())) {
                e.remove();
                modified = true;
            }
        }
        return modified;
    }

    public String toString() {
        Iterator<E> i = iterator();
        if (!i.hasNext()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (; ;) {
            E e = i.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (!i.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(", ");
        }
    }
}
