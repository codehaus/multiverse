package org.multiverse.stms.alpha.manualinstrumentation;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.api.Transaction;
import org.multiverse.templates.AtomicTemplate;

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
        new AtomicTemplate() {
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
        return new AtomicTemplate<Integer>() {
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

    public int size() {
        return new AtomicTemplate<Integer>(true) {
            @Override
            public Integer execute(Transaction t) {        
                return pushedStack.size() + readyToPopStack.size();
            }
        }.execute();        
    }

    public boolean isEmpty() {
        return new AtomicTemplate<Boolean>(true) {
            @Override
            public Boolean execute(Transaction t) {        
                return pushedStack.isEmpty() && readyToPopStack.isEmpty();
            }
        }.execute();          
    }
}
