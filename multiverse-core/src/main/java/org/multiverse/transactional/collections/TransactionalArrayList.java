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
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean add(E e) {
        ensureCapacity(size + 1);
        array.set(size, e);
        size++;
        return true;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > array.length()) {

            int oldCapacity = array.length();

            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }

            System.out.println("growing: " + newCapacity);
            array = array.copyToBiggerArray(newCapacity);
        }
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }

        return array.get(index);
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public int indexOf(Object o) {
        int copiedSize = size;

        for (int k = 0; k < copiedSize; k++) {
            E element = array.get(k);
            if (equals(element, o)) {
                return k;
            }
        }

        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int copiedSize = size;

        for (int k = copiedSize - 1; k >= 0; k--) {
            E element = array.get(k);
            if (equals(element, o)) {
                return k;
            }
        }

        return -1;
    }

    private boolean equals(E element, Object o) {
        return element == null ? o == null : element.equals(o);
    }

    @Override
    public E set(int index, E element) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }

        return array.set(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (c.isEmpty()) {
            return false;
        }

        int oldSize = size;
        int newSize = oldSize + c.size();
        ensureCapacity(newSize);
        size = newSize;

        Iterator<? extends E> it = c.iterator();
        for (int k = oldSize; k < newSize; k++) {
            array.set(k, it.next());
        }

        return true;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }

        for (int k = 0; k < size; k++) {
            array.set(0, null);
        }
        size = 0;
    }

    @Override
    public void add(int index, E element) {
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
    public int hashCode() {
        int localSize = size;
        int hashCode = 1;

        if (localSize == 0) {
            return hashCode;
        }

        for (int k = 0; k < localSize; k++) {
            E item = array.get(k);
            hashCode = 31 * hashCode + (item == null ? 0 : item.hashCode());
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof List)) {
            return false;
        }

        List that = (List) thatObj;
        if (that.size() != size) {
            return false;
        }

        throw new TodoException();
    }

    @Override
    public String toString() {
        int localSize = size;

        if (localSize == 0) {
            return "[]";
        }

        StringBuffer sb = new StringBuffer("[");
        for (int k = 0; k < localSize; k++) {
            sb.append(array.get(k));

            if (k < localSize - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
