package org.multiverse.jta;

import org.multiverse.api.exceptions.TodoException;

import javax.transaction.*;
import javax.transaction.xa.XAResource;

public final class MultiverseJTATransaction implements Transaction {

    private final org.multiverse.api.Transaction transaction;

    public MultiverseJTATransaction(org.multiverse.api.Transaction transaction) {
        if(transaction == null){
            throw new NullPointerException();
        }
        this.transaction = transaction;
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        transaction.commit();
    }

    @Override
    public int getStatus() throws SystemException {
        switch (transaction.getStatus()) {
            case Unstarted:
                return Status.STATUS_UNKNOWN;
            case Active:
                return Status.STATUS_ACTIVE;
            case Committed:
                return Status.STATUS_COMMITTED;
            case Aborted:
                return Status.STATUS_ROLLEDBACK;
            case Prepared:
                return Status.STATUS_PREPARED;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {
        transaction.abort();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        transaction.setAbortOnly();
    }

    @Override
    public void registerSynchronization(Synchronization synchronization) throws RollbackException, IllegalStateException, SystemException {
        throw new TodoException();
    }

    @Override
    public boolean delistResource(XAResource xaResource, int i) throws IllegalStateException, SystemException {
        throw new TodoException();
    }

    @Override
    public boolean enlistResource(XAResource xaResource) throws RollbackException, IllegalStateException, SystemException {
        throw new TodoException();
    }    
}
