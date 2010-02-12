package org.multiverse.stms.beta;

import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.utils.backoff.ExponentialBackoffPolicy;
import org.multiverse.utils.clock.PrimitiveClock;
import org.multiverse.utils.clock.StrictPrimitiveClock;

public class BetaStm implements Stm {

    private final PrimitiveClock clock = new StrictPrimitiveClock();

    @Override
    public long getVersion() {
        return clock.getVersion();
    }

    @Override
    public TransactionFactoryBuilder getTransactionFactoryBuilder() {
        return new TransactionFactoryBuilder() {
            @Override
            public TransactionFactoryBuilder setFamilyName(String familyName) {
                return this;
            }

            @Override
            public TransactionFactoryBuilder setReadonly(boolean readonly) {
                return this;
            }

            @Override
            public TransactionFactoryBuilder setMaxRetryCount(int retryCount) {
                return this;
            }

            @Override
            public TransactionFactoryBuilder setAutomaticReadTracking(boolean automaticReadTracking) {
                return this;
            }

            @Override
            public TransactionFactoryBuilder setInterruptible(boolean interruptible) {
                return this;
            }

            @Override
            public TransactionFactoryBuilder setSmartTxLengthSelector(boolean smartTxlengthSelector) {
                return this;
            }

            @Override
            public TransactionFactoryBuilder setPreventWriteSkew(boolean preventWriteSkew) {
                return this;
            }

            @Override
            public TransactionFactory build() {
                return new TransactionFactoryImpl();
            }
        };
    }

    class TransactionFactoryImpl implements TransactionFactory {
        @Override
        public Transaction start() {
            AbstractTransactionConfig config = new AbstractTransactionConfig(
                    clock,
                    ExponentialBackoffPolicy.INSTANCE_10_MS_MAX,
                    null, false, 1000, false, false, true) {
            };
            return new UpdateBetaTransaction(config);
        }
    }

}
