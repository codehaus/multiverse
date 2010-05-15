package org.multiverse.benchmarks;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import java.util.Random;

/**
 * @author Peter Veentjer
 */
public class BankDriver {
    int nb_threads = 8;
    int duration = 100000;
    int warmup = 2000;

    int m_max = 10;
    int m_read_frequency = 80;
    int m_write_frequency = 20;
    int m_read_threads = 5;
    int m_write_threads = 5;
    int nb = 10000;
    Account[] m_accounts;
    int init = 100;

    @Before
    public void before() {
        m_accounts = new Account[nb];
        for (int i = 0; i < m_accounts.length; i++) {
            m_accounts[i] = new Account("" + i);
            m_accounts[i].deposit(init);
        }
        System.out.println("Number of accounts  = " + nb);
        System.out.println("Initial amount      = " + init);
        System.out.println("Maximal transfer    = " + m_max);
        System.out.println("Read-all frequency  = " + m_read_frequency + "%");
        System.out.println("Write-all frequency = " + m_write_frequency + "%");
        System.out.println("Read-all threads    = " + m_read_threads);
        System.out.println("Write-all threads   = " + m_write_threads);
        System.out.println("Disjoint            = " + Account.s_disjoint);
        System.out.println("Yield               = " + Account.s_yield);
        System.out.println();
    }

    @Test
    public void test() {

        BenchmarkThread[] bt = new BenchmarkThread[nb_threads];
        for (int i = 0; i < bt.length; i++)
            bt[i] = createThread(i, bt.length);

        Thread[] t = new Thread[bt.length];
        for (int i = 0; i < t.length; i++)
            t[i] = new Thread(bt[i]);

        System.out.print("Starting threads...");
        for (int i = 0; i < t.length; i++) {
            System.out.print(" " + i);
            bt[i].setPhase(DeuceBenchmark.WARMUP_PHASE);
            t[i].start();
        }
        System.out.println();

        long wstart = System.currentTimeMillis();
        try {
            Thread.sleep(warmup);
        } catch (InterruptedException e) {
        }
        long wend = System.currentTimeMillis();

        System.out.print("End of warmup phase...");
        for (int i = 0; i < bt.length; i++) {
            System.out.print(" " + i);
            bt[i].setPhase(DeuceBenchmark.TEST_PHASE);
        }
        System.out.println();

        long tstart = System.currentTimeMillis();
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
        }
        long tend = System.currentTimeMillis();

        System.out.print("End of test phase...");
        for (int i = 0; i < bt.length; i++) {
            System.out.print(" " + i);
            bt[i].setPhase(DeuceBenchmark.SHUTDOWN_PHASE);
        }
        System.out.println();

        System.out.println("Waiting for threads to finish...");
        for (int i = 0; i < t.length; i++) {
            try {
                t[i].join();
            } catch (InterruptedException e) {
            }
        }
        System.out.println("All threads returned successfully");

        int steps = 0;
        for (int i = 0; i < bt.length; i++)
            steps += bt[i].getSteps();

