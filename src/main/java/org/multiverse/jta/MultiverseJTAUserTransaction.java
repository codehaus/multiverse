package org.multiverse.jta;

import org.multiverse.api.exceptions.TodoException;

import javax.transaction.*;

/**
 * The Multiverse implementation of the JTA {@link UserTransaction} interface.
 *
 * @author Peter Veentjer.
 */
public class MultiverseJTAUserTransaction implements UserTransaction {

    @Override
    public void begin() throws NotSupportedException, SystemException {
        throw new TodoException();
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException {
        throw new TodoException();
    }

    @Override
    public void rollback() throws SystemException {
        throw new TodoException();
    }

    @Override
    public void setRollbackOnly() throws SystemException {
        throw new TodoException();
    }

    @Override
    public int getStatus() throws SystemException {
        throw new TodoException();
    }

    @Override
    public void setTransactionTimeout(int i) throws SystemException {
        throw new TodoException();
    }
}
