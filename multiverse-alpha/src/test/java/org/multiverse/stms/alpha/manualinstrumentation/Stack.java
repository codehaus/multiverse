package org.multiverse.stms.alpha.manualinstrumentation;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.*;
import org.multiverse.stms.alpha.mixins.FastAtomicObjectMixin;
import org.multiverse.templates.AtomicTemplate;

public final class Stack<E> extends FastAtomicObjectMixin {

    public Stack() {
        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) {
                StackTranlocal<E> tranlocal = (StackTranlocal<E>) ((AlphaTransaction) t).load(Stack.this);
                return null;
            }
        }.execute();
    }

    public int size() {
        return new AtomicTemplate<Integer>(true) {
            @Override
            public Integer execute(Transaction t) {
                StackTranlocal tranlocal = (StackTranlocal) ((AlphaTransaction) t).load(Stack.this);
                return size(tranlocal);
            }
        }.execute();
    }

    public boolean isEmpty() {
        return new AtomicTemplate<Boolean>(true) {
            @Override
            public Boolean execute(Transaction t) {
                StackTranlocal tranlocal = (StackTranlocal) ((AlphaTransaction) t).load(Stack.this);
                return isEmpty(tranlocal);
            }
        }.execute();
    }

    public void push(final E item) {
        new AtomicTemplate() {
            @Override
            public Integer execute(Transaction t) {
                StackTranlocal tranlocal = (StackTranlocal) ((AlphaTransaction) t).load(Stack.this);
                push(tranlocal, item);
                return null;
            }
        }.execute();
    }

    public E pop() {
        return new AtomicTemplate<E>() {
            @Override
            public E execute(Transaction t) {
                StackTranlocal<E> tranlocal = (StackTranlocal) ((AlphaTransaction) t).load(Stack.this);
                return pop(tranlocal);
            }
        }.execute();
    }

    public void clear() {
        new AtomicTemplate() {
            @Override
            public Integer execute(Transaction t) {
                StackTranlocal tranlocal = (StackTranlocal) ((AlphaTransaction) t).load(Stack.this);
                clear(tranlocal);
                return null;
            }
        }.execute();
    }

    @Override
    public AlphaTranlocal ___loadUpdatable(long version) {
        StackTranlocal<E> origin = (StackTranlocal<E>) ___load(version);
        if (origin == null) {
            return new StackTranlocal<E>(this);
        } else {
            return new StackTranlocal<E>(origin);
        }
    }

    public void clear(StackTranlocal<E> tranlocal) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }

        tranlocal.size = 0;
        tranlocal.head = null;
    }

    public void push(StackTranlocal<E> tranlocal, E item) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }

        if (item == null) {
            throw new NullPointerException();
        }

        tranlocal.head = new Node<E>(tranlocal.head, item);
        tranlocal.size++;
    }

    public E pop(StackTranlocal<E> tranlocal) {
        if (tranlocal.___writeVersion > 0) {
            throw new ReadonlyException();
        }

        if (tranlocal.size == 0) {
            retry();
        }

        tranlocal.size--;
        Node<E> oldHead = tranlocal.head;
        tranlocal.head = oldHead.next;
        return oldHead.value;
    }

    public boolean isEmpty(StackTranlocal<E> tranlocal) {
        return tranlocal.size == 0;
    }

    public int size(StackTranlocal<E> tranlocal) {
        return tranlocal.size;
    }

    public static class Node<E> {

        final Node<E> next;
        final E value;

        Node(Node<E> next, E value) {
            this.next = next;
            this.value = value;
        }
    }

    public static final class StackTranlocal<E> extends AlphaTranlocal {

        private final Stack<E> ___atomicObject;
        private StackTranlocal<E> ___origin;
        int size;
        Node<E> head;

        /**
         * Makes an initial version.
         *
         * @param atomicObject
         */
        StackTranlocal(Stack<E> atomicObject) {
            this.___atomicObject = atomicObject;
        }

        /**
         * Makes a clone.
         *
         * @param origin
         */
        StackTranlocal(StackTranlocal<E> origin) {
            this.___origin = origin;
            this.___atomicObject = origin.___atomicObject;
            this.size = origin.size;
            this.head = origin.head;
        }

        @Override
        public AlphaAtomicObject getAtomicObject() {
            return ___atomicObject;
        }

        @Override
        public void prepareForCommit(long writeVersion) {
            this.___writeVersion = writeVersion;
            this.___origin = null;
        }

        @Override
        public AlphaTranlocalSnapshot takeSnapshot() {
            throw new RuntimeException();
        }

        @Override
        public DirtinessStatus getDirtinessStatus() {
            if (___writeVersion > 0) {
                return DirtinessStatus.readonly;
            } else if (___origin == null) {
                return DirtinessStatus.fresh;
            } else if (___origin.size != this.size) {
                return DirtinessStatus.dirty;
            } else if (___origin.head != this.head) {
                return DirtinessStatus.dirty;
            } else {
                return DirtinessStatus.clean;
            }
        }
    }
}

