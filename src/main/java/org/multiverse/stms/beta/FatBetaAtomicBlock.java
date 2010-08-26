package org.multiverse.stms.beta;

import org.multiverse.api.PropagationLevel;
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
public final class FatBetaAtomicBlock extends AbstractBetaAtomicBlock{

    private final PropagationLevel propagationLevel;

    public FatBetaAtomicBlock(final BetaTransactionFactory transactionFactory) {
        super(transactionFactory);
        this.propagationLevel = transactionConfiguration.propagationLevel;
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx,transactionContainer, pool, closure);
                    } else {
                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        throw new NoTransactionFoundException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }
                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        throw new NoTransactionAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }
                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, pool, closure);
                    } else {
                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.start();
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, pool, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }
   }

   private <E> E execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final BetaObjectPool pool, final AtomicClosure<E> closure)throws Exception{

       try{
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx,transactionContainer, pool, closure);
                    } else {
                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        throw new NoTransactionFoundException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }
                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        throw new NoTransactionAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }
                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, pool, closure);
                    } else {
                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.start();
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, pool, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }
   }

   private  int execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final BetaObjectPool pool, final AtomicIntClosure closure)throws Exception{

       try{
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx,transactionContainer, pool, closure);
                    } else {
                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        throw new NoTransactionFoundException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }
                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        throw new NoTransactionAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }
                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, pool, closure);
                    } else {
                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.start();
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, pool, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }
   }

   private  long execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final BetaObjectPool pool, final AtomicLongClosure closure)throws Exception{

       try{
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx,transactionContainer, pool, closure);
                    } else {
                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        throw new NoTransactionFoundException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }
                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        throw new NoTransactionAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }
                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, pool, closure);
                    } else {
                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.start();
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, pool, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }
   }

   private  double execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final BetaObjectPool pool, final AtomicDoubleClosure closure)throws Exception{

       try{
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx,transactionContainer, pool, closure);
                    } else {
                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        throw new NoTransactionFoundException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }
                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        throw new NoTransactionAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }
                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, pool, closure);
                    } else {
                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.start();
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, pool, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }
   }

   private  boolean execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final BetaObjectPool pool, final AtomicBooleanClosure closure)throws Exception{

       try{
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        execute(tx,transactionContainer,  pool, closure);
                        return;
                    } else {
                        closure.execute(tx);
                        return;
                    }
                case Mandatory:
                    if (tx == null) {
                        throw new NoTransactionFoundException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }
                    closure.execute(tx);
                    return;
                case Never:
                    if (tx != null) {
                        throw new NoTransactionAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }
                    closure.execute(null);
                    return;
                case RequiresNew:
                    if (tx == null) {
                        tx = transactionFactory.start(pool);
                        transactionContainer.transaction = tx;
                        execute(tx, transactionContainer, pool, closure);
                        return;
                    } else {
                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.start();
                        transactionContainer.transaction = tx;
                        try {
                            execute(tx, transactionContainer, pool, closure);
                            return;
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    closure.execute(tx);
                    return;
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);            
        }
   }

   private  void execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final BetaObjectPool pool, final AtomicVoidClosure closure)throws Exception{

       try{
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
