package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.predicates.LongPredicate;

/**
 * @author Peter Veentjer
 */
public class LongRefAwaitThread extends TestThread {
    private final BetaLongRef ref;
    private final LongPredicate predicate;

    public LongRefAwaitThread(BetaLongRef ref, final long awaitValue) {
        this(ref, new LongPredicate() {
            @Override
            public boolean evaluate(long current) {
                return current == awaitValue;
            }
        });
    }

    public LongRefAwaitThread(BetaLongRef ref, LongPredicate predicate) {
        this.ref = ref;
        this.predicate = predicate;
    }

    @Override
    public void doRun() throws Exception {
        ref.getStm().getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                System.out.println("Starting wait and ref.value found: " + ref.get());
                ref.await(predicate);
                System.out.println("Finished wait and ref.value found: " + ref.get());
            }
        });
    }
}
