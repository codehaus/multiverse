package org.multiverse.stms.beta.collections;

import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.collections.TransactionalBlockingDeque;
import org.multiverse.api.collections.TransactionalList;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransactionContainer;

public class BetaTransactionalLinkedList<E> implements TransactionalBlockingDeque<E>, TransactionalList<E> {

    // ================= addFirst ============

    @Override
    public void addFirst(final E e) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            atomicAddFirst(e);
        } else {
            addFirst((BetaTransaction) tx, e);
        }
    }

    @Override
    public void putFirst(E e) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public void putLast(E e) throws InterruptedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E takeFirst() throws InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E takeLast() throws InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void put(E e) throws InterruptedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void put(Transaction tx, E e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void atomicPut() throws InterruptedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E take() throws InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int remainingCapacity() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addFirst(final Transaction tx, final E e) {
        addFirst((BetaTransaction) tx, e);
    }

    public void addFirst(final BetaTransaction tx, final E e) {
        throw new TodoException();
    }

    @Override
    public void atomicAddFirst(final E e) {
        throw new TodoException();
    }

    // ============== addList ==================

    @Override
    public void addLast(final E e) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            atomicAddLast(e);
        } else {
            addLast((BetaTransaction) tx, e);
        }
    }

    @Override
    public void addLast(final Transaction tx, final E e) {
        addLast((BetaTransaction) tx, e);
    }

    public void addLast(final BetaTransaction tx, final E e) {
        throw new TodoException();
    }

    @Override
    public void atomicAddLast(final E e) {
        throw new TodoException();
    }

    // ============= offerFirst ================

    @Override
    public boolean offerFirst(final E e) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicOfferFirst(e);
        } else {
            return offerFirst((BetaTransaction) tx, e);
        }
    }

    @Override
    public boolean offerFirst(final Transaction tx, final E e) {
        return offerFirst((BetaTransaction) tx, e);
    }

    public boolean offerFirst(final BetaTransaction tx, final E e) {
        throw new TodoException();
    }

    @Override
    public boolean atomicOfferFirst(final E e) {
        throw new TodoException();
    }

    // ============= offerLast ================

    @Override
    public boolean offerLast(final E e) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicOfferLast(e);
        } else {
            return offerLast((BetaTransaction) tx, e);
        }
    }

    @Override
    public boolean offerLast(final Transaction tx, final E e) {
        return offerLast((BetaTransaction) tx, e);
    }

    public boolean offerLast(final BetaTransaction tx, final E e) {
        throw new TodoException();
    }

    @Override
    public boolean atomicOfferLast(final E e) {
        throw new TodoException();
    }

    // ============= removeFirst ================

    @Override
    public E removeFirst() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicRemoveFirst();
        } else {
            return removeFirst((BetaTransaction) tx);
        }
    }

    @Override
    public E removeFirst(final Transaction tx) {
        return removeFirst((BetaTransaction) tx);
    }

    public E removeFirst(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicRemoveFirst() {
        throw new TodoException();
    }

    // ============= removeLast ================

    @Override
    public E removeLast() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicRemoveLast();
        } else {
            return removeLast((BetaTransaction) tx);
        }
    }

    @Override
    public E removeLast(final Transaction tx) {
        return removeLast((BetaTransaction) tx);
    }

    public E removeLast(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicRemoveLast() {
        throw new TodoException();
    }

    // ============= pollFirst ================

    @Override
    public E pollFirst() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicPollFirst();
        } else {
            return pollFirst((BetaTransaction) tx);
        }
    }

    @Override
    public E pollFirst(final Transaction tx) {
        return pollFirst((BetaTransaction) tx);
    }

    public E pollFirst(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicPollFirst() {
        throw new TodoException();
    }

    // ============= pollLast ================

    @Override
    public E pollLast() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicPollLast();
        } else {
            return pollLast((BetaTransaction) tx);
        }
    }

    @Override
    public E pollLast(final Transaction tx) {
        return pollLast((BetaTransaction) tx);
    }

    public E pollLast(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicPollLast() {
        throw new TodoException();
    }

    // ============= getFirst ================

    @Override
    public E getFirst() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicGetFirst();
        } else {
            return getFirst((BetaTransaction) tx);
        }
    }

    @Override
    public E getFirst(final Transaction tx) {
        return getFirst((BetaTransaction) tx);
    }

    public E getFirst(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicGetFirst() {
        throw new TodoException();
    }

    // ============= getlast ================

    @Override
    public E getLast() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicGetLast();
        } else {
            return getLast((BetaTransaction) tx);
        }
    }

    @Override
    public E getLast(final Transaction tx) {
        return getLast((BetaTransaction) tx);
    }

    public E getLast(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicGetLast() {
        throw new TodoException();
    }

    // ============= peekFirst ================

    @Override
    public E peekFirst() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicPeekFirst();
        } else {
            return peekFirst((BetaTransaction) tx);
        }
    }

    @Override
    public E peekFirst(final Transaction tx) {
        return peekFirst((BetaTransaction) tx);
    }

    public E peekFirst(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicPeekFirst() {
        throw new TodoException();
    }

    // ============= peekLast ================

    @Override
    public E peekLast() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicPeekLast();
        } else {
            return peekLast((BetaTransaction) tx);
        }
    }

    @Override
    public E peekLast(final Transaction tx) {
        return peekLast((BetaTransaction) tx);
    }

    public E peekLast(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicPeekLast() {
        throw new TodoException();
    }

    // ============= removeFirstOccurrence ================

    @Override
    public boolean removeFirstOccurrence(final Object o) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicRemoveFirstOccurrence(o);
        } else {
            return removeFirstOccurrence((BetaTransaction) tx, o);
        }
    }

    @Override
    public boolean removeFirstOccurrence(final Transaction tx, final Object o) {
        return removeFirstOccurrence((BetaTransaction) tx, o);
    }

    public boolean removeFirstOccurrence(final BetaTransaction tx, final Object o) {
        throw new TodoException();
    }

    @Override
    public boolean atomicRemoveFirstOccurrence(final Object o) {
        throw new TodoException();
    }

    // ============= removeLastOccurrence ================

    @Override
    public boolean removeLastOccurrence(Object o) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicRemoveLastOccurrence(o);
        } else {
            return removeLastOccurrence((BetaTransaction) tx, o);
        }
    }

    @Override
    public boolean removeLastOccurrence(final Transaction tx, Object o) {
        return removeLastOccurrence((BetaTransaction) tx, o);
    }

    public boolean removeLastOccurrence(final BetaTransaction tx, Object o) {
        throw new TodoException();
    }

    @Override
    public boolean atomicRemoveLastOccurrence(final Object o) {
        throw new TodoException();
    }

    // ============= push ================

    @Override
    public void push(final E e) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            atomicPush(e);
        } else {
            push((BetaTransaction) tx, e);
        }
    }

    @Override
    public void push(final Transaction tx, final E e) {
        push((BetaTransaction) tx, e);
    }

    public void push(final BetaTransaction tx, final E e) {
        throw new TodoException();
    }

    @Override
    public void atomicPush(final E e) {
        throw new TodoException();
    }

    // ============= pop ================

    @Override
    public E pop() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicPop();
        } else {
            return pop((BetaTransaction) tx);
        }
    }

    @Override
    public E pop(final Transaction tx) {
        return pop((BetaTransaction) tx);
    }

    public E pop(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicPop() {
        throw new TodoException();
    }

    // ============= descendingIterator ================

    @Override
    public Iterator<E> descendingIterator() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicDescendingIterator();
        } else {
            return descendingIterator((BetaTransaction) tx);
        }
    }

    @Override
    public Iterator<E> descendingIterator(final Transaction tx) {
        return descendingIterator(((BetaTransaction) tx));
    }

    public Iterator<E> descendingIterator(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public Iterator<E> atomicDescendingIterator() {
        throw new TodoException();
    }

    // ============= addAll ================

    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicAddAll(index, c);
        } else {
            return addAll((BetaTransaction) tx, index, c);
        }
    }

    @Override
    public boolean addAll(final Transaction tx, final int index, final Collection<? extends E> c) {
        return addAll((BetaTransaction) tx, index, c);
    }

    public boolean addAll(final BetaTransaction tx, final int index, final Collection<? extends E> c) {
        throw new TodoException();
    }


    @Override
    public boolean atomicAddAll(final int index, final Collection<? extends E> c) {
        throw new TodoException();
    }

    // ============= get ================

    @Override
    public E get(final int index) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicGet(index);
        } else {
            return get((BetaTransaction) tx, index);
        }
    }

    @Override
    public E get(final Transaction tx, final int index) {
        return get((BetaTransaction) tx, index);
    }

    public E get(final BetaTransaction tx, final int index) {
        throw new TodoException();
    }

    @Override
    public E atomicGet(final int index) {
        throw new TodoException();
    }

    // ============= set ================

    @Override
    public E set(final int index, final E element) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicSet(index, element);
        } else {
            return set((BetaTransaction) tx, index, element);
        }
    }

    @Override
    public E set(final Transaction tx, final int index, final E element) {
        return set((BetaTransaction) tx, index, element);
    }

    public E set(final BetaTransaction tx, final int index, final E element) {
        throw new TodoException();
    }

    @Override
    public E atomicSet(int index, E element) {
        throw new TodoException();
    }

    // ============= add ================

    @Override
    public void add(final int index, final E element) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            atomicAdd(index, element);
        } else {
            add((BetaTransaction) tx, index, element);
        }
    }

    @Override
    public void add(final Transaction tx, final int index, final E element) {
        add((BetaTransaction) tx, index, element);
    }

    public void add(final BetaTransaction tx, final int index, final E element) {
        throw new TodoException();
    }

    @Override
    public void atomicAdd(final int index, final E element) {
        throw new TodoException();
    }

    // ============= remove ================

    @Override
    public E remove(final int index) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicRemove(index);
        } else {
            return remove((BetaTransaction) tx, index);
        }
    }

    @Override
    public E remove(final Transaction tx, final int index) {
        return remove((BetaTransaction) tx, index);
    }

    public E remove(final BetaTransaction tx, final int index) {
        throw new TodoException();
    }

    @Override
    public E atomicRemove(int index) {
        throw new TodoException();
    }

    // ============= indexOf ================

    @Override
    public int indexOf(final Object o) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicIndexOf(o);
        } else {
            return indexOf((BetaTransaction) tx, o);
        }
    }

    @Override
    public int indexOf(final Transaction tx, final Object o) {
        return indexOf((BetaTransaction) tx, o);
    }

    public int indexOf(final BetaTransaction tx, final Object o) {
        throw new TodoException();
    }

    @Override
    public int atomicIndexOf(final Object o) {
        throw new TodoException();
    }

    // ============= lastIndexOf ================

    @Override
    public int lastIndexOf(final Object o) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicLastIndexOf(o);
        } else {
            return lastIndexOf((BetaTransaction) tx, o);
        }
    }

    @Override
    public int lastIndexOf(final Transaction tx, final Object o) {
        return lastIndexOf((BetaTransaction) tx, o);
    }

    public int lastIndexOf(final BetaTransaction tx, final Object o) {
        throw new TodoException();
    }

    @Override
    public int atomicLastIndexOf(final Object o) {
        throw new TodoException();
    }

    // ============= listIterator ================

    @Override
    public ListIterator<E> listIterator() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicListIterator();
        } else {
            return listIterator((BetaTransaction) tx);
        }
    }

    @Override
    public ListIterator<E> listIterator(final Transaction tx) {
        return listIterator((BetaTransaction) tx);
    }

    public ListIterator<E> listIterator(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public ListIterator<E> atomicListIterator() {
        throw new TodoException();
    }

    @Override
    public ListIterator<E> listIterator(final int index) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicListIterator(index);
        } else {
            return listIterator((BetaTransaction) tx, index);
        }
    }

    @Override
    public ListIterator<E> listIterator(final Transaction tx, final int index) {
        return listIterator((BetaTransaction) tx, index);
    }

    public ListIterator<E> listIterator(final BetaTransaction tx, final int index) {
        throw new TodoException();
    }

    @Override
    public ListIterator<E> atomicListIterator(final int index) {
        throw new TodoException();
    }

    // ============= sublist ================

    @Override
    public List<E> subList(final int fromIndex, final int toIndex) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicSubList(fromIndex, toIndex);
        } else {
            return subList((BetaTransaction) tx, fromIndex, toIndex);
        }
    }

    @Override
    public List<E> subList(final Transaction tx, final int fromIndex, final int toIndex) {
        return subList((BetaTransaction) tx, fromIndex, toIndex);
    }

    public List<E> subList(final BetaTransaction tx, final int fromIndex, final int toIndex) {
        throw new TodoException();
    }

    @Override
    public List<E> atomicSubList(final int fromIndex, final int toIndex) {
        throw new TodoException();
    }

    // ============= offer ================

    @Override
    public boolean offer(final E e) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicOffer(e);
        } else {
            return offer((BetaTransaction) tx, e);
        }
    }

    @Override
    public boolean offer(final Transaction tx, final E e) {
        return offer((BetaTransaction) tx, e);
    }

    public boolean offer(final BetaTransaction tx, final E e) {
        throw new TodoException();
    }

    @Override
    public boolean atomicOffer(final E e) {
        throw new TodoException();
    }

    // ============= remove ================

    @Override
    public E remove() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicRemove();
        } else {
            return remove((BetaTransaction) tx);
        }
    }

    @Override
    public E remove(final Transaction tx) {
        return remove((BetaTransaction) tx);
    }

    public E remove(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicRemove() {
        throw new TodoException();
    }

    // ============= poll ================


    @Override
    public E poll() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicPoll();
        } else {
            return poll((BetaTransaction) tx);
        }
    }

    @Override
    public E poll(Transaction tx) {
        return poll((BetaTransaction) tx);
    }

    public E poll(BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicPoll() {
        throw new TodoException();
    }

    // ============= element ================

    @Override
    public E element() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicPoll();
        } else {
            return poll((BetaTransaction) tx);
        }
    }

    @Override
    public E element(final Transaction tx) {
        return element((BetaTransaction) tx);
    }

    public E element(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public E atomicElement() {
        throw new TodoException();
    }

    // ============= peek ================

    @Override
    public E peek() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicPeek();
        } else {
            return peek((BetaTransaction) tx);
        }
    }

    @Override
    public E peek(final Transaction tx) {
        return peek((BetaTransaction) tx);
    }

    public E peek(final BetaTransaction tx) {
        throw new TodoException();
    }


    @Override
    public E atomicPeek() {
        throw new TodoException();
    }

    // ============= size ================

    @Override
    public int size() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicSize();
        } else {
            return size((BetaTransaction) tx);
        }
    }

    @Override
    public int size(Transaction tx) {
        return size((BetaTransaction) tx);
    }

    public int size(BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public int atomicSize() {
        throw new TodoException();
    }

    // ============= isEmpty ================

    @Override
    public boolean isEmpty() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicIsEmpty();
        } else {
            return isEmpty((BetaTransaction) tx);
        }
    }

    @Override
    public boolean isEmpty(final Transaction tx) {
        return isEmpty((BetaTransaction) tx);
    }

    public boolean isEmpty(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public boolean atomicIsEmpty() {
        return atomicSize() == 0;
    }


    // ============= contains ================

    @Override
    public boolean contains(final Object o) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicContains(o);
        } else {
            return contains((BetaTransaction) tx, o);
        }
    }

    @Override
    public boolean contains(final Transaction tx, Object o) {
        return contains((BetaTransaction) tx, o);
    }

    public boolean contains(final BetaTransaction tx, Object o) {
        throw new TodoException();
    }

    @Override
    public boolean atomicContains(Object o) {
        throw new TodoException();
    }

    // ============= iterator ================

    @Override
    public Iterator<E> iterator() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicIterator();
        } else {
            return iterator((BetaTransaction) tx);
        }
    }

    @Override
    public Iterator<E> iterator(final Transaction tx) {
        return iterator((BetaTransaction) tx);
    }

    public Iterator<E> iterator(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public Iterator<E> atomicIterator() {
        throw new TodoException();
    }

    // ============= toArray ================

    @Override
    public Object[] toArray() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicToArray();
        } else {
            return toArray((BetaTransaction) tx);
        }
    }

    @Override
    public Object[] toArray(final Transaction tx) {
        return toArray((BetaTransaction) tx);
    }

    public Object[] toArray(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public Object[] atomicToArray() {
        throw new TodoException();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicToArray(a);
        } else {
            return toArray((BetaTransaction) tx, a);
        }
    }

    @Override
    public <T> T[] toArray(final Transaction tx, final T[] a) {
        return toArray((BetaTransaction) tx, a);
    }

    public <T> T[] toArray(final BetaTransaction tx, final T[] a) {
        throw new TodoException();
    }

    @Override
    public <T> T[] atomicToArray(T[] a) {
        throw new TodoException();
    }

    // ============= add ================

    @Override
    public boolean add(final E e) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicAdd(e);
        } else {
            return add((BetaTransaction) tx, e);
        }
    }

    @Override
    public boolean add(final Transaction tx, final E e) {
        return add((BetaTransaction) tx, e);
    }

    public boolean add(final BetaTransaction tx, final E e) {
        throw new TodoException();
    }

    @Override
    public boolean atomicAdd(final E e) {
        throw new TodoException();
    }

    // ============= remove ================

    @Override
    public boolean remove(final Object o) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicRemove(o);
        } else {
            return remove((BetaTransaction) tx, o);
        }
    }

    @Override
    public boolean remove(final Transaction tx, final Object o) {
        return remove((BetaTransaction) tx, o);
    }

    public boolean remove(final BetaTransaction tx, final Object o) {
        throw new TodoException();
    }

    @Override
    public boolean atomicRemove(Object o) {
        throw new TodoException();
    }

    // ============= containsAll ================

    @Override
    public boolean containsAll(final Collection<?> c) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicContainsAll(c);
        } else {
            return containsAll((BetaTransaction) tx, c);
        }
    }

    @Override
    public boolean containsAll(final Transaction tx, final Collection<?> c) {
        return containsAll((BetaTransaction) tx, c);
    }

    public boolean containsAll(final BetaTransaction tx, final Collection<?> c) {
        throw new TodoException();
    }

    @Override
    public boolean atomicContainsAll(final Collection<?> c) {
        throw new TodoException();
    }

    // ============= addAll ================

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicAddAll(c);
        } else {
            return addAll((BetaTransaction) tx, c);
        }
    }

    @Override
    public boolean addAll(Transaction tx, Collection<? extends E> c) {
        return addAll((BetaTransaction) tx, c);
    }

    public boolean addAll(BetaTransaction tx, Collection<? extends E> c) {
        throw new TodoException();
    }

    @Override
    public boolean atomicAddAll(Collection<? extends E> c) {
        throw new TodoException();
    }

    // ============= removeAll ================

    @Override
    public boolean removeAll(Collection<?> c) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicRemoveAll(c);
        } else {
            return removeAll((BetaTransaction) tx, c);
        }
    }

    @Override
    public boolean removeAll(Transaction tx, Collection<?> c) {
        return removeAll((BetaTransaction) tx, c);
    }

    public boolean removeAll(BetaTransaction tx, Collection<?> c) {
        throw new TodoException();
    }

    @Override
    public boolean atomicRemoveAll(Collection<?> c) {
        throw new TodoException();
    }

    // ============= retainAll ================

    @Override
    public boolean retainAll(final Collection<?> c) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicRetainAll(c);
        } else {
            return retainAll((BetaTransaction) tx, c);
        }
    }

    @Override
    public boolean retainAll(final Transaction tx, final Collection<?> c) {
        return retainAll((BetaTransaction) tx, c);
    }

    public boolean retainAll(final BetaTransaction tx, final Collection<?> c) {
        throw new TodoException();
    }

    @Override
    public boolean atomicRetainAll(final Collection<?> c) {
        throw new TodoException();
    }

    // ============= clear ================

    @Override
    public void clear() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            atomicClear();
        } else {
            clear((BetaTransaction) tx);
        }
    }

    @Override
    public void clear(final Transaction tx) {
        clear((BetaTransaction) tx);
    }

    public void clear(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public void atomicClear() {
        throw new TodoException();
    }

    // ============= equals ================

    @Override
    public boolean equals(final Object o) {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicEquals(o);
        } else {
            return equals((BetaTransaction) tx, o);
        }
    }

    @Override
    public boolean equals(final Transaction tx, final Object o) {
        return equals((BetaTransaction) tx, o);
    }

    public boolean equals(final BetaTransaction tx, final Object o) {
        throw new TodoException();
    }

    @Override
    public boolean atomicEquals(final Object o) {
        throw new TodoException();
    }

    // ============= hashCode ================

    @Override
    public int hashCode() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicHashCode();
        } else {
            return hashCode((BetaTransaction) tx);
        }
    }

    @Override
    public int hashCode(final Transaction tx) {
        return hashCode((BetaTransaction) tx);
    }

    public int hashCode(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public int atomicHashCode() {
        throw new TodoException();
    }

    // ============= toString ================

    public String toString() {
        final ThreadLocalTransaction.Container container = getThreadLocalTransactionContainer();
        final Transaction tx = container.transaction;
        if (tx == null || !tx.isAlive()) {
            return atomicToString();
        } else {
            return toString((BetaTransaction) tx);
        }
    }

    @Override
    public String toString(final Transaction tx) {
        return toString((BetaTransaction)tx);
    }

    public String toString(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public String atomicToString() {
        throw new TodoException();
    }
    
}
