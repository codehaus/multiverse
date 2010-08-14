package org.multiverse.actors;

import org.multiverse.api.exceptions.TodoException;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Take should only by done by 1 thread.
 * <p/>
 * The take will only return an empty
 * <p/>
 * ..if there is no room for putting, a thread will wait on switching of the pair.
 * ..the thread that does a put on the first item successfully, should also enable the
 * 'stuff available flag'. This needs to be done under a lock so the waiting thread will
 * be notified.
 * <p/>
 * When the putting thread can't place an item on the last, it enters the also a a synchronized block
 * and waits until the taking thread has cleared his queue and replaced it.
 * <p/>
 * The problem: it could be that a transaction placed an item on the array that already is switched and
 * you get out of order items.
 * <p/>
 * Advantage of current approach, placing an item only takes one volatile read (to get the array) and
 * one cas (if you are lucky) to place the item. Only on the end and at the beginning it gets more
 * expensive. Another advantage is that taking an item can be done without any synchronization.
 * <p/>
 * A placer will never do the switching.. only the taker.
 *
 * @author Peter Veentjer
 */
public class Mailbox<E> implements BlockingQueue<E> {
    private final int spinCount;

    private final Lock switchLock = new ReentrantLock();
    // private final AtomicReference<Node<E>> firstFree;
    //private final AtomicReference<Node<E>> firstFull;

    public Mailbox(int capacity, int spinCount) {
        this.spinCount = spinCount;
        AtomicReferenceArray<E> left = new AtomicReferenceArray<E>(capacity);
        AtomicReferenceArray<E> right = new AtomicReferenceArray<E>(capacity);
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }

        throw new TodoException();
    }

    @Override
    public E take() throws InterruptedException {
        while (true) {
            //  Node<E> node = firstFull.get();
            //  E value = node.value.get();
            //  if (value != null) {
            //
            //  } else {
            //
            //  }
        }
    }

    static class Node<E> {
        Node<E> next;
        Node<E> prev;
        AtomicReference<E> value = new AtomicReference<E>();
    }
    //====================== non implemented stuff ===========================

    @Override
    public boolean add(E e) {
        throw new TodoException();
    }

    @Override
    public boolean offer(E e) {
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
    public int remainingCapacity() {
        throw new TodoException();
    }

    @Override
    public boolean remove(Object o) {
        throw new TodoException();
    }

    @Override
    public boolean contains(Object o) {
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

    @Override
    public E remove() {
        throw new TodoException();
    }

    @Override
    public E poll() {
        throw new TodoException();
    }

    @Override
    public E element() {
        throw new TodoException();
    }

    @Override
    public E peek() {
        throw new TodoException();
    }

    @Override
    public int size() {
        throw new TodoException();
    }

    @Override
    public boolean isEmpty() {
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
    public boolean containsAll(Collection<?> c) {
        throw new TodoException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
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
    public void clear() {
        throw new TodoException();
    }
}
