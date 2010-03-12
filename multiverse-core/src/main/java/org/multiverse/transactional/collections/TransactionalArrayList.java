package org.multiverse.transactional.collections;

import org.multiverse.transactional.arrays.TransactionalReferenceArray;
import org.multiverse.utils.TodoException;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A {@link TransactionalList} based on a (transactional) array. It is the transactional version of the
 * {@link java.util.ArrayList}.
 *
 * @author Peter Veentjer.
 * @param <E>
 */
public class TransactionalArrayList<E> implements TransactionalList<E> {

    private TransactionalReferenceArray<E> array;
    private int size;

    /**
     * Creates a new TransactionalArrayList with capacity 10. This is the same initial capacity as
     * {@link java.util.ArrayList#ArrayList()}.
     */
    public TransactionalArrayList() {
        this(10);
    }

    /**
     * Creates a new TransactionalArrayList with the provided capacity.
     *
     * @param capacity the initial capacity of the TransactionalArrayList.
     */
    public TransactionalArrayList(int capacity) {
        this.array = new TransactionalReferenceArray<E>(capacity);
    }

    @Override
    public boolean add(E e) {
        if(size ==array.length()){

        }

        array.set(size, e);
        size++;
        return true;
    }


    @Override
    public void clear() {
        throw new TodoException();
    }

    @Override
    public E set(int index, E element) {
        throw new TodoException();
    }

    @Override
    public void add(int index, E element) {
        throw new TodoException();
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }

        return array.get(index);
    }

    @Override
    public int indexOf(Object o) {
        throw new TodoException();
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new TodoException();
    }

    @Override
    public ListIterator<E> listIterator() {
        throw new TodoException();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        throw new TodoException();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new TodoException();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        throw new TodoException();
    }

    @Override
    public Iterator<E> iterator() {
        throw new TodoException();
    }

    @Override
    public Object[] toArray() {
        throw new TodoException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new TodoException();
    }

    @Override
    public boolean remove(Object o) {
        throw new TodoException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new TodoException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new TodoException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new TodoException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new TodoException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new TodoException();
    }

    @Override
    public E remove(int index) {
        throw new TodoException();
    }

    @Override
    public String toString() {
        throw new TodoException();
    }

    @Override
    public int hashCode() {
        throw new TodoException();
    }

    @Override
    public boolean equals(Object thatObj) {
        throw new TodoException();
    }
}
