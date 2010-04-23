package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.templates.TransactionTemplate;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;

public final class IntQueue {

    private final IntStack pushedStack;
    private final IntStack readyToPopStack;
    private final int maxCapacity;

    public IntQueue() {
        this(Integer.MAX_VALUE);
    }

    public IntQueue(int maxCapacity) {
        if (maxCapacity < 0) {
            throw new IllegalArgumentException();
        }
        pushedStack = new IntStack();
        readyToPopStack = new IntStack();
        this.maxCapacity = maxCapacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void push(final int item) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) {
                if (size() >= maxCapacity) {
                    retry();
                }

                pushedStack.push(item);
                return null;
            }
        }.execute();
    }

    public int pop() {
        return new TransactionTemplate<Integer>() {
            @Override
            public Integer execute(Transaction t) {
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
            public Integer execute(Transaction t) {
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
            public Boolean execute(Transaction t) {
                return pushedStack.isEmpty() && readyToPopStack.isEmpty();
            }
        }.execute();
    }
}
