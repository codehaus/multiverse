package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.AbstractTransactionSnapshot;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AbstractAlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.AlphaStmUtils.toTxObjectString;

public abstract class AbstractReadonlyAlphaTransaction
        extends AbstractAlphaTransaction<ReadonlyConfiguration, AbstractTransactionSnapshot> {

    public AbstractReadonlyAlphaTransaction(ReadonlyConfiguration config) {
        super(config);
    }

    protected abstract AlphaTranlocal findAttached(AlphaTransactionalObject txObject);

    protected abstract void attach(AlphaTranlocal tranlocal);

    @Override
    protected final AlphaTranlocal doOpenForRead(AlphaTransactionalObject transactionalObject) {
        AlphaTranlocal tranlocal = findAttached(transactionalObject);
        if (tranlocal == null) {
            tranlocal = load(transactionalObject);

            if (tranlocal == null) {
                throw createLoadUncommittedException(transactionalObject);
            }

            if (config.readTrackingEnabled) {
                attach(tranlocal);
            }
        }

        return tranlocal;
    }

    protected UncommittedReadConflict createLoadUncommittedException(AlphaTransactionalObject txObject) {
        String msg = format(
                "Can't open for read transactional object '%s' in transaction '%s' because the " +
                        "readonly transactional object has not been committed before. The cause of this " +
                        "problem is very likely that a reference to this transactional object escaped " +
                        "the creating transaction before that transaction was committed.'",
                toTxObjectString(txObject), config.getFamilyName());
        return new UncommittedReadConflict(msg);
    }

    @Override
    public AlphaTranlocal doOpenForCommutingWrite(AlphaTransactionalObject txObject) {
        //forward it to the write
        return doOpenForWrite(txObject);
    }

    @Override
    public AlphaTranlocal doOpenForConstruction(AlphaTransactionalObject txObject) {
        //forward it to the write
        return doOpenForWrite(txObject);
    }

    @Override
    protected final AlphaTranlocal doOpenForWrite(AlphaTransactionalObject txObject) {
        SpeculativeConfiguration speculativeConfig = config.speculativeConfiguration;
        if (speculativeConfig.isSpeculativeReadonlyEnabled()) {
            speculativeConfig.signalSpeculativeReadonlyFailure();
            throw SpeculativeConfigurationFailure.create();
        }

        String msg = format(
                "Can't open for write transactional object '%s' because transaction '%s' is readonly'",
                toTxObjectString(txObject), config.getFamilyName());
        throw new ReadonlyException(msg);
    }
}
