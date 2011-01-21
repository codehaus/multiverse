package org.multiverse.stms.gamma.transactions;

import org.multiverse.stms.gamma.transactions.fat.FatLinkedGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatMapGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransaction;

public class GammaTransactionPool {

    private final static boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("org.multiverse.stm,beta.transactions.BetaTransactionPool.enabled", "true"));

    private final boolean enabled;

    private final FatMonoGammaTransaction[] poolMonoGammaTransaction = new FatMonoGammaTransaction[10];
    private int poolMonoGammaTransactionIndex = -1;
    private final FatLinkedGammaTransaction[] poolFatArrayGammaTransaction = new FatLinkedGammaTransaction[10];
    private int poolFatArrayGammaTransactionIndex = -1;
    private final FatMapGammaTransaction[] poolMapGammaTransaction = new FatMapGammaTransaction[10];
    private int poolMapGammaTransactionIndex = -1;

    public GammaTransactionPool() {
        enabled = ENABLED;
    }

    /**
     * Takes a FatMonoGammaTransaction from the pool.
     *
     * @return the taken FatMonoGammaTransaction or null of none available.
     */
    public FatMonoGammaTransaction takeMonoGammaTransaction() {
        if (!enabled || poolMonoGammaTransactionIndex == -1) {
            return null;
        }

        FatMonoGammaTransaction tx = poolMonoGammaTransaction[poolMonoGammaTransactionIndex];
        poolMonoGammaTransaction[poolMonoGammaTransactionIndex] = null;
        poolMonoGammaTransactionIndex--;
        return tx;
    }


    /**
     * Takes a FatArrayGammaTransaction from the pool.
     *
     * @return the taken FatArrayGammaTransaction or null of none available.
     */
    public FatLinkedGammaTransaction takeArrayGammaTransaction() {
        if (!enabled || poolFatArrayGammaTransactionIndex == -1) {
            return null;
        }

        FatLinkedGammaTransaction tx = poolFatArrayGammaTransaction[poolFatArrayGammaTransactionIndex];
        poolFatArrayGammaTransaction[poolFatArrayGammaTransactionIndex] = null;
        poolFatArrayGammaTransactionIndex--;
        return tx;
    }


    /**
     * Takes a FatArrayTreeGammaTransaction from the pool.
     *
     * @return the taken FatArrayTreeGammaTransaction or null of none available.
     */
    public FatMapGammaTransaction takeMapGammaTransaction() {
        if (!enabled || poolMapGammaTransactionIndex == -1) {
            return null;
        }

        FatMapGammaTransaction tx = poolMapGammaTransaction[poolMapGammaTransactionIndex];
        poolMapGammaTransaction[poolMapGammaTransactionIndex] = null;
        poolMapGammaTransactionIndex--;
        return tx;
    }

    /**
     * Puts a GammaTransaction in the pool.
     * <p/>
     * todo: This is where the cleanup of the Transaction should be done..
     *
     * @param tx the GammaTransaction to put in the pool.
     * @throws NullPointerException if tx is null.
     */
    public void putGammaTransaction(GammaTransaction tx) {
        if (!enabled) {
            return;
        }

        if (tx == null) {
            throw new NullPointerException();
        }

        switch (tx.transactionType) {
            case GammaTransaction.POOL_TRANSACTIONTYPE_MONO:
                if (poolMonoGammaTransactionIndex == poolMonoGammaTransaction.length - 1) {
                    return;
                }

                poolMonoGammaTransactionIndex++;
                poolMonoGammaTransaction[poolMonoGammaTransactionIndex] = (FatMonoGammaTransaction) tx;
                break;
            case GammaTransaction.POOL_TRANSACTIONTYPE_ARRAY:
                if (poolFatArrayGammaTransactionIndex == poolFatArrayGammaTransaction.length - 1) {
                    return;
                }

                poolFatArrayGammaTransactionIndex++;
                poolFatArrayGammaTransaction[poolFatArrayGammaTransactionIndex] = (FatLinkedGammaTransaction) tx;
                break;
            case GammaTransaction.POOL_TRANSACTIONTYPE_MAP:
                if (poolMapGammaTransactionIndex == poolMapGammaTransaction.length - 1) {
                    return;
                }

                poolMapGammaTransactionIndex++;
                poolMapGammaTransaction[poolMapGammaTransactionIndex] = (FatMapGammaTransaction) tx;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
