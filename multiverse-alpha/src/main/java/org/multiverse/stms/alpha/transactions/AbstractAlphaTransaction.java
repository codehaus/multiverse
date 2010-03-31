package org.multiverse.stms.alpha.transactions;

import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.AbstractTransaction;
import org.multiverse.stms.AbstractTransactionConfiguration;
import org.multiverse.stms.AbstractTransactionSnapshot;
import org.multiverse.stms.alpha.AlphaStmUtils;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.AlphaStmUtils.toTxObjectString;

/**
 * An abstract {@link AlphaTransaction} that provides some basic pluming logic.
 *
 * @author Peter Veentjer.
 * @param <C>
 * @param <S>
 */
public abstract class AbstractAlphaTransaction<C extends AbstractTransactionConfiguration, S extends AbstractTransactionSnapshot>
        extends AbstractTransaction<C, S> implements AlphaTransaction {

    public AbstractAlphaTransaction(C config) {
        super(config);
    }

    @Override
    public final AlphaTranlocal openForCommutingWrite(AlphaTransactionalObject txObject) {
         switch (getStatus()) {
            case active:
                if (txObject == null) {
                    String msg = format(
                            "Can't open for write a null transactional object on transaction '%s' ",
                            config.getFamilyName());
                    throw new NullPointerException(msg);
                }

                return doOpenForCommutingWrite(txObject);
            case prepared:
                String preparedMsg = format(
                        "Can't open for write transactional object '%s' "
                                + "because transaction '%s' already is prepared to commit.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new PreparedTransactionException(preparedMsg);
            case committed:
                String committedMsg = format(
                        "Can't open for write transactional object '%s' "
                                + "because transaction '%s' already is committed.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                String abortedMsg = format(
                        "Can't open for commuting write transactional object '%s' "
                                + "because transaction '%s' already is aborted.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new IllegalStateException();
        }
    }

    protected AlphaTranlocal doOpenForCommutingWrite(AlphaTransactionalObject txObject) {
        String msg = format(
                "Can't can't open for write transactional object '%s' " +
                        "because transaction '%s' and class '%s' doesn't support this operation.",
                toTxObjectString(txObject), config.getFamilyName(), getClass());
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public final AlphaTranlocal openForRead(AlphaTransactionalObject txObject) {
        switch (getStatus()) {
            case active:
                if (txObject == null) {
                    return null;
                }

                return doOpenForRead(txObject);
            case prepared:
                String preparedMsg = format(
                        "Can't open for read transactional object '%s' " +
                                "because transaction '%s' is prepared to commit.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new PreparedTransactionException(preparedMsg);
            case committed:
                String committedMsg = format(
                        "Can't open for read transactional object '%s' " +
                                "because transaction '%s' already is committed.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                String abortedMsg = format(
                        "Can't open for read transactional object '%s' " +
                                "because transaction '%s' already is aborted.",
                        AlphaStmUtils.toTxObjectString(txObject), config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new IllegalStateException();
        }
    }

    protected AlphaTranlocal doOpenForRead(AlphaTransactionalObject txObject) {
        String msg = format(
                "Can't open for read transactional object '%s' " +
                        "because transaction '%s' and class '%s' doesn't support this operation.",
                toTxObjectString(txObject), config.getFamilyName(), getClass());
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public final AlphaTranlocal openForWrite(AlphaTransactionalObject txObject) {
        switch (getStatus()) {
            case active:
                if (txObject == null) {
                    String msg = format(
                            "Can't open for write a null transactional object on transaction '%s' ",
                            config.getFamilyName());
                    throw new NullPointerException(msg);
                }

                return doOpenForWrite(txObject);
            case prepared:
                String preparedMsg = format(
                        "Can't open for write transactional object '%s' "
                                + "because transaction '%s' already is prepared to commit.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new PreparedTransactionException(preparedMsg);
            case committed:
                String committedMsg = format(
                        "Can't open for write transactional object '%s' "
                                + "because transaction '%s' already is committed.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                String abortedMsg = format(
                        "Can't open for commuting write transactional object '%s' "
                                + "because transaction '%s' already is aborted.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new IllegalStateException();
        }
    }

    protected AlphaTranlocal doOpenForWrite(AlphaTransactionalObject txObject) {
        String msg = format(
                "Can't can't open for write transactional object '%s' " +
                        "because transaction '%s' and class '%s' doesn't support this operation.",
                toTxObjectString(txObject), config.getFamilyName(), getClass());
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public final AlphaTranlocal openForConstruction(AlphaTransactionalObject txObject) {
        switch (getStatus()) {
            case active:
                if (txObject == null) {
                    String msg = format(
                            "Can't open for construction a null transactional object on transaction '%s' ",
                            config.getFamilyName());
                    throw new NullPointerException(msg);
                }

                return doOpenForConstruction(txObject);
            case prepared:
                String preparedMsg = format(
                        "Can't open for construction transactional object '%s' "
                                + "because transaction '%s' already is prepared to commit.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new PreparedTransactionException(preparedMsg);
            case committed:
                String committedMsg = format(
                        "Can't open for construction transactional object '%s' "
                                + "because transaction '%s' already is committed.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case aborted:
                String abortedMsg = format(
                        "Can't open for construction transactional object '%s' "
                                + "because transaction '%s' already is aborted.",
                        toTxObjectString(txObject), config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new IllegalStateException();
        }
    }

    protected AlphaTranlocal doOpenForConstruction(AlphaTransactionalObject txObject) {
        String msg = format(
                "Can't can't open for construction transactional object '%s' " +
                        "because transaction '%s' and class '%s' doesn't support this operation.",
                toTxObjectString(txObject), config.getFamilyName(), getClass());
        throw new UnsupportedOperationException(msg);
    }
}
