package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.Latch;
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
        extends AbstractAlphaTransaction<ReadonlyAlphaTransactionConfiguration, AbstractTransactionSnapshot> {

    public AbstractReadonlyAlphaTransaction(ReadonlyAlphaTransactionConfiguration config) {
        super(config);
    }

    protected abstract AlphaTranlocal findAttached(AlphaTransactionalObject txObject);

    protected abstract void attach(AlphaTranlocal tranlocal);

    @Override
    protected final AlphaTranlocal doOpenForRead(AlphaTransactionalObject txObject) {
        AlphaTranlocal tranlocal = findAttached(txObject);
        if (tranlocal == null) {
            tranlocal = txObject.___load(getReadVersion());

            if (tranlocal == null) {
                throw createLoadUncommittedException(txObject);
            }

            if (config.automaticReadTracking) {
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
    protected boolean doRegisterRetryLatch(Latch latch, long wakeupVersion) {
        SpeculativeConfiguration speculativeConfig = config.speculativeConfig;
        if (speculativeConfig.isSpeculativeNonAutomaticReadTrackingEnabled()) {
            speculativeConfig.signalSpeculativeNonAutomaticReadtrackingFailure();
            throw SpeculativeConfigurationFailure.create();
        }

        return super.doRegisterRetryLatch(latch, wakeupVersion);
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
        SpeculativeConfiguration speculativeConfig = config.speculativeConfig;
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
