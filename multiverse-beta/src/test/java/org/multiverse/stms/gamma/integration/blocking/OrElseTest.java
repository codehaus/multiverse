package org.multiverse.stms.gamma.integration.blocking;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.StmUtils;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicLongClosure;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.references.LongRef;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.multiverse.api.StmUtils.newLongRef;
import static org.multiverse.api.StmUtils.retry;

/**
 * @author Peter Veentjer
 */
public class OrElseTest {

    @Test(expected = TransactionRequiredException.class)
    public void whenCalledWithoutTransaction_thenTransactionRequiredException(){
        AtomicClosure closure = mock(AtomicClosure.class);

        StmUtils.execute(closure, closure);
    }

    @Test
    public void whenEitherBranchIsSuccess() {
        final LongRef ref1 = newLongRef(1);
        final LongRef ref2 = newLongRef(0);

        long value = StmUtils.execute(new AtomicLongClosure() {
            @Override
            public long execute(Transaction tx) throws Exception {
                return StmUtils.execute(new GetClosure(ref1), new GetClosure(ref2));
            }
        });

        assertEquals(1, value);
    }

    class GetClosure implements AtomicLongClosure {
        private final LongRef ref;

        GetClosure(LongRef ref) {
            this.ref = ref;
        }

        @Override
        public long execute(Transaction tx) throws Exception {
            if (ref.get() == 0) {
                retry();
            }

            return ref.get();
        }
    }

    @Test
    @Ignore
    public void whenOrElseBranchIsSuccess() {
        final LongRef ref1 = newLongRef(0);
        final LongRef ref2 = newLongRef(2);

        long value = StmUtils.execute(new AtomicLongClosure() {
            @Override
            public long execute(Transaction tx) throws Exception {
                return StmUtils.execute(new GetClosure(ref1), new GetClosure(ref2));
            }
        });

        assertEquals(2, value);
    }

    @Test
    @Ignore
    public void whenBothBranchedBlock(){

    }
}
