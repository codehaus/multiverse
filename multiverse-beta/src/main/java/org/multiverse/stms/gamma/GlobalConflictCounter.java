package org.multiverse.stms.gamma;

import org.multiverse.stms.gamma.transactionalobjects.GammaObject;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The GlobalConflictCounter is used as a mechanism for guaranteeing read consistency. Depending on the configuration of the
 * transaction, if a transaction does a read, it also makes the read semi visible (only the number of readers are interesting
 * and not the actual transaction). If a updating transaction sees that there are readers, it increased the GlobalConflictCounter
 * and forces all reading transactions to do a conflict scan once they read transactional objects they have not read before.
 *
 * This mechanism is based on the SkySTM. The advantage of this approach compared to the TL2 approach is that the GlobalConflictCounter
 * is only increased on conflict and not on every update.
 *
 * Small transactions don't make use of this mechanism and do a full conflict scan every time. The advantage is that the pressure
 * on the GlobalConflictCounter is reduced and that expensive arrives/departs (require a cas) are reduced as well.
 *
 * @author Peter Veentjer.
 */
public final class GlobalConflictCounter {

    private final AtomicLong counter = new AtomicLong(Long.MIN_VALUE);

    public void signalConflict(GammaObject object) {
        long count = counter.get();
        counter.compareAndSet(count, count + 1);
    }

    public long count() {
        return counter.get();
    }
}
