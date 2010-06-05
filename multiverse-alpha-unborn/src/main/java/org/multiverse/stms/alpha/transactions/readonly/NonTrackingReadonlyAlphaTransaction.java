package org.multiverse.stms.alpha.transactions.readonly;

import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

/**
 * A readonly {@link org.multiverse.stms.alpha.transactions.AlphaTransaction} implementation that doesn't track reads.
 * <p/>
 * Unlike the {@link org.multiverse.stms.alpha.transactions.update.MapUpdateAlphaTransaction} a readonly transaction doesn't need track
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
public class NonTrackingReadonlyAlphaTransaction extends AbstractReadonlyAlphaTransaction {

    public NonTrackingReadonlyAlphaTransaction(ReadonlyConfiguration config) {
        super(config);
    }

    @Override
    protected boolean dodoRegisterRetryLatch(Latch latch, long wakeupVersion) {
        return false;
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
