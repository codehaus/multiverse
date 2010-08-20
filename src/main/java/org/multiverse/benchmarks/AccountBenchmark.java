package org.multiverse.benchmarks;

import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatArrayBetaTransaction;

import java.util.Random;

import static org.multiverse.benchmarks.BenchmarkUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

/**
 * @author Peter Veentjer
 */
public class AccountBenchmark {

    private final int accountCount;
    private final BetaStm stm;
    private final int threadCount;
    private final int transactionCount;
    private final long initialAmount;
    private final float rate;
    private final int readFrequency;
    private final int writeFrequency;
    private final int max;

    public static void main(String[] args) {
        AccountBenchmark benchmark = new AccountBenchmark();
        benchmark.run();
    }

    public AccountBenchmark() {
        this.stm = new BetaStm();
        this.accountCount = 50000;
        this.threadCount = 8;
        this.transactionCount = 30 * 1000 * 1000;
        this.initialAmount = 1000;
        this.rate = 1.01f;
        this.readFrequency = 100;
        this.writeFrequency = 100;
        this.max = 100;
    }

    private void run() {
        System.out.printf("Multiverse> Account benchmark\n");
        System.out.printf("Multiverse> TransactionCount: %s\n", transactionCount);
        System.out.printf("Multiverse> Accounts:         %s\n", accountCount);
        System.out.printf("Multiverse> Threads:          %s\n", threadCount);
        System.out.printf("Multiverse> InitialAmount     %s\n", initialAmount);
        System.out.printf("Multiverse> Rate:             %s\n", rate);
        System.out.printf("Multiverse> InitialAmount:    %s\n", initialAmount);
        System.out.printf("Multiverse> WriteFrequency:   %s\n", readFrequency);
        System.out.printf("Multiverse> ReadFrequency:    %s\n", writeFrequency);
        System.out.printf("Multiverse> Max amount:       %s\n", max);

        LongRef[] accounts = createAccounts();

        Latch startLatch = new Latch();
        TransferThread[] threads = new TransferThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new TransferThread(k, startLatch, accounts);
        }
        System.out.println("Multiverse> Starting");
        startAll(threads);

        long startMs = System.currentTimeMillis();
        startLatch.open();

        joinAll(threads);

        long durationMs = System.currentTimeMillis() - startMs;

        long controlFlowConflictCount = 0;
        long readConflictCount = 0;
        long writeConflictCount = 0;
        long lockConflictCount = 0;
        long individualDurationMs = 0;
        for (TransferThread t : threads) {
            individualDurationMs += t.durationMs;
        }

        System.out.printf("Multiverse> Finished in %s ms\n", durationMs);
        System.out.printf("Multiverse> Average %s Transactions/second\n",
                transactionsPerSecond(transactionCount * threadCount, durationMs));
        System.out.printf("Multiverse> Average Individual %s Transactions/second\n",
                transactionsPerSecond(transactionCount * threadCount, individualDurationMs / threadCount));

        System.out.println("controlFlowErrorCount: " + controlFlowConflictCount);
        System.out.println("readConflictCount: " + readConflictCount);
        System.out.println("writeConflictCount: " + writeConflictCount);
        System.out.println("lockConflictCount: " + lockConflictCount);
        System.out.println("conflictscans: " + FatArrayBetaTransaction.conflictScan);

        //printAccounts(accounts);

        //double percentage = 100f * Ref.pooled.get() / (Ref.pooled.get() + Ref.nonPooled.get());
        //System.out.printf("Multiverse> Pooled percentage %s\n", percentage);

        //long expected = transactionCount * threadCount * 2;
        //double x = (100f * Tranlocal.created.get()) / expected;
        //System.out.printf("Created real percentage %s\n", x);
    }

    private LongRef[] createAccounts() {
        LongRef[] accounts = new LongRef[accountCount];

        for (int k = 0; k < accounts.length; k++) {
            accounts[k] = createLongRef(stm, initialAmount);
        }
        return accounts;
    }

    private void printAccounts(LongRef[] accounts) {
        for (BetaTransactionalObject account : accounts) {
            System.out.println("Account: " + ((LongRefTranlocal) account.___unsafeLoad()).value + " " + account.___getOrec());
        }
    }

    private class TransferThread extends Thread {
        private final Latch startLatch;
        private final BetaObjectPool pool = new BetaObjectPool();
        private final Random random = new Random();
        private long durationMs;
        private LongRef[] accounts;

        public TransferThread(int id, Latch startLatch, final LongRef[] accounts) {
            super("TransferThread-" + id);
            this.setPriority(Thread.MAX_PRIORITY);

            this.startLatch = startLatch;
            this.accounts = accounts;
        }

        @Override
        public void run() {
            AtomicBlock addInterestBlock = stm.getTransactionFactoryBuilder()
                    .buildAtomicBlock();

            AtomicVoidClosure addInterestClosure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    for (int k = 0; k < accounts.length; k++) {
                        LongRef account = accounts[k];
                        LongRefTranlocal tranlocal = btx.openForWrite(account, false, pool);
                        tranlocal.value = Math.round(tranlocal.value * rate);
                    }
                }
            };

            AtomicBlock computeTotalBlock = stm.getTransactionFactoryBuilder()
                    .setReadonly(true)
                    .buildAtomicBlock();

            AtomicVoidClosure computeTotalClosure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    long sum = 0;
                    for (int k = 0; k < accounts.length; k++) {
                        LongRefTranlocal tranlocal = btx.openForWrite(accounts[k], false, pool);
                        sum += tranlocal.value;
                    }
                }
            };

            AtomicBlock transferBlock = stm.getTransactionFactoryBuilder()
                    .buildAtomicBlock();

            AtomicVoidClosure transferClosure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;

                    long amount = random.nextInt(max) + 1;

                    LongRef from = accounts[random.nextInt(accountCount)];
                    LongRef to = accounts[random.nextInt(accountCount)];

                    LongRefTranlocal fromTranlocal = btx.openForWrite(from, true, pool);
                    LongRefTranlocal toTranlocal = btx.openForWrite(to, true, pool);

                    //if (fromTranlocal.value < amount) {
                    //throw OverdraftException.INSTANCE;
                    //}

                    fromTranlocal.value -= amount;
                    toTranlocal.value += amount;
                }
            };

            startLatch.await();

            long startMs = System.currentTimeMillis();

            long _transactionCount = transactionCount;
            for (int k = 0; k < _transactionCount; k++) {
                transferBlock.execute(transferClosure);
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s finished in %s ms\n", getName(), durationMs);
        }
    }

    static class OverdraftException extends RuntimeException {
        final static OverdraftException INSTANCE = new OverdraftException();
    }
}
