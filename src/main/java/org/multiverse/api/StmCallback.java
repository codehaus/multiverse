package org.multiverse.api;

/**
 * The StmCallback provides the opportunity for external systems to hook in closely with the Multiverse STM.
 * All transaction starts/aborts/commits are reported back to this interface and additional logic can be
 * placed after a transaction is committed, aborted and started.
 */
public interface StmCallback {

    /**
     * This method is called just after a transaction is started.
     *
     * @param tx the transaction that is started.
     */
    void afterStart(Transaction tx);

    /**
     * This method is called just after a transaction aborted.
     *
     * @param tx the transaction that is aborted.
     */
    void afterAbort(Transaction tx);

    /**
     * This method is called just after a transaction is committed.
     *
     * @param tx the transaction that is committed.
     */
    void afterCommit(Transaction tx);
}
