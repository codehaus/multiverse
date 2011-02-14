package org.multiverse.api.collections;

import org.multiverse.api.Transaction;

import java.util.Iterator;

public interface TransactionalIterator<E> extends Iterator<E> {

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @param tx Transaction used for this operation
     * @return <tt>true</tt> if the iterator has more elements.
     */
    boolean hasNext(Transaction tx);

    /**
     * Returns the next element in the iteration.
     *
     * @param tx Transaction used for this operation
     * @return the next element in the iteration.
     * @throws java.util.NoSuchElementException
     *          iteration has no more elements.
     */
    E next(Transaction tx);

    /**
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation).  This method can be called only once per
     * call to <tt>next</tt>.  The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     *
     * @param tx Transaction used for this operation
     * @throws UnsupportedOperationException if the <tt>remove</tt>
     *                                       operation is not supported by this Iterator.
     * @throws IllegalStateException         if the <tt>next</tt> method has not
     *                                       yet been called, or the <tt>remove</tt> method has already
     *                                       been called after the last call to the <tt>next</tt>
     *                                       method.
     */
    void remove(Transaction tx);
}
