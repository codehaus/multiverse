package org.multiverse.stms.beta;

import org.multiverse.stms.beta.transactions.*;

/**
 * A pool for tranlocals. The pool is not threadsafe and should be connected to a thread (can
 * be stored in a threadlocal). Eventually the performance of the stm will be limited to the rate
 * of cleanup, and using a pool seriously improves scalability.
 * <p/>
 * Improvement: atm there is only one single type of tranlocal. If there are more types of tranlocals,
 * each class needs to have an index. This index can be used to determine the type of ref. If the pool
 * contains an array of arrays, where the first array is index based on the type of the ref, finding the
 * second array (that contains pooled tranlocals) can be found easily.
 * <p/>
 * ObjectPool is not thread safe and should not be shared between threads.
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class BetaTransactionPool {

    private final static boolean ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm,beta.BetaObjectPool.enabled","true"));

    private final boolean transactionPoolingEnabled;

    private final LeanMonoBetaTransaction[] poolLeanMonoBetaTransaction = new LeanMonoBetaTransaction[10];
    private int poolLeanMonoBetaTransactionIndex = -1;
    private final FatMonoBetaTransaction[] poolFatMonoBetaTransaction = new FatMonoBetaTransaction[10];
    private int poolFatMonoBetaTransactionIndex = -1;
    private final LeanArrayBetaTransaction[] poolLeanArrayBetaTransaction = new LeanArrayBetaTransaction[10];
    private int poolLeanArrayBetaTransactionIndex = -1;
    private final FatArrayBetaTransaction[] poolFatArrayBetaTransaction = new FatArrayBetaTransaction[10];
    private int poolFatArrayBetaTransactionIndex = -1;
    private final LeanArrayTreeBetaTransaction[] poolLeanArrayTreeBetaTransaction = new LeanArrayTreeBetaTransaction[10];
    private int poolLeanArrayTreeBetaTransactionIndex = -1;
    private final FatArrayTreeBetaTransaction[] poolFatArrayTreeBetaTransaction = new FatArrayTreeBetaTransaction[10];
    private int poolFatArrayTreeBetaTransactionIndex = -1;

    public BetaTransactionPool() {
        transactionPoolingEnabled = ENABLED;
    }

    /**
     * Takes a LeanMonoBetaTransaction from the pool.
     *
     * @return the taken LeanMonoBetaTransaction or null of none available.
     */
    public LeanMonoBetaTransaction takeLeanMonoBetaTransaction(){
        if(!transactionPoolingEnabled || poolLeanMonoBetaTransactionIndex == -1){
            return null;
        }

        LeanMonoBetaTransaction tx = poolLeanMonoBetaTransaction[poolLeanMonoBetaTransactionIndex];
        poolLeanMonoBetaTransaction[poolLeanMonoBetaTransactionIndex]=null;
        poolLeanMonoBetaTransactionIndex--;
        return tx;
    }

    /**
     * Takes a FatMonoBetaTransaction from the pool.
     *
     * @return the taken FatMonoBetaTransaction or null of none available.
     */
    public FatMonoBetaTransaction takeFatMonoBetaTransaction(){
        if(!transactionPoolingEnabled || poolFatMonoBetaTransactionIndex == -1){
            return null;
        }

        FatMonoBetaTransaction tx = poolFatMonoBetaTransaction[poolFatMonoBetaTransactionIndex];
        poolFatMonoBetaTransaction[poolFatMonoBetaTransactionIndex]=null;
        poolFatMonoBetaTransactionIndex--;
        return tx;
    }

    /**
     * Takes a LeanArrayBetaTransaction from the pool.
     *
     * @return the taken LeanArrayBetaTransaction or null of none available.
     */
    public LeanArrayBetaTransaction takeLeanArrayBetaTransaction(){
        if(!transactionPoolingEnabled || poolLeanArrayBetaTransactionIndex == -1){
            return null;
        }

        LeanArrayBetaTransaction tx = poolLeanArrayBetaTransaction[poolLeanArrayBetaTransactionIndex];
        poolLeanArrayBetaTransaction[poolLeanArrayBetaTransactionIndex]=null;
        poolLeanArrayBetaTransactionIndex--;
        return tx;
    }

    /**
     * Takes a FatArrayBetaTransaction from the pool.
     *
     * @return the taken FatArrayBetaTransaction or null of none available.
     */
    public FatArrayBetaTransaction takeFatArrayBetaTransaction(){
        if(!transactionPoolingEnabled || poolFatArrayBetaTransactionIndex == -1){
            return null;
        }

        FatArrayBetaTransaction tx = poolFatArrayBetaTransaction[poolFatArrayBetaTransactionIndex];
        poolFatArrayBetaTransaction[poolFatArrayBetaTransactionIndex]=null;
        poolFatArrayBetaTransactionIndex--;
        return tx;
    }

    /**
     * Takes a LeanArrayTreeBetaTransaction from the pool.
     *
     * @return the taken LeanArrayTreeBetaTransaction or null of none available.
     */
    public LeanArrayTreeBetaTransaction takeLeanArrayTreeBetaTransaction(){
        if(!transactionPoolingEnabled || poolLeanArrayTreeBetaTransactionIndex == -1){
            return null;
        }

        LeanArrayTreeBetaTransaction tx = poolLeanArrayTreeBetaTransaction[poolLeanArrayTreeBetaTransactionIndex];
        poolLeanArrayTreeBetaTransaction[poolLeanArrayTreeBetaTransactionIndex]=null;
        poolLeanArrayTreeBetaTransactionIndex--;
        return tx;
    }

    /**
     * Takes a FatArrayTreeBetaTransaction from the pool.
     *
     * @return the taken FatArrayTreeBetaTransaction or null of none available.
     */
    public FatArrayTreeBetaTransaction takeFatArrayTreeBetaTransaction(){
        if(!transactionPoolingEnabled || poolFatArrayTreeBetaTransactionIndex == -1){
            return null;
        }

        FatArrayTreeBetaTransaction tx = poolFatArrayTreeBetaTransaction[poolFatArrayTreeBetaTransactionIndex];
        poolFatArrayTreeBetaTransaction[poolFatArrayTreeBetaTransactionIndex]=null;
        poolFatArrayTreeBetaTransactionIndex--;
        return tx;
    }


    /**
     * Puts a BetaTransaction in the pool.
     *
     * todo: This is where the cleanup of the Transaction should be done..
     *
     * @param tx the BetaTransaction to put in the pool.
     * @throws NullPointerException if tx is null.
     */
    public void putBetaTransaction(BetaTransaction tx){
        if(tx == null){
            throw new NullPointerException();
        }

        if(!transactionPoolingEnabled){
            return;
        }

        switch(tx.getPoolTransactionType()){
            case BetaTransaction.POOL_TRANSACTIONTYPE_LEAN_MONO:
            {
                if(poolLeanMonoBetaTransactionIndex == poolLeanMonoBetaTransaction.length - 1){
                    return;
                }

                poolLeanMonoBetaTransactionIndex++;
                poolLeanMonoBetaTransaction[poolLeanMonoBetaTransactionIndex] = (LeanMonoBetaTransaction)tx;
            }
            break;
            case BetaTransaction.POOL_TRANSACTIONTYPE_FAT_MONO:
            {
                if(poolFatMonoBetaTransactionIndex == poolFatMonoBetaTransaction.length - 1){
                    return;
                }

                poolFatMonoBetaTransactionIndex++;
                poolFatMonoBetaTransaction[poolFatMonoBetaTransactionIndex] = (FatMonoBetaTransaction)tx;
            }
            break;
            case BetaTransaction.POOL_TRANSACTIONTYPE_LEAN_ARRAY:
            {
                if(poolLeanArrayBetaTransactionIndex == poolLeanArrayBetaTransaction.length - 1){
                    return;
                }

                poolLeanArrayBetaTransactionIndex++;
                poolLeanArrayBetaTransaction[poolLeanArrayBetaTransactionIndex] = (LeanArrayBetaTransaction)tx;
            }
            break;
            case BetaTransaction.POOL_TRANSACTIONTYPE_FAT_ARRAY:
            {
                if(poolFatArrayBetaTransactionIndex == poolFatArrayBetaTransaction.length - 1){
                    return;
                }

                poolFatArrayBetaTransactionIndex++;
                poolFatArrayBetaTransaction[poolFatArrayBetaTransactionIndex] = (FatArrayBetaTransaction)tx;
            }
            break;
            case BetaTransaction.POOL_TRANSACTIONTYPE_LEAN_ARRAYTREE:
            {
                if(poolLeanArrayTreeBetaTransactionIndex == poolLeanArrayTreeBetaTransaction.length - 1){
                    return;
                }

                poolLeanArrayTreeBetaTransactionIndex++;
                poolLeanArrayTreeBetaTransaction[poolLeanArrayTreeBetaTransactionIndex] = (LeanArrayTreeBetaTransaction)tx;
            }
            break;
            case BetaTransaction.POOL_TRANSACTIONTYPE_FAT_ARRAYTREE:
            {
                if(poolFatArrayTreeBetaTransactionIndex == poolFatArrayTreeBetaTransaction.length - 1){
                    return;
                }

                poolFatArrayTreeBetaTransactionIndex++;
                poolFatArrayTreeBetaTransaction[poolFatArrayTreeBetaTransactionIndex] = (FatArrayTreeBetaTransaction)tx;
            }
            break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
