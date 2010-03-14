package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.ControlFlowError;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.api.exceptions.TransactionTooSmallError;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.mixins.FastTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * A manual instrumented ref that extends the {@link FastTxObjectMixin}.
 *
 * @author Peter Veentjer.
 */
public class FastManualRef extends FastTxObjectMixin {

    public static FastManualRef createUncommitted() {
        return new FastManualRef(String.class);
    }

    public FastManualRef(AlphaStm stm) {
        this(stm, 0);
    }

    public FastManualRef(AlphaStm stm, final int value) {
        new TransactionTemplate(stm) {
            @Override
            public Object execute(Transaction t) {
                FastManualRefTranlocal tranlocal = (FastManualRefTranlocal) ((AlphaTransaction) t).openForWrite(
                        FastManualRef.this);
                tranlocal.value = value;
                return null;
            }
        }.execute();
    }

    public FastManualRef(AlphaTransaction t, final int value) {
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) ((AlphaTransaction) t).openForWrite(FastManualRef.this);
        tranlocal.value = value;
    }

    //this constructor is used for creating an uncommitted IntValue, class is used to prevent
    //overloading problems

    private FastManualRef(Class someClass) {
    }

    public int get(AlphaStm stm) {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder().setReadonly(true).build();
        return get(txFactory);
    }

    public int get(TransactionFactory txFactory) {
        return new TransactionTemplate<Integer>(txFactory) {
            @Override
            public Integer execute(Transaction t) throws Exception {
                return get((AlphaTransaction) t);
            }
        }.execute();
    }

    public int get(AlphaTransaction t) {
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) t.openForRead(this);
        return tranlocal.value;
    }

    public void inc(AlphaStm stm) {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder().setReadonly(false).build();
        inc(txFactory);
    }

    public void inc(TransactionFactory txFactory) {
        new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction t) {
                inc((AlphaTransaction) t);
                return null;
            }
        }.execute();
    }

    public void fastIncWithThreadLocal(TransactionFactory txFactory) {
        AlphaTransaction tx = (AlphaTransaction) txFactory.start();
        setThreadLocalTransaction(tx);
        try {
            int attempt = 0;
            do {
                try {
                    attempt++;

                    FastManualRefTranlocal tranlocal = (FastManualRefTranlocal) tx.openForWrite(this);
                    tranlocal.value++;

                    tx.commit();
                    return;
                } catch (TransactionTooSmallError ex) {
                    tx = (AlphaTransaction) txFactory.start();
                } catch (ControlFlowError throwable) {
                    tx.getConfig().getBackoffPolicy().delayedUninterruptible(tx, attempt);
                } finally {
                    if (tx.getStatus() != TransactionStatus.committed) {
                        if (attempt - 1 < tx.getConfig().getMaxRetryCount()) {
                            tx.restart();
                        } else {
                            tx.abort();
                        }
                    }
                }
            } while (attempt - 1 < tx.getConfig().getMaxRetryCount());

            String msg = format("Could not complete transaction '%s' within %s retries",
                    tx.getConfig().getFamilyName(), tx.getConfig().getMaxRetryCount());
            throw new TooManyRetriesException(msg);
        } finally {
            clearThreadLocalTransaction();
        }
    }

    public void fastIncWithoutThreadLocal(TransactionFactory txFactory) {
        AlphaTransaction tx = (AlphaTransaction) txFactory.start();
        int attempt = 0;
        do {
            try {
                attempt++;

                FastManualRefTranlocal tranlocal = (FastManualRefTranlocal) tx.openForWrite(this);
                tranlocal.value++;

                tx.commit();
                return;
            } catch (TransactionTooSmallError ex) {
                tx = (AlphaTransaction) txFactory.start();
            } catch (ControlFlowError throwable) {
                tx.getConfig().getBackoffPolicy().delayedUninterruptible(tx, attempt);
            } finally {
                if (tx.getStatus() != TransactionStatus.committed) {
                    if (attempt - 1 < tx.getConfig().getMaxRetryCount()) {
                        tx.restart();
                    } else {
                        tx.abort();
                    }
                }
            }
        } while (attempt - 1 < tx.getConfig().getMaxRetryCount());

        String msg = format("Could not complete transaction '%s' within %s retries",
                tx.getConfig().getFamilyName(), tx.getConfig().getMaxRetryCount());
        throw new TooManyRetriesException(msg);
    }


    public void inc(AlphaTransaction tx) {
        FastManualRefTranlocal tranlocal = (FastManualRefTranlocal) tx.openForWrite(this);
        tranlocal.value++;
    }

    public void set(AlphaStm stm, int value) {
        TransactionFactory factory = stm.getTransactionFactoryBuilder().setReadonly(false).build();
        set(factory, value);
    }

    public void set(TransactionFactory factory, final int value) {
        new TransactionTemplate(factory) {
            @Override
            public Object execute(Transaction t) throws Exception {
                set((AlphaTransaction) t, value);
                return null;
            }
        }.execute();
    }

    public void set(AlphaTransaction tx, int value) {
        FastManualRefTranlocal tranlocal = (FastManualRefTranlocal) tx.openForWrite(this);
        tranlocal.value = value;
    }

    @Override
    public FastManualRefTranlocal ___openUnconstructed() {
        return new FastManualRefTranlocal(this);
    }
}

