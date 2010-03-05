package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import java.util.concurrent.atomic.AtomicReferenceArray;

@TransactionalObject
public class TransactionalArray<E> {

    private final AtomicReferenceArray<Ref> array;

    public TransactionalArray(int length) {
        array = new AtomicReferenceArray<Ref>(length);
    }

    public E get(int index) {
        Ref ref = getRef(index);
        return (E) ref.get();
    }

    public void set(E item, int index) {
        Ref ref = getRef(index);
        ref.set(item);
    }

    @TransactionalMethod(readonly = true)
    public int length() {
        return array.length();
    }

    private Ref getRef(int index) {
        Ref ref = array.get(index);
        if (ref == null) {
            ref = new Ref();
            if (!array.compareAndSet(index, null, ref)) {
                ref = array.get(index);
            }
        }
        return ref;
    }
}
