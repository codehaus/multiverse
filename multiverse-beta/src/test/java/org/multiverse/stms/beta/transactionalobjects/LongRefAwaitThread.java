package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.predicates.LongPredicate;

import static org.multiverse.api.StmUtils.execute;

/**
 * @author Peter Veentjer
 */
public class LongRefAwaitThread extends TestThread {
    private final BetaLongRef ref;
    private final LongPredicate predicate;

    public LongRefAwaitThread(BetaLongRef ref, final long value){
        this(ref, new LongPredicate(){
            @Override
            public boolean evaluate(long current) {
                return current == value;
            }
        });
    }

    public LongRefAwaitThread(BetaLongRef ref, LongPredicate predicate) {
        this.ref = ref;
        this.predicate = predicate;
    }

    @Override
    public void doRun() throws Exception {
        execute(new AtomicVoidClosure(){
            @Override
            public void execute(Transaction tx) throws Exception {
                ref.await(predicate);
            }
        });
    }
}
