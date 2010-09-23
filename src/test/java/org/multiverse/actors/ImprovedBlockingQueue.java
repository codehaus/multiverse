package org.multiverse.actors;

import org.multiverse.api.AtomicBlock;
import org.multiverse.api.GlobalStmInstance;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactionalobjects.IntRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;

public class ImprovedBlockingQueue<E> implements BetaStmConstants {

    private BetaRef<E>[] items;
    private BetaIntRef headIndex = new BetaIntRef((BetaStm) getGlobalStmInstance());
    private BetaIntRef tailIndex = new BetaIntRef((BetaStm) getGlobalStmInstance());

    public ImprovedBlockingQueue(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException();
        }
        items = new BetaRef[maxCapacity];
        for (int k = 0; k < maxCapacity; k++) {
            items[k] = new BetaRef((BetaStm) getGlobalStmInstance());
        }
    }

    private final AtomicBlock putBlock = GlobalStmInstance.getGlobalStmInstance()
            .createTransactionFactoryBuilder()
            .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites)
            .setDirtyCheckEnabled(false)
            .buildAtomicBlock();

    public void put(final E item) {
        putBlock.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                int head = headIndex.get(tx);
                int tail = tailIndex.get(tx);

                if (head == tail - 1) {
                    retry();
                }

                items[head].set(tx, item);
                head++;

                if (head == items.length) {
                    head = 0;
                }

                headIndex.set(tx, head);
            }
        });
    }

    public void put(AtomicVoidClosure putClosure) {
        putBlock.execute(putClosure);
    }

    public PutClosure createPutClosure() {
        return new PutClosure();
    }

    public class PutClosure implements AtomicVoidClosure {
        public E item;

        @Override
        public void execute(Transaction tx) throws Exception {
            BetaTransaction btx = (BetaTransaction) tx;
            IntRefTranlocal head = btx.openForWrite(headIndex, LOCKMODE_NONE);
            IntRefTranlocal tail = btx.openForRead(tailIndex, LOCKMODE_NONE);


            if (head.value == tail.value - 1) {
                retry();
            }

            items[head.value].set(btx, item);
            head.value++;

            if (head.value == items.length) {
                head.value = 0;
            }
        }
    }

    private final AtomicBlock takeBlock = GlobalStmInstance.getGlobalStmInstance()
            .createTransactionFactoryBuilder()
            .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites)
            .setDirtyCheckEnabled(false)
            .buildAtomicBlock();

    private final AtomicClosure<E> takeClosure = new AtomicClosure<E>() {
        @Override
        public E execute(Transaction tx) throws Exception {
            BetaTransaction btx = (BetaTransaction) tx;
            IntRefTranlocal head = btx.openForRead(headIndex, LOCKMODE_NONE);
            IntRefTranlocal tail = btx.openForWrite(tailIndex, LOCKMODE_NONE);

            if (head.value == tail.value) {
                retry();
            }

            E item = items[tail.value].getAndSet(null);
            tail.value++;

            if (tail.value == items.length) {
                tail.value = 0;
            }

            return item;
        }
    };

    public E take() throws InterruptedException {
        return takeBlock.execute(takeClosure);
    }
}
