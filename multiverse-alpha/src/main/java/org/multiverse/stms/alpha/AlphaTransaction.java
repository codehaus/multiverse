package org.multiverse.stms.alpha;

import org.multiverse.api.Transaction;

/**
 * A {@link Transaction} interface tailored for the Alpha STM.
 * <p/>
 * Since a AlphaTransaction is a {@link Transaction}, it isn't thread-safe.
 *
 * @author Peter Veentjer.
 */
public interface AlphaTransaction extends Transaction {

    /**
     * Loads the Tranlocal for the specified atomicObject. It depends on the transaction if it can be used for updates
     * or not. But because each tranlocal is protected against updates after it has been committed, nothing can go wrong
     * if a committed version is 'abused'. Of course a Tranlocal.committed check needs to be added (instrumentation) in
     * the code.
     * <p/>
     * If atomicObject is null, the return value is null.
     * <p/>
     * It doesn't matter if the atomicObject has never been committed before. When an atomicObject is created, the
     * constructor also needs to do a load.
     *
     * @param atomicObject the atomicObject to get the tranlocal for.
     * @return the loaded Tranlocal. If atomicObject is null, the returned value will be null. Otherwise a tranlocal is
     *         returned.
     *
     * @throws org.multiverse.api.exceptions.LoadException
     *          if something goes wrong while loading.
     * @throws org.multiverse.api.exceptions.DeadTransactionException
     *          if this transaction isn't active.
     */
    AlphaTranlocal load(AlphaAtomicObject atomicObject);
}
