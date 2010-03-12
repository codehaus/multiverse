package org.multiverse.transactional.arrays;

import org.multiverse.annotations.Exclude;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.transactional.DefaultTransactionalReference;
import org.multiverse.transactional.TransactionalReference;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

@TransactionalObject
public final class TransactionalReferenceArray<E> {

    private final AtomicReferenceArray<TransactionalReference> array;
    private final AtomicReference<E>[] rawArray;

    public TransactionalReferenceArray(int length) {
        if(length<0){
            throw new IllegalArgumentException();
        }

        rawArray = new AtomicReference[length];
        array = new AtomicReferenceArray<TransactionalReference>(rawArray);
    }

    public E get(int index) {
        TransactionalReference<E> ref = getTransactionalReference(index);
        return ref.get();
    }

    public E set(int index, E item) {
        TransactionalReference<E> ref = getTransactionalReference(index);
        return ref.set(item);
    }

    @Exclude
    public int length() {
        return array.length();
    }


    public TransactionalReferenceArray<E> copyToBiggerArray(int newCapacity) {
        if (newCapacity < 0 || newCapacity >= array.length()) {
            throw new IllegalArgumentException();
        }

        TransactionalReference[] newArray = new TransactionalReference[newCapacity];
        System.arraycopy(array.);
        TransactionalReferenceArray<E>
    }

    //this is a method that requires its own transaction.
    private TransactionalReference<E> getTransactionalReference(int index) {
        TransactionalReference<E> ref = array.get(index);
        if (ref == null) {
            //it would be nice if the reference already was committed after it is created so it doesn't need an update
            //transaction.
            ref = new DefaultTransactionalReference<E>();
            if (!array.compareAndSet(index, null, ref)) {
                ref = array.get(index);
            }
        }
        return ref;
    }
}
