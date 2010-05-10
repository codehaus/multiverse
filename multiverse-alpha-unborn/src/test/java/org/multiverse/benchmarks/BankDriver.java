package org.multiverse.benchmarks;

/**
 * @author Peter Veentjer
 */
public class BankDriver {

    /*
    int m_max = 10;
    int m_read_frequency = 0;
    int m_write_frequency = 0;
    int m_read_threads = 0;
    int m_write_threads = 0;
    Account[] m_accounts;

    public void init(String[] args) {


        int nb = 8;
        float init = 10000;
        boolean error = false;

        for (int i = 0; i < args.length && !error; i++) {
            if (args[i].equals("-n")) {
                if (++i < args.length)
                    nb = Integer.parseInt(args[i]);
                else
                    error = true;
            } else if (args[i].equals("-i")) {
                if (++i < args.length)
                    init = Float.parseFloat(args[i]);
                else
                    error = true;
            } else if (args[i].equals("-m")) {
                if (++i < args.length)
                    m_max = Integer.parseInt(args[i]);
                else
                    error = true;
            } else if (args[i].equals("-r")) {
                if (++i < args.length)
                    m_read_frequency = Integer.parseInt(args[i]);
                else
                    error = true;
            } else if (args[i].equals("-w")) {
                if (++i < args.length)
                    m_write_frequency = Integer.parseInt(args[i]);
                else
                    error = true;
            } else if (args[i].equals("-R")) {
                if (++i < args.length)
                    m_read_threads = Integer.parseInt(args[i]);
                else
                    error = true;
            } else if (args[i].equals("-W")) {
                if (++i < args.length)
                    m_write_threads = Integer.parseInt(args[i]);
                else
                    error = true;
            } else if (args[i].equals("-d")) {
                // Use disjoint sets of accounts
                Account.s_disjoint = true;
            } else if (args[i].equals("-y")) {
                // Can create livelocks
                Account.s_yield = true;
            } else
                error = true;
        }
        if (error) {
            System.out.println("Benchmark arguments: [-n nb-accounts] [-i initial-amount] [-m max-transfer] [-r read-all-frequency] [-w write-all-frequency] [-R read-all-threads] [-W write-all-threads] [-d] [-y]");
            System.exit(1);
        }
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
            transfers += ((BenchmarkThread) threads[i]).m_nb_transfers;
            reads += ((BenchmarkThread) threads[i]).m_nb_reads;
            writes += ((BenchmarkThread) threads[i]).m_nb_writes;
        }

        return "T=" + transfers + ", R=" + reads + ", W=" + writes + ", TOTAL=" + total + sb.toString();
    }

    static public class Bank {

        final private List<Account> m_accounts;

        public Bank() {
            m_accounts = new LinkedList<Account>();
        }

        public Account createAccount(String name) {
            Account a = new Account(name);
            m_accounts.add(a);
            return a;
        }
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

        private int m_steps;

        public DeuceBenchmarkThread() {
            m_steps = 0;
        }

        public void setPhase(int phase) {
            m_phase = phase;
        }

        public int getSteps() {
            return m_steps;
        }

        public void run() {
             while (m_phase == TEST_PHASE) {
                step();
                m_steps++;
            }
        }

        abstract protected void step();

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

        protected void step() {

            if (m_id < m_read_threads) {
                // Compute total of all accounts (read-all transaction)
                Account.computeTotal(m_accounts);
                    m_nb_reads++;
            } else if (m_id < m_read_threads + m_write_threads) {
                // Add 0% interest (write-all transaction)
                Account.addInterest(m_accounts, 0);
                    m_nb_writes++;
            } else {
                int i = m_random.nextInt(100);
                if (i < m_read_frequency) {
                    // Compute total of all accounts (read-all transaction)
                    Account.computeTotal(m_accounts);
                        m_nb_reads++;
                } else if (i < m_read_frequency + m_write_frequency) {
                    // Add 0% interest (write-all transaction)
                    Account.addInterest(m_accounts, 0);
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
      */
}
