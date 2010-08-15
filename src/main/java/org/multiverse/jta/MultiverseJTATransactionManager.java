package org.multiverse.jta;

import org.multiverse.api.exceptions.ControlFlowError;
import org.multiverse.api.exceptions.TodoException;

import javax.transaction.*;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * The Multiverse implementation of the JTA {@link javax.transaction.TransactionManager} interface.
 *
 * @author Peter Veentjer.
 */
public class MultiverseJTATransactionManager implements TransactionManager {

    @Override
    public void begin() throws NotSupportedException, SystemException {
        throw new TodoException();
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
        org.multiverse.api.Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            throw new IllegalStateException();
        }

        try {
            tx.commit();
        } catch (ControlFlowError error) {
            throw new RollbackException(error.getMessage());
        }
    }

    @Override
    public int getStatus() throws SystemException {
        org.multiverse.api.Transaction tx = getThreadLocalTransaction();
        if (tx == null) {
            return Status.STATUS_NO_TRANSACTION;
        }

        switch (tx.getStatus()) {
            case Unstarted:
                return Status.STATUS_UNKNOWN;
            case Active:
                return Status.STATUS_ACTIVE;
            case Prepared:
                return Status.STATUS_PREPARED;
            case Aborted:
                return Status.STATUS_ROLLEDBACK;
            case Committed:
                return Status.STATUS_COMMITTED;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        org.multiverse.api.Transaction tx = getThreadLocalTransaction();

        if (tx == null) {

        }

        return new MultiverseJTATransaction(tx);
    }

    @Override
    public void resume(Transaction transaction) throws InvalidTransactionException, SystemException {
        throw new TodoException();
    }

    @Override
    public void rollback() throws  SecurityException, SystemException {
        org.multiverse.api.Transaction tx = getThreadLocalTransaction();

        if (tx == null) {
            throw new IllegalStateException("No Transaction found");
        }

        tx.abort();
    }

    @Override
    public void setRollbackOnly() throws SystemException {
        org.multiverse.api.Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new IllegalStateException("No transaction found");
        }

        tx.setAbortOnly();
    }

    @Override
    public void setTransactionTimeout(int i) throws SystemException {
        throw new TodoException();
    }

    @Override
    public Transaction suspend() throws SystemException {
        throw new TodoException();
    }
}
