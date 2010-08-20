package org.multiverse.stms.beta;

import org.multiverse.api.closures.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * @author Peter Veentjer
 */
public final class LeanBetaAtomicBlock extends AbstractBetaAtomicBlock{

    public LeanBetaAtomicBlock(final BetaTransactionFactory transactionFactory) {
        super(transactionFactory);
    }

    public BetaTransactionFactory getTransactionFactory(){
        return transactionFactory;
    }
    public <E> E execute(final AtomicClosure<E> closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        BetaTransaction tx = (BetaTransaction)getThreadLocalTransaction();
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            setThreadLocalTransaction(tx);
            boolean abort = true;

            try {
                do {
                    try {
                        E result = closure.execute(tx);
                        tx.commit(pool);
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        waitForChange(pool, tx);
                        abort = false;
                    } catch (SpeculativeConfigurationError e) {
                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx, pool);
                        pool.putBetaTransaction(old);
                        abort = false;
                    } catch (ReadConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    } catch (WriteConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    }
                } while (tx.softReset(pool));
            } finally {
                if (abort) {
                    tx.abort(pool);
                }

                pool.putBetaTransaction(tx);
                clearThreadLocalTransaction();
            }
        }catch(Exception e){
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new InvisibleCheckedException(e);
            }
        }

        throw new TooManyRetriesException(
            format("Maximum number of %s retries has been reached for transaction '%s'",
                transactionConfiguration.getMaxRetries(), transactionConfiguration.getFamilyName()));

    }
 
    public  int execute(final AtomicIntClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        BetaTransaction tx = (BetaTransaction)getThreadLocalTransaction();
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            setThreadLocalTransaction(tx);
            boolean abort = true;

            try {
                do {
                    try {
                        int result = closure.execute(tx);
                        tx.commit(pool);
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        waitForChange(pool, tx);
                        abort = false;
                    } catch (SpeculativeConfigurationError e) {
                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx, pool);
                        pool.putBetaTransaction(old);
                        abort = false;
                    } catch (ReadConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    } catch (WriteConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    }
                } while (tx.softReset(pool));
            } finally {
                if (abort) {
                    tx.abort(pool);
                }

                pool.putBetaTransaction(tx);
                clearThreadLocalTransaction();
            }
        }catch(Exception e){
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new InvisibleCheckedException(e);
            }
        }

        throw new TooManyRetriesException(
            format("Maximum number of %s retries has been reached for transaction '%s'",
                transactionConfiguration.getMaxRetries(), transactionConfiguration.getFamilyName()));

    }
 
    public  long execute(final AtomicLongClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        BetaTransaction tx = (BetaTransaction)getThreadLocalTransaction();
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            setThreadLocalTransaction(tx);
            boolean abort = true;

            try {
                do {
                    try {
                        long result = closure.execute(tx);
                        tx.commit(pool);
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        waitForChange(pool, tx);
                        abort = false;
                    } catch (SpeculativeConfigurationError e) {
                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx, pool);
                        pool.putBetaTransaction(old);
                        abort = false;
                    } catch (ReadConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    } catch (WriteConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    }
                } while (tx.softReset(pool));
            } finally {
                if (abort) {
                    tx.abort(pool);
                }

                pool.putBetaTransaction(tx);
                clearThreadLocalTransaction();
            }
        }catch(Exception e){
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new InvisibleCheckedException(e);
            }
        }

        throw new TooManyRetriesException(
            format("Maximum number of %s retries has been reached for transaction '%s'",
                transactionConfiguration.getMaxRetries(), transactionConfiguration.getFamilyName()));

    }
 
    public  double execute(final AtomicDoubleClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        BetaTransaction tx = (BetaTransaction)getThreadLocalTransaction();
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            setThreadLocalTransaction(tx);
            boolean abort = true;

            try {
                do {
                    try {
                        double result = closure.execute(tx);
                        tx.commit(pool);
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        waitForChange(pool, tx);
                        abort = false;
                    } catch (SpeculativeConfigurationError e) {
                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx, pool);
                        pool.putBetaTransaction(old);
                        abort = false;
                    } catch (ReadConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    } catch (WriteConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    }
                } while (tx.softReset(pool));
            } finally {
                if (abort) {
                    tx.abort(pool);
                }

                pool.putBetaTransaction(tx);
                clearThreadLocalTransaction();
            }
        }catch(Exception e){
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new InvisibleCheckedException(e);
            }
        }

        throw new TooManyRetriesException(
            format("Maximum number of %s retries has been reached for transaction '%s'",
                transactionConfiguration.getMaxRetries(), transactionConfiguration.getFamilyName()));

    }
 
    public  boolean execute(final AtomicBooleanClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        BetaTransaction tx = (BetaTransaction)getThreadLocalTransaction();
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            setThreadLocalTransaction(tx);
            boolean abort = true;

            try {
                do {
                    try {
                        boolean result = closure.execute(tx);
                        tx.commit(pool);
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        waitForChange(pool, tx);
                        abort = false;
                    } catch (SpeculativeConfigurationError e) {
                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx, pool);
                        pool.putBetaTransaction(old);
                        abort = false;
                    } catch (ReadConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    } catch (WriteConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    }
                } while (tx.softReset(pool));
            } finally {
                if (abort) {
                    tx.abort(pool);
                }

                pool.putBetaTransaction(tx);
                clearThreadLocalTransaction();
            }
        }catch(Exception e){
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new InvisibleCheckedException(e);
            }
        }

        throw new TooManyRetriesException(
            format("Maximum number of %s retries has been reached for transaction '%s'",
                transactionConfiguration.getMaxRetries(), transactionConfiguration.getFamilyName()));

    }
 
    public  void execute(final AtomicVoidClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        BetaTransaction tx = (BetaTransaction)getThreadLocalTransaction();
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                closure.execute(tx);
                return;
            }

            tx = transactionFactory.start();
            setThreadLocalTransaction(tx);
            boolean abort = true;

            try {
                do {
                    try {
                        closure.execute(tx);
                        tx.commit(pool);
                        abort = false;
                        return;
                    } catch (Retry e) {
                        waitForChange(pool, tx);
                        abort = false;
                    } catch (SpeculativeConfigurationError e) {
                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx, pool);
                        pool.putBetaTransaction(old);
                        abort = false;
                    } catch (ReadConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    } catch (WriteConflict e) {
                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                        abort = false;
                    }
                } while (tx.softReset(pool));
            } finally {
                if (abort) {
                    tx.abort(pool);
                }

                pool.putBetaTransaction(tx);
                clearThreadLocalTransaction();
            }
        }catch(Exception e){
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new InvisibleCheckedException(e);
            }
        }

        throw new TooManyRetriesException(
            format("Maximum number of %s retries has been reached for transaction '%s'",
                transactionConfiguration.getMaxRetries(), transactionConfiguration.getFamilyName()));

    }
 
 
 }
