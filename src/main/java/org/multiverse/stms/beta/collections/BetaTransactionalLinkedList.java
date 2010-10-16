package org.multiverse.stms.beta.collections;

import org.multiverse.api.Transaction;
import org.multiverse.api.collections.TransactionalBlockingDeque;
import org.multiverse.api.collections.TransactionalList;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.references.Ref;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.multiverse.api.StmUtils.newRef;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class BetaTransactionalLinkedList<E> implements
        TransactionalBlockingDeque<E>, TransactionalList<E>, BetaStmConstants {

    // ================= addFirst ============

    private final BetaRef<Node<E>> headRef;
    private final BetaRef<Node<E>> tailRef;
    private final BetaStm stm;
    private final BetaIntRef sizeRef;
    private final int capacity;

    public BetaTransactionalLinkedList(BetaStm stm, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        headRef = new BetaRef<Node<E>>(stm);
        tailRef = new BetaRef<Node<E>>(stm);
        sizeRef = new BetaIntRef(stm);

        this.capacity = capacity;
        this.stm = stm;
    }

    @Override
    public BetaStm getStm() {
        return stm;
    }

    @Override
    public void addFirst(final E e) {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        addFirst((BetaTransaction) tx, e);
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

    @Override
    public void putFirst(E e) {
        throw new TodoException();
    }

    @Override
    public void putLast(E e) {
        Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        putLast((BetaTransaction) tx, e);
    }


    @Override
    public void putLast(Transaction tx, E e) {
        putLast((BetaTransaction) tx, e);
    }

    public void putLast(BetaTransaction tx, E e) {
        throw new TodoException();
    }

    @Override
    public void atomicPutLast(E e) {
        throw new TodoException();
    }

    @Override
    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public E takeFirst() {
        Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return takeFirst(tx);
    }

    @Override
    public E takeFirst(Transaction tx) {
        return takeFirst((BetaTransaction) tx);
    }

    public E takeFirst(BetaTransaction tx) {
        E item = pollFirst(tx);

        if (item == null) {
            retry();
        }

        return item;
    }

    @Override
    public E atomicTakeFirst() throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public E takeLast() {
        Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return takeLast(tx);
    }

    @Override
    public E takeLast(Transaction tx) {
        return takeLast((BetaTransaction) tx);
    }

    public E takeLast(BetaTransaction tx) {
        E item = pollLast(tx);
        if (item == null) {
            retry();
        }

        return item;
    }

    @Override
    public E atomicTakeLast() throws InterruptedException {
        throw new TodoException();
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
    public void put(E e) {
        putLast(e);
    }

    @Override
    public void put(Transaction tx, E e) {
        putLast(tx, e);
    }

    public void put(BetaTransaction tx, E e) {
        putLast(tx, e);
    }

    @Override
    public void atomicPut() throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public E take() {
        return takeFirst();
    }

    @Override
    public E take(Transaction tx) {
        return takeFirst(tx);
    }

    public E take(BetaTransaction tx) {
        return takeFirst(tx);
    }

    @Override
    public E atomicTake() throws InterruptedException {
        return atomicTakeFirst();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TodoException();
    }

    @Override
    public int remainingCapacity() {
        throw new TodoException();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        throw new TodoException();
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new TodoException();
    }


    // ============== addList ==================

    @Override
    public void addLast(final E e) {
        final Transaction tx = getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        addLast((BetaTransaction) tx, e);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return offerFirst((BetaTransaction) tx, e);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return offerLast((BetaTransaction) tx, e);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return removeFirst((BetaTransaction) tx);
    }

    @Override
    public E removeFirst(final Transaction tx) {
        return removeFirst((BetaTransaction) tx);
    }

    public E removeFirst(final BetaTransaction tx) {
        E item = pollFirst(tx);
        if (item == null) {
            throw new NoSuchElementException();
        }
        return item;
    }

    @Override
    public E atomicRemoveFirst() {
        E item = atomicPollFirst();
        if (item == null) {
            throw new NoSuchElementException();
        }
        return item;
    }

    // ============= removeLast ================

    @Override
    public E removeLast() {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return removeLast((BetaTransaction) tx);
    }

    @Override
    public E removeLast(final Transaction tx) {
        return removeLast((BetaTransaction) tx);
    }

    public E removeLast(final BetaTransaction tx) {
        E item = atomicPollLast();
        if (item == null) {
            throw new NoSuchElementException();
        }
        return item;
    }

    @Override
    public E atomicRemoveLast() {
        E item = atomicPollLast();
        if (item == null) {
            throw new NoSuchElementException();
        }
        return item;
    }

    // ============= pollFirst ================

    @Override
    public E pollFirst() {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return pollFirst((BetaTransaction) tx);
    }

    @Override
    public E pollFirst(final Transaction tx) {
        return pollFirst((BetaTransaction) tx);
    }

    public E pollFirst(final BetaTransaction tx) {
        Node<E> head = tx.openForRead(headRef, LOCKMODE_NONE).value;
        if (head == null) {
            return null;
        }

        throw new NullPointerException();
    }

    @Override
    public E atomicPollFirst() {
        throw new TodoException();
    }

    // ============= pollLast ================

    @Override
    public E pollLast() {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return pollLast((BetaTransaction) tx);
    }

    @Override
    public E pollLast(final Transaction tx) {
        return pollLast((BetaTransaction) tx);
    }

    public E pollLast(final BetaTransaction tx) {
        Node<E> tail = tx.openForRead(tailRef, LOCKMODE_NONE).value;
        if (tail == null) {
            return null;
        }

        throw new TodoException();
    }

    @Override
    public E atomicPollLast() {
        throw new TodoException();
    }

    // ============= getFirst ================

    @Override
    public E getFirst() {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return getFirst((BetaTransaction) tx);
    }

    @Override
    public E getFirst(final Transaction tx) {
        return getFirst((BetaTransaction) tx);
    }

    public E getFirst(final BetaTransaction tx) {
        E item = peekFirst(tx);
        if (item == null) {
            throw new NoSuchElementException();
        }
        return item;
    }

    @Override
    public E atomicGetFirst() {
        throw new TodoException();
    }

    // ============= getlast ================

    @Override
    public E getLast() {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return getLast((BetaTransaction) tx);
    }

    @Override
    public E getLast(final Transaction tx) {
        return getLast((BetaTransaction) tx);
    }

    public E getLast(final BetaTransaction tx) {
        final E item = peekLast(tx);
        if (item == null) {
            throw new NoSuchElementException();
        }
        return item;
    }

    @Override
    public E atomicGetLast() {
        throw new TodoException();
    }

    // ============= peekFirst ================

    @Override
    public E peekFirst() {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return peekFirst((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return peekLast((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return removeFirstOccurrence((BetaTransaction) tx, o);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return removeLastOccurrence((BetaTransaction) tx, o);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        push((BetaTransaction) tx, e);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return pop((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return descendingIterator((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return addAll((BetaTransaction) tx, index, c);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return get((BetaTransaction) tx, index);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return set((BetaTransaction) tx, index, element);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        add((BetaTransaction) tx, index, element);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return remove((BetaTransaction) tx, index);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return indexOf((BetaTransaction) tx, o);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return lastIndexOf((BetaTransaction) tx, o);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return listIterator((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return listIterator((BetaTransaction) tx, index);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return subList((BetaTransaction) tx, fromIndex, toIndex);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return offer((BetaTransaction) tx, e);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return remove((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return poll((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return poll((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return peek((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return size((BetaTransaction) tx);
    }

    @Override
    public int size(Transaction tx) {
        return size((BetaTransaction) tx);
    }

    public int size(BetaTransaction tx) {
        return sizeRef.get(tx);
    }

    @Override
    public int atomicSize() {
        return sizeRef.atomicGet();
    }

    // ============= isEmpty ================

    @Override
    public boolean isEmpty() {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return isEmpty((BetaTransaction) tx);
    }

    @Override
    public boolean isEmpty(final Transaction tx) {
        return isEmpty((BetaTransaction) tx);
    }

    public boolean isEmpty(final BetaTransaction tx) {
        return tx.openForRead(headRef, LOCKMODE_NONE).value == null;
    }

    @Override
    public boolean atomicIsEmpty() {
        return sizeRef.atomicGet() == 0;
    }

    // ============= contains ================

    @Override
    public boolean contains(final Object o) {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return contains((BetaTransaction) tx, o);
    }

    @Override
    public boolean contains(final Transaction tx, Object o) {
        return contains((BetaTransaction) tx, o);
    }

    public boolean contains(final BetaTransaction tx, Object o) {
        return indexOf(tx, o) > -1;
    }

    @Override
    public boolean atomicContains(Object o) {
        return atomicIndexOf(o) > -1;
    }

    // ============= iterator ================

    @Override
    public Iterator<E> iterator() {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return iterator((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return toArray((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return toArray((BetaTransaction) tx, a);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return add((BetaTransaction) tx, e);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return remove((BetaTransaction) tx, o);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return containsAll((BetaTransaction) tx, c);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return addAll((BetaTransaction) tx, c);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return removeAll((BetaTransaction) tx, c);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return retainAll((BetaTransaction) tx, c);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        clear((BetaTransaction) tx);
    }

    @Override
    public void clear(final Transaction tx) {
        clear((BetaTransaction) tx);
    }

    public void clear(final BetaTransaction tx) {
        tx.openForWrite(headRef, LOCKMODE_NONE).value = null;
        tx.openForWrite(tailRef, LOCKMODE_NONE).value = null;
        tx.openForWrite(sizeRef, LOCKMODE_NONE).value = 0;
    }

    @Override
    public void atomicClear() {
        throw new TodoException();
    }

    // ============= equals ================

    @Override
    public boolean equals(final Object o) {
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return equals((BetaTransaction) tx, o);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return hashCode((BetaTransaction) tx);
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
        final Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }
        return toString((BetaTransaction) tx);
    }

    @Override
    public String toString(final Transaction tx) {
        return toString((BetaTransaction) tx);
    }

    public String toString(final BetaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public String atomicToString() {
        throw new TodoException();
    }

    static class Node<E> {
        Ref<Node<E>> prev = newRef();
        Ref<Node<E>> next = newRef();
        Ref<E> value = newRef();
    }
}