        System.out.println("RESULTS:\n");
        System.out.println("  Warmup duration (ms) = " + (wend - wstart));
        System.out.println("  Test duration (ms)   = " + (tend - tstart));
        System.out.println("  Nb iterations        = " + steps);
        System.out.println("  Stats                = " + getStats(bt));
        for (int i = 0; i < bt.length; i++)
            System.out.println("    " + i + " : " + bt[i].getSteps() +
                    " (" + bt[i].getStats() + ")");
    }

    public BenchmarkThread createThread(int i, int nb) {
        return new BenchmarkThread(i, nb, m_accounts, m_max, m_read_frequency, m_write_frequency, m_read_threads, m_write_threads);
    }

    public String getStats(BenchmarkThread[] threads) {
        int total = 0;
        StringBuffer sb = new StringBuffer();
        sb.append(" [");
        for (int i = 0; i < m_accounts.length; i++) {
            total += m_accounts[i].getBalance();
            sb.append(" " + i + "=" + m_accounts[i].getBalance());
        }
        sb.append(" ]");
        int transfers = 0;
        int reads = 0;
        int writes = 0;
        for (int i = 0; i < threads.length; i++) {
            transfers += threads[i].m_nb_transfers;
            reads += threads[i].m_nb_reads;
            writes += threads[i].m_nb_writes;
        }

        return "T=" + transfers + ", R=" + reads + ", W=" + writes + ", TOTAL=" + total + sb.toString();
    }


    @TransactionalObject
    static public class Account {

        static volatile boolean s_disjoint = false;
        static volatile boolean s_yield = false;

        final private String m_name;
        private float m_balance;

        public Account() {
            m_name = "Empty";
            m_balance = 0;
        }

        public Account(String name) {
            m_name = name;
            m_balance = 0;
        }

        public String getName() {
            return m_name;
        }

        public float getBalance() {
            return m_balance;
        }

        public void deposit(float amount) {
            m_balance += amount;
        }

        public void withdraw(float amount) throws OverdraftException {
            if (m_balance < amount)
                throw new OverdraftException("Cannot withdraw $" + amount + " from $" + m_balance);
            m_balance -= amount;
        }

        @TransactionalMethod
        static public void addInterest(Account[] accounts, float rate) {
            for (Account a : accounts) {
                a.deposit(a.getBalance() * rate);
                if (s_yield)
                    Thread.yield();
            }
        }

        @TransactionalMethod
        static public double computeTotal(Account[] accounts) {
            double total = 0.0;
            for (Account a : accounts) {
                total += a.getBalance();
                if (s_yield)
                    Thread.yield();
            }
            return total;
        }

        @TransactionalMethod
        static public void transfer(Account src, Account dst, float amount) throws OverdraftException {
            dst.deposit(amount);
            if (s_yield)
                Thread.yield();
            src.withdraw(amount);
        }
    }

    static public class OverdraftException extends Exception {

        public OverdraftException() {
            super();
        }

        public OverdraftException(String reason) {
            super(reason);
        }
    }

    static abstract public class DeuceBenchmarkThread implements Runnable {

        volatile private int m_phase;
        private int m_steps;

        public DeuceBenchmarkThread() {
            m_phase = DeuceBenchmark.WARMUP_PHASE;
            m_steps = 0;
        }

        public void setPhase(int phase) {
            m_phase = phase;
        }

        public int getSteps() {
            return m_steps;
        }

        public void run() {
            while (m_phase == DeuceBenchmark.WARMUP_PHASE) {
                step(DeuceBenchmark.WARMUP_PHASE);
            }
            while (m_phase == DeuceBenchmark.TEST_PHASE) {
                step(DeuceBenchmark.TEST_PHASE);
                m_steps++;
            }
        }

        abstract protected void step(int phase);

        abstract public String getStats();
    }


    static public class BenchmarkThread extends DeuceBenchmarkThread {

        final private int m_id;
        final private int m_nb;
        final private Account[] m_accounts;
        final private int m_max;
        final private int m_read_frequency;
        final private int m_write_frequency;
        final private int m_read_threads;
        final private int m_write_threads;
        int m_nb_transfers;
        int m_nb_reads;
        int m_nb_writes;
        final private Random m_random;

        BenchmarkThread(int id, int nb, Account[] accounts, int max, int read_frequency, int write_frequency, int read_threads, int write_threads) {
            m_id = id;
            m_nb = nb;
            m_accounts = accounts;
            m_max = max;
            m_read_frequency = read_frequency;
            m_write_frequency = write_frequency;
            m_read_threads = read_threads;
            m_write_threads = write_threads;
            m_nb_transfers = m_nb_reads = m_nb_writes = 0;
            m_random = new Random();
        }

        protected void step(int phase) {

            if (m_id < m_read_threads) {
                // Compute total of all accounts (read-all transaction)
                Account.computeTotal(m_accounts);
                if (phase == DeuceBenchmark.TEST_PHASE)
                    m_nb_reads++;
            } else if (m_id < m_read_threads + m_write_threads) {
                // Add 0% interest (write-all transaction)
                Account.addInterest(m_accounts, 0);
                if (phase == DeuceBenchmark.TEST_PHASE)
                    m_nb_writes++;
            } else {
                int i = m_random.nextInt(100);
                if (i < m_read_frequency) {
                    // Compute total of all accounts (read-all transaction)
                    Account.computeTotal(m_accounts);
                    if (phase == DeuceBenchmark.TEST_PHASE)
                        m_nb_reads++;
                } else if (i < m_read_frequency + m_write_frequency) {
                    // Add 0% interest (write-all transaction)
                    Account.addInterest(m_accounts, 0);
                    if (phase == DeuceBenchmark.TEST_PHASE)
                        m_nb_writes++;
                } else {
                    int amount = m_random.nextInt(m_max) + 1;
                    Account src;
                    Account dst;
                    if (Account.s_disjoint && m_nb <= m_accounts.length) {
                        src = m_accounts[m_random.nextInt(m_accounts.length / m_nb) * m_nb + m_id];
                        dst = m_accounts[m_random.nextInt(m_accounts.length / m_nb) * m_nb + m_id];
                    } else {
                        src = m_accounts[m_random.nextInt(m_accounts.length)];
                        dst = m_accounts[m_random.nextInt(m_accounts.length)];
                    }

                    try {
                        Account.transfer(src, dst, amount);
                        if (phase == DeuceBenchmark.TEST_PHASE)
                            m_nb_transfers++;
                    } catch (OverdraftException e) {
                        System.err.println("Overdraft: " + e.getMessage());
                    }
                }
            }
        }

        public String getStats() {
            return "T=" + m_nb_transfers + ", R=" + m_nb_reads + ", W=" + m_nb_writes;
        }
    }

    public interface DeuceBenchmark {

        public static final int WARMUP_PHASE = 1;
        public static final int TEST_PHASE = 2;
        public static final int SHUTDOWN_PHASE = 3;
    }
}
