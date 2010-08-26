package org.multiverse.stms.beta;

import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.closures.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransactionContainer;
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

    @Override
    public <E> E executeChecked(
        final AtomicClosure<E> closure)throws Exception{
    
        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public <E> E execute(final AtomicClosure<E> closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        E result = closure.execute(tx);
                        if(tx.getStatus().isAlive()){
                            tx.commit(pool);
                        }
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
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  int executeChecked(
        final AtomicIntClosure closure)throws Exception{
    
        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  int execute(final AtomicIntClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        int result = closure.execute(tx);
                        if(tx.getStatus().isAlive()){
                            tx.commit(pool);
                        }
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
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  long executeChecked(
        final AtomicLongClosure closure)throws Exception{
    
        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  long execute(final AtomicLongClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        long result = closure.execute(tx);
                        if(tx.getStatus().isAlive()){
                            tx.commit(pool);
                        }
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
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  double executeChecked(
        final AtomicDoubleClosure closure)throws Exception{
    
        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  double execute(final AtomicDoubleClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        double result = closure.execute(tx);
                        if(tx.getStatus().isAlive()){
                            tx.commit(pool);
                        }
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
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  boolean executeChecked(
        final AtomicBooleanClosure closure)throws Exception{
    
        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  boolean execute(final AtomicBooleanClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.start();
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        boolean result = closure.execute(tx);
                        if(tx.getStatus().isAlive()){
                            tx.commit(pool);
                        }
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
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  void executeChecked(
        final AtomicVoidClosure closure)throws Exception{
    
        try{
            execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  void execute(final AtomicVoidClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.getStatus().isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.getStatus().isAlive()){
                closure.execute(tx);
                return;
            }

            tx = transactionFactory.start();
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        closure.execute(tx);
                        if(tx.getStatus().isAlive()){
                            tx.commit(pool);
                        }
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
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
   }
