package org.multiverse.stms.alpha.transactions;

import org.multiverse.api.LogLevel;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.AbstractTransaction;
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
public abstract class AbstractAlphaTransaction<C extends AbstractAlphaTransactionConfiguration, S extends AbstractTransactionSnapshot>
        extends AbstractTransaction<C, S> implements AlphaTransaction {

    public AbstractAlphaTransaction(C config) {
        super(config);
    }

    protected final AlphaTranlocal load(AlphaTransactionalObject txObject) {
        int spin = 0;
        while (true) {
            try {
                return txObject.___load(getReadVersion());
            } catch (LockNotFreeReadConflict lockNotFreeReadConflict) {
                if (spin >= config.maxReadSpinCount) {
                    throw lockNotFreeReadConflict;
                }

            }
            spin++;
        }
    }

    @Override
    public final AlphaTranlocal openForRead(AlphaTransactionalObject transactionalObject) {
        if (___LOGGING_ENABLED) {
            if (config.logLevel.isLogableFrom(LogLevel.fine)) {
                System.out.println(config.familyName + " openForRead " + toTxObjectString(transactionalObject));
            }
        }

        switch (getStatus()) {
            case New:
                if (transactionalObject == null) {
                    return null;
                }

                start();
                return doOpenForRead(transactionalObject);
            case Active:
                if (transactionalObject == null) {
                    return null;
                }

                return doOpenForRead(transactionalObject);
            case Prepared:
                String preparedMsg = format(
                        "Can't open for read transactional object '%s' " +
                                "because transaction '%s' is prepared to commit.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new PreparedTransactionException(preparedMsg);
            case Committed:
                String committedMsg = format(
                        "Can't open for read transactional object '%s' " +
                                "because transaction '%s' already is committed.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case Aborted:
                String abortedMsg = format(
                        "Can't open for read transactional object '%s' " +
                                "because transaction '%s' already is aborted.",
                        AlphaStmUtils.toTxObjectString(transactionalObject), config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new IllegalStateException("unhandled transactionStatus: " + getStatus());
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
    public final AlphaTranlocal openForWrite(AlphaTransactionalObject transactionalObject) {
        if (___LOGGING_ENABLED) {
            if (config.logLevel.isLogableFrom(LogLevel.fine)) {
                System.out.println(config.familyName + " openForWrite " + toTxObjectString(transactionalObject));
            }
        }

        switch (getStatus()) {
            case New:
                if (transactionalObject == null) {
                    String msg = format(
                            "Can't open for write a null transactional object on transaction '%s' ",
                            config.getFamilyName());
                    throw new NullPointerException(msg);
                }
                start();
                //fall through
            case Active:
                if (transactionalObject == null) {
                    String msg = format(
                            "Can't open for write a null transactional object on transaction '%s' ",
                            config.getFamilyName());
                    throw new NullPointerException(msg);
                }

                return doOpenForWrite(transactionalObject);
            case Prepared:
                String preparedMsg = format(
                        "Can't open for write transactional object '%s' "
                                + "because transaction '%s' already is prepared to commit.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new PreparedTransactionException(preparedMsg);
            case Committed:
                String committedMsg = format(
                        "Can't open for write transactional object '%s' "
                                + "because transaction '%s' already is committed.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case Aborted:
                String abortedMsg = format(
                        "Can't open for commuting write transactional object '%s' "
                                + "because transaction '%s' already is aborted.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new IllegalStateException("unhandled transactionStatus: " + getStatus());
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
    public final AlphaTranlocal openForConstruction(AlphaTransactionalObject transactionalObject) {
        if (___LOGGING_ENABLED) {
            if (config.logLevel.isLogableFrom(LogLevel.fine)) {
                System.out.println(config.familyName + " openForConstruction " + toTxObjectString(transactionalObject));
            }
        }

        switch (getStatus()) {
            case New:
                if (transactionalObject == null) {
                    String msg = format(
                            "Can't open for construction a null transactional object on transaction '%s' ",
                            config.getFamilyName());
                    throw new NullPointerException(msg);
                }
                start();
                //fall through
            case Active:
                if (transactionalObject == null) {
                    String msg = format(
                            "Can't open for construction a null transactional object on transaction '%s' ",
                            config.getFamilyName());
                    throw new NullPointerException(msg);
                }

                return doOpenForConstruction(transactionalObject);
            case Prepared:
                String preparedMsg = format(
                        "Can't open for construction transactional object '%s' "
                                + "because transaction '%s' already is prepared to commit.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new PreparedTransactionException(preparedMsg);
            case Committed:
                String committedMsg = format(
                        "Can't open for construction transactional object '%s' "
                                + "because transaction '%s' already is committed.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case Aborted:
                String abortedMsg = format(
                        "Can't open for construction transactional object '%s' "
                                + "because transaction '%s' already is aborted.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new IllegalStateException("unhandled transactionStatus: " + getStatus());
        }
    }

    protected AlphaTranlocal doOpenForConstruction(AlphaTransactionalObject txObject) {
        String msg = format(
                "Can't can't open for construction transactional object '%s' " +
                        "because transaction '%s' and class '%s' doesn't support this operation.",
                toTxObjectString(txObject), config.getFamilyName(), getClass());
        throw new UnsupportedOperationException(msg);
    }


    @Override
    public final AlphaTranlocal openForCommutingWrite(AlphaTransactionalObject transactionalObject) {
        if (___LOGGING_ENABLED) {
            if (config.logLevel.isLogableFrom(LogLevel.fine)) {
                System.out.println(config.familyName + " openForCommutingWrite " + toTxObjectString(transactionalObject));
            }
        }

        switch (getStatus()) {
            case New:
               //fall through
            case Active:
                if (transactionalObject == null) {
                    String msg = format(
                            "Can't open for write a null transactional object on transaction '%s' ",
                            config.getFamilyName());
                    throw new NullPointerException(msg);
                }

                return doOpenForCommutingWrite(transactionalObject);
            case Prepared:
                String preparedMsg = format(
                        "Can't open for write transactional object '%s' "
                                + "because transaction '%s' already is prepared to commit.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new PreparedTransactionException(preparedMsg);
            case Committed:
                String committedMsg = format(
                        "Can't open for write transactional object '%s' "
                                + "because transaction '%s' already is committed.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new DeadTransactionException(committedMsg);
            case Aborted:
                String abortedMsg = format(
                        "Can't open for commuting write transactional object '%s' "
                                + "because transaction '%s' already is aborted.",
                        toTxObjectString(transactionalObject), config.getFamilyName());
                throw new DeadTransactionException(abortedMsg);
            default:
                throw new IllegalStateException("unhandled transactionStatus: " + getStatus());
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
    protected final boolean doRegisterRetryLatch(Latch latch, long wakeupVersion) {
        if (!config.explicitRetryAllowed) {
            String msg = format("Transaction %s explicitly doesn't allow for a retry (needed for blocking operations)",
                    config.getFamilyName());
            throw new NoRetryPossibleException(msg);
        }

        SpeculativeConfiguration speculativeConfig = config.speculativeConfiguration;

        if (!config.readTrackingEnabled) {
            if (speculativeConfig.isSpeculativeNoReadTrackingEnabled()) {
                speculativeConfig.signalSpeculativeReadTrackingDisabledFailure();
                throw SpeculativeConfigurationFailure.create();
            }

            return false;
        }

        return dodoRegisterRetryLatch(latch, wakeupVersion);
    }

    protected abstract boolean dodoRegisterRetryLatch(Latch latch, long wakeupVersion);
}
