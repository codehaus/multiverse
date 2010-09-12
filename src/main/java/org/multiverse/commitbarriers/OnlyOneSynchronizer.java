package org.multiverse.commitbarriers;

import org.multiverse.api.Transaction;

/**
 * A Transaction synchronization structure where only one transaction is able to commit and the rest are
 * aborted. This structure can be used to have multiple transaction be run in a fork/join mode, but with the
 * big difference that only one transaction is allowed to commit, and the others will be aborted. 
 */
public class OnlyOneSynchronizer {

    private volatile boolean isFinished = false;

    public void register(Transaction tx){

    }

    public void arrive(Transaction tx){
        if(isFinished){
            tx.abort();
            throw new RuntimeException();
        }

        tx.prepare();
        tx.commit();
    }
}
