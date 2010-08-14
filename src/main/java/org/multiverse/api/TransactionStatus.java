package org.multiverse.api;

/**
 * @author Peter Veentjer
 */
public enum TransactionStatus {

    New(false), Aborted(false), Committed(false), Active(true) , Prepared(true);

    private final boolean alive;

    TransactionStatus(boolean alive){
        this.alive  = alive;
    }

    public boolean isAlive(){
        return alive;
    }
}
