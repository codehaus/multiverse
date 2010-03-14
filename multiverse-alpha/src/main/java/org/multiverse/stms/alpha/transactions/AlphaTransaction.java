package org.multiverse.stms.alpha.transactions;

import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

/**
 * A {@link org.multiverse.api.Transaction} interface tailored for the Alpha STM.
 * <p/>
 * Since a AlphaTransaction is a {@link org.multiverse.api.Transaction}, it isn't thread-safe.
 *
 * @author Peter Veentjer.
 */
public interface AlphaTransaction extends Transaction {

    /**
     * Opens the transactional object for reading purposes. It is not allowed to be used for writing purposes, because
     * it could be used by other transactions. If the transactional object already is opened for writing, that
     * tranlocal is returned.
     * <p/>
     * If txObject is null, the return value is null.
     * <p/>
     * It doesn't matter if the transactionalObject has never been committed before. When an transactionalObject is
     * created, the constructor also needs to do a openForWrite.
     *
     * @param txObject the transactional object to getClassMetadata the tranlocal for.
     * @return the opened tranlocal.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *          if something goes wrong while opening the txObject.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if this transaction isn't active.
     */
    AlphaTranlocal openForRead(AlphaTransactionalObject txObject);

    /**
     * Opens the txObject for writing purposes. It depends on the transaction if this operations is
     * supported.
     * <p/>
     * It doesn't matter if the transactional object has never been committed before. When an transactional object
     * is created, the constructor also needs to do a openForWrite.
     *
     * @param txObject the transactional object to getClassMetadata the tranlocal for.
     * @return the opened tranlocal.
     * @throws NullPointerException if txObject is null. One can't write on a 'null' transactional object, that
     *                              would normally also cause a NullPointerException.
     * @throws org.multiverse.api.exceptions.ReadConflict
     *                              if something goes wrong while opening the txObject for writing.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *                              if this transaction isn't active.
     */
    AlphaTranlocal openForWrite(AlphaTransactionalObject txObject);
}
