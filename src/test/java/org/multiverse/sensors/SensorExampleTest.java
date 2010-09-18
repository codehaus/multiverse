package org.multiverse.sensors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

@Ignore
public class SensorExampleTest {
    private BetaStm stm;
    private BetaLongRef ref;

    @Before
    public void setUp() {
        stm = new BetaStm();
        stm.getSimpleProfiler().startPrintingDaemon();
        ref = newLongRef(stm);
    }

    @Test
    public void test() {
        WorkerThread[] threads = new WorkerThread[5];
        for(int k=0;k<threads.length;k++){
            threads[k]=new WorkerThread(k);
        }

        startAll(threads);
        joinAll(threads);
    }

    public class WorkerThread extends TestThread {

        public WorkerThread(int id){
            super("WorkerThread-"+id);
        }

        @Override
        public void doRun() throws Exception {
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .setFamilyName(getName())
                    .buildAtomicBlock();

            System.out.println(block.getTransactionFactory().getTransactionConfiguration());

            for (int k = 0; k < 10000; k++) {
                block.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        BetaTransaction btx = (BetaTransaction) tx;
                        btx.openForWrite(ref, false).value++;
                    }
                });
            }
        }
    }
}
