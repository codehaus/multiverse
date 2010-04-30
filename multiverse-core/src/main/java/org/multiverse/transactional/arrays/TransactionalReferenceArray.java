package org.multiverse.transactional.arrays;

import org.multiverse.annotations.NonTransactional;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.programmatic.ProgrammaticReference;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.transactional.TransactionalReference;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * An Transactional array. The elements in the array are of type {@link TransactionalReference}.
 * The elements are created eagerly. In the future some kind of lazy creation mechanism will be added
 * as well, but for the time being this is the simplest solution.
 * <p/>
 * Just as with normal arrays, the length of the TransactionalReferenceArray can't change after
 * is is created. See the {@link org.multiverse.transactional.collections.TransactionalArrayList} for
 * an alternative.
 *
 * @author Peter Veentjer.
 * @param <E>
 */
@TransactionalObject
public final class TransactionalReferenceArray<E> {

    private final static ProgrammaticReferenceFactory referenceFactory = getGlobalStmInstance()
            .getProgrammaticReferenceFactoryBuilder()
            .build();

    private final ProgrammaticReference<E>[] array;

    /**
     * Creates a new TransactionalReferenceArray with the specified length.
     *
     * @param length the length of the TransactionalReferenceArray.
     * @throws IllegalArgumentException if length smaller than 0.
     */
    public TransactionalReferenceArray(int length) {
        if (length < 0) {
            throw new IllegalArgumentException();
        }

        array = new ProgrammaticReference[length];
        for (int k = 0; k < length; k++) {
            array[k] = referenceFactory.atomicCreateReference(null);
        }
    }

    private TransactionalReferenceArray(ProgrammaticReference<E>[] array) {
        if (array == null) {
            throw new NullPointerException();
        }

        this.array = array;
    }

    /**
     * Gets the element at the specified index.
     *
     * @param index the index of the element to get.
     * @return the element at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    @TransactionalMethod(readonly = true)
    public E get(int index) {
        return array[index].get();
    }

    /**
     * Returns the currently stored item at the specified index. This call doesn't
     * look at a transaction running in the ThreadLocalTransaction.
     *
     * @param index the index of the item to get
     * @return the value of the item
     * @throws IndexOutOfBoundsException if index is out of bounds.
     */
    @NonTransactional
    public E atomicGet(int index) {
        return array[index].atomicGet();
    }

    /**
     * Sets the element at the specified index.
     * <p/>
     * If a transaction currently is running in the ThreadLocalTransaction, that transaction
     * is used, otherwise this call is executed atomically (and very very fast).
     *
     * @param index the index of the element to set.
     * @param item  the item to set.
     * @return the previous content of element at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    public E set(int index, E item) {
        return array[index].set(item);
    }

    /**
     * Atomically sets the element at the specified index. If a transaction
     * is running in the ThreadLocalTransaction, it is ignored. That is why
     * this call is very fast.
     *
     * @param index  the index of the element to atomically set.
     * @param update the new value
     * @return the previous stored value.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    @NonTransactional
    public E atomicSet(int index, E update) {
        //it doesn't need a transaction, so it is excluded.
        return array[index].atomicSet(update);
    }

    /**
     * Executes an atomic compare and set, so this call doesn't look at a running transaction,
     * but essentially runs with its own transaction.
     *
     * @param index    the index of the reference where the compare and set action needs to be executed on.
     * @param expected the value the reference is expected to have.
     * @param update   the new value
     * @return true if it was a success, false otherwise.
     * @throws IndexOutOfBoundsException if index is smaller than 0, or larger than array.size.
     */
    @NonTransactional
    public boolean atomicCompareAndSet(int index, E expected, E update) {
        //it doesn't need a transaction, so it is excluded.
        return array[index].atomicCompareAndSet(expected, update);
    }

    /**
     * Returns the length of this TransactionalReferenceArray .
     *
     * @return the length of this TransactionalReferenceArray .
     */
    @NonTransactional
    public int length() {
        //it doesn't need a transaction, so it is excluded.
        return array.length;
    }

    /**
     * Shifts all the items in the array one to the left (so essentially removes items).
     *
     * @param firstIndex the first index of the item to shift to the left.
     * @param lastIndex  the index of the last item (inclusive) to shift to the left.
     * @throws IndexOutOfBoundsException if firstIndex smaller than 1, or firstIndex larger or equal to
     *                                   the array.length.
     */
    public void shiftLeft(int firstIndex, int lastIndex) {
        if (firstIndex < 1 || firstIndex >= array.length) {
            throw new IndexOutOfBoundsException();
        }

        //[a,b,c
        //[b,c,null
        for (int k = firstIndex; k <= lastIndex; k++) {
            E right = array[k].get();
            array[k - 1].set(right);
        }

        array[lastIndex].set(null);
    }

    /**
     * Create room
     *
     * @param firstIndex
     * @param lastIndex
     */
    public void shiftRight(int firstIndex, int lastIndex) {
        if (firstIndex < 0 || firstIndex >= array.length - 1) {
            throw new IndexOutOfBoundsException();
        }

        //[a   , b, c
        //[null, a, b, c

        //endIndex not checked
        for (int k = lastIndex; k >= firstIndex; k--) {
            E left = array[k].get();
            array[k + 1].set(left);
        }

        array[firstIndex].set(null);
    }

    /**
     * Copies the content of this TransactionalReferenceArray to a bigger TransactionalReferenceArray.
     *
     * @param newLength the new capacity of the bigger TransactionalReferenceArray
     * @return the new TransactionalReferenceArray
     * @throws IllegalArgumentException if newCapacity is smaller than the current capacity.
     */
    public TransactionalReferenceArray<E> copyToBiggerArray(int newLength) {
        if (newLength < array.length) {
            throw new IllegalArgumentException();
        }

        ProgrammaticReference[] newArray = new ProgrammaticReference[newLength];
        System.arraycopy(array, 0, newArray, 0, array.length);

        for (int k = array.length; k < newLength; k++) {
            newArray[k] = referenceFactory.atomicCreateReference(null);
        }

        return new TransactionalReferenceArray<E>(newArray);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public String toString() {
        int length = length();
        if (length == 0) {
            return "[]";
        }

        StringBuffer sb = new StringBuffer("[");
        for (int k = 0; k < length; k++) {
            sb.append(array[k].get());

            if (k < length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Sends back an array containing the value of the references in this TransactionalReferenceArray.
     *
     * @param size the number of items to copy. If the size is smaller than the array length, it will
     *             only copy till that length.
     * @return the copied array.
     * @throws IllegalArgumentException if size smaller than 0 or larger than the length
     */
    @TransactionalMethod(readonly = true)
    public Object[] toArray(int size) {
        if (size < 0 || size > array.length) {
            throw new IllegalArgumentException();
        }

        Object[] result = new Object[size];
        for (int k = 0; k < size; k++) {
            result[k] = array[k].get();
        }
        return result;
    }
}
