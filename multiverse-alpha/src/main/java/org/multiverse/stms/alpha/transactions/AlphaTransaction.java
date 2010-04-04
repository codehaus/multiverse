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
     * If transactional object is null, the return value is null.
     * <p/>
     * If a openForRead is done on a transactional object that never has been committed
     * before, a UncommittedReadConflict is thrown.
     * <p/>
     * If the transactional object already is opened for write, that version is returned.
     * <p/>
     * If the transactional object already was opened for a commuting operation, it is fixated
     * and returned and now can be used for direct writing purposes.
     * <p/>
     * If the transactional object already was opened for construction, that tranlocal is returned.
     *
     * @param transactionalObject the transactional object to getClassMetadata the tranlocal for.
     * @return the opened tranlocal.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     * @throws IllegalThreadStateException if the transaction isn't active.
     */
    AlphaTranlocal openForRead(AlphaTransactionalObject transactionalObject);

    /**
     * Opens the transactional object for writing purposes.
     * <p/>
     * It depends on the transaction if this operations is supported.
     * <p/>
     * If a openForWrite is done on a transactional object that never has been committed
     * before, a UncommittedReadConflict is thrown.
     * <p/>
     * If the transactional object already was opened for read, it is now upgraded to an
     * open for write (if it was tracked).
     * <p/>
     * If the transactional object was opened for write, it will be fixated that tranlocal
     * can now be used for writing purposes.
     * <p/>
     * If the transactional object already was opened for construction, that tranlocal
     * is returned.
     *
     * @param transactionalObject the transactional object to getClassMetadata the tranlocal for.
     * @return the opened tranlocal.
     * @throws NullPointerException        if transactional object is null.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     * @throws IllegalThreadStateException if the transaction isn't active.
     */
    AlphaTranlocal openForWrite(AlphaTransactionalObject transactionalObject);

    /**
     * Opens the transactional object for a commuting operation.
     * <p/>
     * It depends on the transaction if this operations is supported.
     * <p/>
     * If the transactional object has never been committed before, a UncommittedReadConflict
     * is thrown.
     * If the transactional object has been opened for read and is tracked, it will
     * be upgraded to an opened for write.
     * <p/>
     * If the transactional object already was opened for write, that tranlocal
     * is returned and no commuting operations are possible for that tranlocal.
     * <p/>
     * If the transactional object already was opened for construction, that tranlocal
     * is returned.
     *
     * @param transactionalObject the transactional object to open
     * @return the opened tranlocal.
     * @throws NullPointerException        if transactional object is null.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     * @throws IllegalThreadStateException if the transaction isn't active.
     */
    AlphaTranlocal openForCommutingWrite(AlphaTransactionalObject transactionalObject);

    /**
     * Opens the transactional object for construction (and writing) purposes.
     * <p/>
     * It depends on the transaction if this operation is supported.
     * <p/>
     * It is extremely important that this call is done only once for a
     * transactional objects (when it is constructed). There is no protection against
     * doing this multiple times, and when this happens it could overwrite already
     * committed changes when the transaction commits.
     * <p/>
     * Normally this call only is made by instrumented code or by Multiverse provided
     * transactional structure.
     *
     * @param transactionalObject the transactional object to open for construction
     * @return the opened tranlocal.
     * @throws NullPointerException        if transactional object is null.
     * @throws org.multiverse.api.exceptions.ControlFlowError
     *
     * @throws IllegalThreadStateException if the transaction isn't active.
     */
    AlphaTranlocal openForConstruction(AlphaTransactionalObject transactionalObject);
}
