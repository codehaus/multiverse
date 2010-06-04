package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
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
                throw createUncommittedException(transactionalObject);
            }

            if (config.readTrackingEnabled) {
                attach(tranlocal);
            }
        }

        return tranlocal;
    }

    @Override
    public AlphaTranlocal doOpenForCommutingWrite(AlphaTransactionalObject transactionalObject) {
        //forward it to the write
        return doOpenForWrite(transactionalObject);
    }

    @Override
    public AlphaTranlocal doOpenForConstruction(AlphaTransactionalObject transactionalObject) {
        //forward it to the write
        return doOpenForWrite(transactionalObject);
    }

    @Override
    protected final AlphaTranlocal doOpenForWrite(AlphaTransactionalObject transactionalObject) {
        SpeculativeConfiguration speculativeConfig = config.speculativeConfiguration;
        if (speculativeConfig.isSpeculativeReadonlyEnabled()) {
            speculativeConfig.signalSpeculativeReadonlyFailure();
            throw SpeculativeConfigurationFailure.create();
        }

        String msg = format(
                "Can't open for write transactional object '%s' because transaction '%s' is readonly'",
                toTxObjectString(transactionalObject), config.getFamilyName());
        throw new ReadonlyException(msg);
    }
}
