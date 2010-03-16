package org.multiverse.transactional.collections;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.transactional.arrays.TransactionalReferenceArray;
import org.multiverse.utils.TodoException;

import java.lang.reflect.Array;
import java.util.*;

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
     * Creates a new TransactionalArrayList that contains the items.
     *
     * @param items the items to add to this List.
     * @throws NullPointerException if items is null.
     */
    public TransactionalArrayList(E... items) {
        this(items.length);

        for (E item : items) {
            add(item);
        }
    }

    /**
     * Creates a new TransactionalArrayList that contains the items.
     *
     * @param items the items to add to this List.
     * @throws NullPointerException if items is null.
     */
    public TransactionalArrayList(Collection<? extends E> items) {
        this(items.size());

        for (E item : items) {
            add(item);
        }
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

            array = array.copyToBiggerArray(newCapacity);
        }
    }

    @Override
    @TransactionalMethod(readonly = true)
    //TODO: needs to be removed as the interface inheritance works
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

    private static boolean equals(Object element, Object o) {
        return element == null ? o == null : element.equals(o);
    }

    @Override
    @TransactionalMethod(automaticReadTracking = false)
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
    public Object[] toArray() {
        return array.toArray(size);
    }

    @Override
    public <T> T[] toArray(T[] a) {

        int size = this.size;

        T[] r = a.length >= size ? a : (T[]) Array.newInstance(a.getClass().getComponentType(), size);

        for (int k = 0; k < size; k++) {
            r[k] = (T) array.get(k);
        }

        for (int k = size; k < a.length; k++) {
            r[k] = null;
        }

        return r;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (c.isEmpty()) {
            return true;
        }

        for (Iterator it = c.iterator(); it.hasNext();) {
            if (!contains(it.next())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean remove(Object o) {
        int indexOf = indexOf(o);
        if (indexOf == -1) {
            return false;
        }

        remove(indexOf);
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (c.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (Object item : c) {
            //can be made more efficient by using the previous found index instead
            //of searching from the beginning again.
            while (remove(item)) {
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public E remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }

        E item = array.get(index);

        if (index < size - 1) {
            array.shiftLeft(index + 1, size - 1);
        } else {
            array.set(index, null);
        }

        size--;
        return item;
    }

    @Override
    public void add(int index, E element) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException();
        }

        ensureCapacity(size + 1);

        array.shiftRight(index, size - 1);

        size++;
        array.set(index, element);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException();
        }

        if (c == null) {
            throw new NullPointerException();
        }

        if (c.isEmpty()) {
            return false;
        }

        ensureCapacity(size + c.size());

        //array.shiftRight(index, size - index);
        //array.set(index, element);
        //size+=c.size();
        //return true;
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
        return new IteratorImpl();
    }

    @TransactionalObject
    private class IteratorImpl implements Iterator<E> {

        private int index;

        @Override
        public boolean hasNext() {
            return index < size();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            E item = get(index);
            index++;
            return item;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (isEmpty()) {
            return false;
        }

        if (c.isEmpty()) {
            clear();
            return true;
        }

        boolean changed = false;

        for (int k = 0; k < size; k++) {
            E item = array.get(k);
            if (!c.contains(item)) {
                remove(k);
                k--;
                changed = true;
            }
        }

        return changed;
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

        if (size == 0) {
            return true;
        }

        Iterator thatIt = that.iterator();
        for (int k = 0; k < size; k++) {
            if (!equals(array.get(k), thatIt.next())) {
                return false;
            }
        }

        return true;
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
