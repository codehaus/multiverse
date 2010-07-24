package org.multiverse.stms.beta;

import org.multiverse.api.BackoffPolicy;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;
import org.multiverse.stms.beta.transactions.MonoBetaTransaction;

/**
 * @author Peter Veentjer
 */
public abstract class TransactionTemplate {

    public long controlFlowErrorCount = 0;
    public long readConflictCount = 0;
    public long writeConflictCount = 0;

    private final BetaStm stm;
    private final BetaTransaction tx;
    private final BackoffPolicy backoffPolicy = BackoffPolicy.INSTANCE_100_MS_MAX;

    public TransactionTemplate(BetaStm stm) {
        this(stm, new MonoBetaTransaction(new BetaTransactionConfig(stm)));
    }

    public TransactionTemplate(BetaStm stm, BetaTransaction tx) {
        this.stm = stm;
        this.tx = tx;
    }

    public abstract void execute(BetaTransaction tx);

    public final void execute(ObjectPool pool) {
        tx.resetAttempt();
                 
        do {
            tx.reset(pool);
            boolean abort = true;
            try {
                try {
                    execute(tx);
                    tx.commit(pool);
                    abort = false;
                    return;
                } catch (ControlFlowError e) {
                    controlFlowErrorCount++;
                    if (e instanceof RetryError) {
                        CheapLatch latch = pool.takeCheapLatch();
                        if (latch == null) {
                            latch = new CheapLatch();
                        }
                        //todo: essentially this reset is not needed if fresh created latch.
                        long lockEra = latch.reset();
                        tx.registerChangeListenerAndAbort(latch, pool);
                        latch.await(lockEra);
                        pool.putCheapLatch(latch);
                    } else {
                        if (e instanceof ReadConflict) {
                            readConflictCount++;
                        }
                        if (e instanceof WriteConflict) {
                            writeConflictCount++;
                        }
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    }
                    //there is another problem..
                    abort = false;
                }
            }catch(Exception e){
                e.printStackTrace();   
            }finally {
                if (abort) {
                    if(tx.getStatus() == TransactionStatus.Committed){
                        System.out.println("committed");
                    }
                    tx.abort(pool);
                }
            }
            tx.incAttempt();
        } while (tx.getAttempt() <= tx.getConfig().getMaxRetries());

        throw new TooManyRetriesException();
    }
}
