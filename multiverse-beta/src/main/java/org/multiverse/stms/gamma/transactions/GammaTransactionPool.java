package org.multiverse.stms.gamma.transactions;

public class GammaTransactionPool {

    private final static boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("org.multiverse.stm,beta.transactions.BetaTransactionPool.enabled", "true"));

    private final boolean enabled;

    private final MonoGammaTransaction[] poolMonoGammaTransaction = new MonoGammaTransaction[10];
    private int poolMonoGammaTransactionIndex = -1;
    private final ArrayGammaTransaction[] poolFatArrayGammaTransaction = new ArrayGammaTransaction[10];
    private int poolFatArrayGammaTransactionIndex = -1;
    private final MapGammaTransaction[] poolMapGammaTransaction = new MapGammaTransaction[10];
    private int poolMapGammaTransactionIndex = -1;

    public GammaTransactionPool() {
        enabled = ENABLED;
    }

    /**
     * Takes a FatMonoGammaTransaction from the pool.
     *
     * @return the taken FatMonoGammaTransaction or null of none available.
     */
    public MonoGammaTransaction takeMonoGammaTransaction() {
        if (!enabled || poolMonoGammaTransactionIndex == -1) {
            return null;
        }

        MonoGammaTransaction tx = poolMonoGammaTransaction[poolMonoGammaTransactionIndex];
        poolMonoGammaTransaction[poolMonoGammaTransactionIndex] = null;
        poolMonoGammaTransactionIndex--;
        return tx;
    }


    /**
     * Takes a FatArrayGammaTransaction from the pool.
     *
     * @return the taken FatArrayGammaTransaction or null of none available.
     */
    public ArrayGammaTransaction takeArrayGammaTransaction() {
        if (!enabled || poolFatArrayGammaTransactionIndex == -1) {
            return null;
        }

        ArrayGammaTransaction tx = poolFatArrayGammaTransaction[poolFatArrayGammaTransactionIndex];
        poolFatArrayGammaTransaction[poolFatArrayGammaTransactionIndex] = null;
        poolFatArrayGammaTransactionIndex--;
        return tx;
    }


    /**
     * Takes a FatArrayTreeGammaTransaction from the pool.
     *
     * @return the taken FatArrayTreeGammaTransaction or null of none available.
     */
    public MapGammaTransaction takeMapGammaTransaction() {
        if (!enabled || poolMapGammaTransactionIndex == -1) {
            return null;
        }

        MapGammaTransaction tx = poolMapGammaTransaction[poolMapGammaTransactionIndex];
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
                poolMonoGammaTransaction[poolMonoGammaTransactionIndex] = (MonoGammaTransaction) tx;
                break;
            case GammaTransaction.POOL_TRANSACTIONTYPE_ARRAY:
                if (poolFatArrayGammaTransactionIndex == poolFatArrayGammaTransaction.length - 1) {
                    return;
                }

                poolFatArrayGammaTransactionIndex++;
                poolFatArrayGammaTransaction[poolFatArrayGammaTransactionIndex] = (ArrayGammaTransaction) tx;
                break;
            case GammaTransaction.POOL_TRANSACTIONTYPE_MAP:
                if (poolMapGammaTransactionIndex == poolMapGammaTransaction.length - 1) {
                    return;
                }

                poolMapGammaTransactionIndex++;
                poolMapGammaTransaction[poolMapGammaTransactionIndex] = (MapGammaTransaction) tx;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
