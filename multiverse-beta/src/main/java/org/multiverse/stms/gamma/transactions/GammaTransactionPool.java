package org.multiverse.stms.gamma.transactions;

import org.multiverse.stms.gamma.transactions.fat.FatLinkedGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatMapGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransaction;
import org.multiverse.stms.gamma.transactions.lean.LeanLinkedGammaTransaction;
import org.multiverse.stms.gamma.transactions.lean.LeanMonoGammaTransaction;

public class GammaTransactionPool {

    private final static boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("org.multiverse.stm,beta.transactions.BetaTransactionPool.enabled", "true"));

    private final boolean enabled;

    private final FatMonoGammaTransaction[] poolFatMono = new FatMonoGammaTransaction[10];
    private int poolFatMonoIndex = -1;
    private final FatLinkedGammaTransaction[] poolFatArray = new FatLinkedGammaTransaction[10];
    private int poolFatArrayIndex = -1;
    private final LeanMonoGammaTransaction[] poolLeanMono = new LeanMonoGammaTransaction[10];
    private int poolLeanMonoIndex = -1;
    private final LeanLinkedGammaTransaction[] poolLeanArray = new LeanLinkedGammaTransaction[10];
    private int poolLeanArrayIndex = -1;

    private final FatMapGammaTransaction[] poolMap = new FatMapGammaTransaction[10];
    private int poolMapIndex = -1;

    public GammaTransactionPool() {
        enabled = ENABLED;
    }

    /**
     * Takes a FatMonoGammaTransaction from the pool.
     *
     * @return the taken FatMonoGammaTransaction or null of none available.
     */
    public FatMonoGammaTransaction takeFatMono() {
        if (!enabled || poolFatMonoIndex == -1) {
            return null;
        }

        FatMonoGammaTransaction tx = poolFatMono[poolFatMonoIndex];
        poolFatMono[poolFatMonoIndex] = null;
        poolFatMonoIndex--;
        return tx;
    }


    /**
     * Takes a FatArrayGammaTransaction from the pool.
     *
     * @return the taken FatArrayGammaTransaction or null of none available.
     */
    public FatLinkedGammaTransaction takeFatArray() {
        if (!enabled || poolFatArrayIndex == -1) {
            return null;
        }

        FatLinkedGammaTransaction tx = poolFatArray[poolFatArrayIndex];
        poolFatArray[poolFatArrayIndex] = null;
        poolFatArrayIndex--;
        return tx;
    }

    /**
     * Takes a FatMonoGammaTransaction from the pool.
     *
     * @return the taken FatMonoGammaTransaction or null of none available.
     */
    public LeanMonoGammaTransaction takeLeanMono() {
        if (!enabled || poolLeanMonoIndex == -1) {
            return null;
        }

        LeanMonoGammaTransaction tx = poolLeanMono[poolLeanMonoIndex];
        poolLeanMono[poolLeanMonoIndex] = null;
        poolLeanMonoIndex--;
        return tx;
    }


    /**
     * Takes a FatArrayGammaTransaction from the pool.
     *
     * @return the taken FatArrayGammaTransaction or null of none available.
     */
    public LeanLinkedGammaTransaction takeLeanArray() {
        if (!enabled || poolLeanArrayIndex == -1) {
            return null;
        }

        LeanLinkedGammaTransaction tx = poolLeanArray[poolLeanArrayIndex];
        poolLeanArray[poolLeanArrayIndex] = null;
        poolLeanArrayIndex--;
        return tx;
    }


    /**
     * Takes a FatArrayTreeGammaTransaction from the pool.
     *
     * @return the taken FatArrayTreeGammaTransaction or null of none available.
     */
    public FatMapGammaTransaction takeMap() {
        if (!enabled || poolMapIndex == -1) {
            return null;
        }

        FatMapGammaTransaction tx = poolMap[poolMapIndex];
        poolMap[poolMapIndex] = null;
        poolMapIndex--;
        return tx;
    }

    /**
     * Puts a GammaTransaction in the pool.
     *
     * @param tx the GammaTransaction to put in the pool.
     * @throws NullPointerException if tx is null.
     */
    public void put(GammaTransaction tx) {
        if (!enabled) {
            return;
        }

        if (tx == null) {
            throw new NullPointerException();
        }

        switch (tx.transactionType) {
            case GammaTransaction.POOL_TRANSACTIONTYPE_FAT_MONO:
                if (poolFatMonoIndex == poolFatMono.length - 1) {
                    return;
                }

                poolFatMonoIndex++;
                poolFatMono[poolFatMonoIndex] = (FatMonoGammaTransaction) tx;
                break;
            case GammaTransaction.POOL_TRANSACTIONTYPE_FAT_ARRAY:
                if (poolFatArrayIndex == poolFatArray.length - 1) {
                    return;
                }

                poolFatArrayIndex++;
                poolFatArray[poolFatArrayIndex] = (FatLinkedGammaTransaction) tx;
                break;
            case GammaTransaction.POOL_TRANSACTIONTYPE_LEAN_MONO:
                if (poolLeanMonoIndex == poolLeanMono.length - 1) {
                    return;
                }

                poolLeanMonoIndex++;
                poolLeanMono[poolLeanMonoIndex] = (LeanMonoGammaTransaction) tx;
                break;
            case GammaTransaction.POOL_TRANSACTIONTYPE_LEAN_ARRAY:
                if (poolLeanArrayIndex == poolLeanArray.length - 1) {
                    return;
                }

                poolLeanArrayIndex++;
                poolLeanArray[poolLeanArrayIndex] = (LeanLinkedGammaTransaction) tx;
                break;
            case GammaTransaction.POOL_TRANSACTIONTYPE_MAP:
                if (poolMapIndex == poolMap.length - 1) {
                    return;
                }

                poolMapIndex++;
                poolMap[poolMapIndex] = (FatMapGammaTransaction) tx;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
