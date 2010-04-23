package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.templates.TransactionTemplate;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class Queue<E> {

    private final Stack<E> pushedStack;
    private final Stack<E> readyToPopStack;
    private final int maxCapacity;

    public Queue(int maxCapacity) {
        if (maxCapacity < 0) {
            throw new IllegalArgumentException();
        }
        pushedStack = new Stack<E>();
        readyToPopStack = new Stack<E>();
        this.maxCapacity = maxCapacity;
    }

    public Queue() {
        this(Integer.MAX_VALUE);
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void push(final E item) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                if (size() > maxCapacity) {
                    throw new IllegalStateException();
                }

                pushedStack.push(item);
                return null;
            }
        }.execute();
    }

    public E take() {
        return new TransactionTemplate<E>() {
            @Override
            public E execute(Transaction t) throws Exception {
                if (readyToPopStack.isEmpty()) {

                    while (!pushedStack.isEmpty()) {
                        readyToPopStack.push(pushedStack.pop());
                    }
                }

                return readyToPopStack.pop();
            }
        }.execute();
    }

    private final static TransactionFactory sizeTxFactory = getGlobalStmInstance()
            .getTransactionFactoryBuilder()
            .setReadonly(true)
            .setReadTrackingEnabled(false).build();


    public int size() {
        return new TransactionTemplate<Integer>(sizeTxFactory) {
            @Override
            public Integer execute(Transaction t) throws Exception {
                return pushedStack.size() + readyToPopStack.size();
            }
        }.execute();
    }

    private final static TransactionFactory isEmptyTxFactory = getGlobalStmInstance()
            .getTransactionFactoryBuilder()
            .setReadonly(true)
            .setReadTrackingEnabled(false).build();

    public boolean isEmpty() {
        return new TransactionTemplate<Boolean>(isEmptyTxFactory) {
            @Override
            public Boolean execute(Transaction t) throws Exception {
                return pushedStack.isEmpty() && readyToPopStack.isEmpty();
            }
        }.execute();
    }
}
