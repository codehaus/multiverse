package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.AbstractTransactionConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.profiling.ProfileRepository;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

/**
 * A readonly {@link org.multiverse.stms.alpha.transactions.AlphaTransaction} implementation that doesn't track reads.
 * <p/>
 * Unlike the {@link org.multiverse.stms.alpha.transactions.update.GrowingUpdateAlphaTransaction} a readonly transaction doesn't need track
 * any reads done. This has the advantage that a readonly transaction consumes a lot less resources (so no collection
 * needed to track all the reads) and commits are also a lot quicker (no dirtyness checking).
 * <p/>
 * A disadvantage of not tracking reads, is that the retry/orelse functionality is not available in reaodnly
 * transactions because the transaction has no clue which objects were loaded. So it also has no clue about the objects
 * to listen to on a retry.
 * <p/>
 * Although readonly transactions are isolated from update transactions from a correctness point of view, from a
 * practical point of view a readonly transaction could be obstructed by an update transaction:
 * <p/>
 * in the following scenario, the <u>second</u> load will fail with a {@code LoadTooOldVersionException}:
 * <p/>
 * <pre>
 * T1 (ro): |load_X-----------------load_X|
 * T2 (up):         |write_X|
 * </pre>
 * In the future a version history will be added for previous committed data. So the chance that a old version is not
 * available is going to decrease.
 *
 * @author Peter Veentjer.
 */
public class NonTrackingReadonlyAlphaTransaction
        extends AbstractReadonlyAlphaTransaction<NonTrackingReadonlyAlphaTransaction.Config> {

    public static class Config extends AbstractTransactionConfig {

        public final ProfileRepository profiler;

        public Config(
                Clock clock, RestartBackoffPolicy restartBackoffPolicy,
                String familyName,
                ProfileRepository profiler,
                int maxRetryCount) {
            super(clock, restartBackoffPolicy, familyName, true, maxRetryCount, false, false, false);
            this.profiler = profiler;
        }
    }

    public static class Factory implements TransactionFactory<AlphaTransaction> {

        private final Config config;

        public Factory(Config config) {
            this.config = config;
        }

        @Override
        public AlphaTransaction start() {
            return new NonTrackingReadonlyAlphaTransaction(config);
        }
    }

    public NonTrackingReadonlyAlphaTransaction(Config config) {
        super(config);
        init();
    }

    @Override
    protected AlphaTranlocal findAttached(AlphaTransactionalObject txObject) {
        return null;
    }

    @Override
    protected void attach(AlphaTranlocal tranlocal) {
        throw new UnsupportedOperationException();
    }
}
