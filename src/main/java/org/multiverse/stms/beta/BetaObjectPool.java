package org.multiverse.stms.beta;

import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.beta.refs.*;
import org.multiverse.stms.beta.transactions.*;
import java.util.ArrayList;

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
public final class BetaObjectPool {

    private final static boolean ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm,beta.BetaObjectPool.enabled","true"));

    private final static boolean TRANLOCAL_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.tranlocalPooling",""+ENABLED));

    private final static boolean TRANLOCALARRAY_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.tranlocalArrayPooling",""+ENABLED));

    private final static boolean LATCH_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.latchPooling",""+ENABLED));

    private final static boolean LISTENER_POOLING_ENABLED  = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.listenersPooling",""+ENABLED));

    private final static boolean LISTENERSARRAY_POOLING_ENABLED  = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.listenersArrayPooling",""+ENABLED));

    private final static boolean TRANSACTION_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.transactionPooling",""+ENABLED));

    private final static boolean ARRAYLIST_POOLING_ENABLED = Boolean.parseBoolean(
        System.getProperty("org.multiverse.stm.beta.BetaObjectPool.arrayListPooling",""+ENABLED));

    private final boolean tranlocalPoolingEnabled;
    private final boolean tranlocalArrayPoolingEnabled;
    private final boolean latchPoolingEnabled;
    private final boolean listenersPoolingEnabled;
    private final boolean listenersArrayPoolingEnabled;
    private final boolean transactionPoolingEnabled;
    private final boolean arrayListPoolingEnabled;

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


    private final RefTranlocal[] tranlocalsRef = new RefTranlocal[100];
    private int lastUsedRef = -1;
    private final IntRefTranlocal[] tranlocalsIntRef = new IntRefTranlocal[100];
    private int lastUsedIntRef = -1;
    private final LongRefTranlocal[] tranlocalsLongRef = new LongRefTranlocal[100];
    private int lastUsedLongRef = -1;
    private TranlocalPool[] pools = new TranlocalPool[1000];

    private CheapLatch[] cheapLatchPool = new CheapLatch[10];
    private int cheapLatchPoolIndex = -1;

    private StandardLatch[] standardLatchPool = new StandardLatch[10];
    private int standardLatchPoolIndex = -1;   

    private Listeners[] listenersPool = new Listeners[1000];
    private int listenersPoolIndex = -1;

    private ArrayList[] arrayListPool = new ArrayList[10];
    private int arrayListPoolIndex = -1;

    public BetaObjectPool() {
        arrayListPoolingEnabled = ARRAYLIST_POOLING_ENABLED;
        tranlocalArrayPoolingEnabled = TRANLOCALARRAY_POOLING_ENABLED;
        tranlocalPoolingEnabled = TRANLOCAL_POOLING_ENABLED;
        latchPoolingEnabled = LATCH_POOLING_ENABLED;        
        listenersPoolingEnabled = LISTENER_POOLING_ENABLED;
        listenersArrayPoolingEnabled = LISTENERSARRAY_POOLING_ENABLED;
        transactionPoolingEnabled = TRANSACTION_POOLING_ENABLED;
    }

    /**
     * Takes a RefTranlocal from the pool for the specified Ref.
     *
     * @param owner the Ref to get the RefTranlocal for.     
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public RefTranlocal take(final Ref owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!tranlocalPoolingEnabled) {
            return null;
        }

        if (lastUsedRef == -1) {
            return null;
        }

        RefTranlocal tranlocal = tranlocalsRef[lastUsedRef];
        tranlocal.owner = owner;
        tranlocalsRef[lastUsedRef] = null;
        lastUsedRef--;
        return tranlocal;
    }

    /**
     * Puts an old RefTranlocal in this pool. If the tranlocal is allowed to be null,
     * the call is ignored. The same goes for when the tranlocal is permanent, since you
     * can't now how many transactions are still using it.
     *
     * @param tranlocal the RefTranlocal to pool.
     */
    public void put(final RefTranlocal tranlocal) {
        if (!tranlocalPoolingEnabled || tranlocal == null||tranlocal.isPermanent) {
            return;
        }

        if (lastUsedRef == tranlocalsRef.length - 1) {
            return;
        }

        tranlocal.prepareForPooling(this);
        lastUsedRef++;
        tranlocalsRef[lastUsedRef] = tranlocal;
    }

    /**
     * Takes a IntRefTranlocal from the pool for the specified IntRef.
     *
     * @param owner the IntRef to get the IntRefTranlocal for.     
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public IntRefTranlocal take(final IntRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!tranlocalPoolingEnabled) {
            return null;
        }

        if (lastUsedIntRef == -1) {
            return null;
        }

        IntRefTranlocal tranlocal = tranlocalsIntRef[lastUsedIntRef];
        tranlocal.owner = owner;
        tranlocalsIntRef[lastUsedIntRef] = null;
        lastUsedIntRef--;
        return tranlocal;
    }

    /**
     * Puts an old IntRefTranlocal in this pool. If the tranlocal is allowed to be null,
     * the call is ignored. The same goes for when the tranlocal is permanent, since you
     * can't now how many transactions are still using it.
     *
     * @param tranlocal the IntRefTranlocal to pool.
     */
    public void put(final IntRefTranlocal tranlocal) {
        if (!tranlocalPoolingEnabled || tranlocal == null||tranlocal.isPermanent) {
            return;
        }

        if (lastUsedIntRef == tranlocalsIntRef.length - 1) {
            return;
        }

        tranlocal.prepareForPooling(this);
        lastUsedIntRef++;
        tranlocalsIntRef[lastUsedIntRef] = tranlocal;
    }

    /**
     * Takes a LongRefTranlocal from the pool for the specified LongRef.
     *
     * @param owner the LongRef to get the LongRefTranlocal for.     
     * @return the pooled tranlocal, or null if none is found.
     * @throws NullPointerException if owner is null.
     */
    public LongRefTranlocal take(final LongRef owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if (!tranlocalPoolingEnabled) {
            return null;
        }

        if (lastUsedLongRef == -1) {
            return null;
        }

        LongRefTranlocal tranlocal = tranlocalsLongRef[lastUsedLongRef];
        tranlocal.owner = owner;
        tranlocalsLongRef[lastUsedLongRef] = null;
        lastUsedLongRef--;
        return tranlocal;
    }

    /**
     * Puts an old LongRefTranlocal in this pool. If the tranlocal is allowed to be null,
     * the call is ignored. The same goes for when the tranlocal is permanent, since you
     * can't now how many transactions are still using it.
     *
     * @param tranlocal the LongRefTranlocal to pool.
     */
    public void put(final LongRefTranlocal tranlocal) {
        if (!tranlocalPoolingEnabled || tranlocal == null||tranlocal.isPermanent) {
            return;
        }

        if (lastUsedLongRef == tranlocalsLongRef.length - 1) {
            return;
        }

        tranlocal.prepareForPooling(this);
        lastUsedLongRef++;
        tranlocalsLongRef[lastUsedLongRef] = tranlocal;
    }

    public Tranlocal take(final BetaTransactionalObject owner) {
        if (owner == null) {
            throw new NullPointerException();
        }

        if(!tranlocalPoolingEnabled){
            return null;
        }

        int classIndex = owner.getClassIndex();

        switch(classIndex){
            case 0:
                return take((Ref)owner);
            case 1:
                return take((IntRef)owner);
            case 2:
                return take((LongRef)owner);
        }

        if(classIndex >= pools.length){
            return null;
        }

        TranlocalPool pool = pools[classIndex];
        if(pool.lastUsed == -1){
            return null;
        }

        Tranlocal tranlocal = pool.tranlocals[pool.lastUsed];
        tranlocal.owner = owner;
        pool.tranlocals[pool.lastUsed] = null;
        pool.lastUsed--;
        return tranlocal;
    }

    public void put(final Tranlocal tranlocal) {
        if (!tranlocalPoolingEnabled || tranlocal == null || tranlocal.isPermanent) {
            return;
        }

        BetaTransactionalObject owner = tranlocal.owner;
        int classIndex = owner.getClassIndex();

        switch(classIndex){
            case 0:
                put((RefTranlocal)tranlocal);
                return;
            case 1:
                put((IntRefTranlocal)tranlocal);
                return;
            case 2:
                put((LongRefTranlocal)tranlocal);
                return;
        }

        if(classIndex >= pools.length){
            TranlocalPool[] newPools = new TranlocalPool[pools.length * 2];
            System.arraycopy(pools, 0, newPools, 0, pools.length);
            pools = newPools;
        }

        TranlocalPool pool = pools[classIndex];
        if(pool == null){
            pool = new TranlocalPool();
            pools[classIndex]=pool;
        }

        if(pool.lastUsed == pool.tranlocals.length - 1){
            return;
        }

        tranlocal.prepareForPooling(this);
        pool.lastUsed++;
        pool.tranlocals[pool.lastUsed] = tranlocal;
    }

    static class TranlocalPool{
        int lastUsed = -1;
        Tranlocal[] tranlocals = new Tranlocal[100];
    }

    private Tranlocal[][] tranlocalArrayPool = new Tranlocal[8193][];

    public void putTranlocalArray(final Tranlocal[] array){
        if(array == null){
            throw new NullPointerException();
        }

        if(!tranlocalArrayPoolingEnabled){
            return;
        }

        if(array.length-1>tranlocalArrayPool.length){
            return;
        }

        int index = array.length;

        if(tranlocalArrayPool[index]!=null){
            return;
        }

        //lets clean the array
        for(int k=0;k < array.length;k++){
            array[k]=null;
        }

        tranlocalArrayPool[index]=array;
    }

    /**
     * Takes a tranlocal array from the pool
     */
    public Tranlocal[] takeTranlocalArray(final int size){
        if(size<0){
            throw new IllegalArgumentException();
        }

        if(!tranlocalArrayPoolingEnabled){
            return null;
        }

        int index = size;

        if(index>=tranlocalArrayPool.length){
            return null;
        }

        if(tranlocalArrayPool[index]==null){
            return null;
        }

        Tranlocal[] array = tranlocalArrayPool[index];
        tranlocalArrayPool[index]=null;
        return array;
    }

    public CheapLatch takeCheapLatch(){
        if(!latchPoolingEnabled || cheapLatchPoolIndex == -1){
            return null;
        }

        CheapLatch latch = cheapLatchPool[cheapLatchPoolIndex];
        cheapLatchPool[cheapLatchPoolIndex]=null;
        cheapLatchPoolIndex--;
        return latch;
    }

    public void putCheapLatch(CheapLatch latch){
        if(latch == null){
            throw new NullPointerException();
        }

        if(!latchPoolingEnabled || cheapLatchPoolIndex == cheapLatchPool.length-1){
            return;
        }

        latch.prepareForPooling();
        cheapLatchPoolIndex++;
        cheapLatchPool[cheapLatchPoolIndex]=latch;        
    }

    public StandardLatch takeStandardLatch(){
        if(!latchPoolingEnabled || standardLatchPoolIndex == -1){
            return null;
        }

        StandardLatch latch = standardLatchPool[standardLatchPoolIndex];
        standardLatchPool[standardLatchPoolIndex]=null;
        standardLatchPoolIndex--;
        return latch;
    }

    public void putStandardLatch(StandardLatch latch){
        if(latch == null){
            throw new NullPointerException();
        }

        if(!latchPoolingEnabled || standardLatchPoolIndex == standardLatchPool.length-1){
            return;
        }

        latch.prepareForPooling();
        standardLatchPoolIndex++;
        standardLatchPool[standardLatchPoolIndex]=latch;
    }

    // ====================== array list ===================================

    /**
     * Takes an ArrayList from the pool, The returned ArrayList is cleared.
     *
     * @return the ArrayList from the pool, or null of none is found.
     */
    public ArrayList takeArrayList(){
        if(!arrayListPoolingEnabled || arrayListPoolIndex == -1){
            return null;
        }

        ArrayList list = arrayListPool[arrayListPoolIndex];
        arrayListPool[arrayListPoolIndex]=null;
        arrayListPoolIndex--;
        return list;
    }

    /**
     * Puts an ArrayList in this pool. The ArrayList will be cleared before being placed
     * in the pool.
     *
     * @param list the ArrayList to place in the pool.
     * @throws NullPointerException if list is null.
     */
    public void putArrayList(ArrayList list){
        if(list == null){
            throw new NullPointerException();
        }

        if(!arrayListPoolingEnabled || arrayListPoolIndex == arrayListPool.length-1){
            return;
        }

        list.clear();
        arrayListPoolIndex++;
        arrayListPool[arrayListPoolIndex]=list;
    }


    // ============================ listeners ==================================

    public Listeners takeListeners(){
        if(!listenersPoolingEnabled || listenersPoolIndex == -1){
            return null;
        }

        Listeners listeners = listenersPool[listenersPoolIndex];
        listenersPool[listenersPoolIndex]=null;
        listenersPoolIndex--;
        return listeners;
    }

    public void putListeners(Listeners listeners){
        if(listeners == null){
            throw new NullPointerException();
        }

        if(!listenersPoolingEnabled || listenersPoolIndex == listenersPool.length-1){
            return;
        }

        listeners.prepareForPooling();
        listenersPoolIndex++;
        listenersPool[listenersPoolIndex]=listeners;
    }

    // ============================= listeners array =============================

    private Listeners[] listenersArray = new Listeners[100000];

    /**
     * Takes a Listeners array from the pool. If an array is returned, it is completely nulled.
     *
     * @param minimalSize the minimalSize of the Listeners array.
     * @returns the found Listeners array, or null if none is taken from the pool.
     * @throws IllegalArgumentException if minimalSize is smaller than 0.
     */
    public Listeners[] takeListenersArray(int minimalSize){
        if( minimalSize < 0 ){
            throw new IllegalArgumentException();
        }

        if(!listenersArrayPoolingEnabled){
            return null;
        }

        if(listenersArray == null || listenersArray.length < minimalSize){
            return null;
        }

        Listeners[] result = listenersArray;
        listenersArray = null;
        return result;
    }

    /**
     * Puts a Listeners array in the pool.
     *
     * Listeners array should be nulled before being put in the pool. It is not going to be done by this
     * BetaObjectPool but should be done when the listeners on the listeners array are notified.
     *
     * @throws NullPointerException if listenersArray is null.
     */
    public void putListenersArray(Listeners[] listenersArray){
        if(listenersArray == null){
            throw new NullPointerException();
        }

        if(!listenersArrayPoolingEnabled){
            return;
        }

        if(this.listenersArray!=listenersArray){
            return;
        }

        this.listenersArray = listenersArray;
    }

    // ========================== transactions ==============================

    public LeanMonoBetaTransaction takeLeanMonoBetaTransaction(){
        if(!transactionPoolingEnabled || poolLeanMonoBetaTransactionIndex == -1){
            return null;
        }

        LeanMonoBetaTransaction tx = poolLeanMonoBetaTransaction[poolLeanMonoBetaTransactionIndex];
        poolLeanMonoBetaTransaction[poolLeanMonoBetaTransactionIndex]=null;
        poolLeanMonoBetaTransactionIndex--;
        return tx;
    }

    public FatMonoBetaTransaction takeFatMonoBetaTransaction(){
        if(!transactionPoolingEnabled || poolFatMonoBetaTransactionIndex == -1){
            return null;
        }

        FatMonoBetaTransaction tx = poolFatMonoBetaTransaction[poolFatMonoBetaTransactionIndex];
        poolFatMonoBetaTransaction[poolFatMonoBetaTransactionIndex]=null;
        poolFatMonoBetaTransactionIndex--;
        return tx;
    }

    public LeanArrayBetaTransaction takeLeanArrayBetaTransaction(){
        if(!transactionPoolingEnabled || poolLeanArrayBetaTransactionIndex == -1){
            return null;
        }

        LeanArrayBetaTransaction tx = poolLeanArrayBetaTransaction[poolLeanArrayBetaTransactionIndex];
        poolLeanArrayBetaTransaction[poolLeanArrayBetaTransactionIndex]=null;
        poolLeanArrayBetaTransactionIndex--;
        return tx;
    }

    public FatArrayBetaTransaction takeFatArrayBetaTransaction(){
        if(!transactionPoolingEnabled || poolFatArrayBetaTransactionIndex == -1){
            return null;
        }

        FatArrayBetaTransaction tx = poolFatArrayBetaTransaction[poolFatArrayBetaTransactionIndex];
        poolFatArrayBetaTransaction[poolFatArrayBetaTransactionIndex]=null;
        poolFatArrayBetaTransactionIndex--;
        return tx;
    }

    public LeanArrayTreeBetaTransaction takeLeanArrayTreeBetaTransaction(){
        if(!transactionPoolingEnabled || poolLeanArrayTreeBetaTransactionIndex == -1){
            return null;
        }

        LeanArrayTreeBetaTransaction tx = poolLeanArrayTreeBetaTransaction[poolLeanArrayTreeBetaTransactionIndex];
        poolLeanArrayTreeBetaTransaction[poolLeanArrayTreeBetaTransactionIndex]=null;
        poolLeanArrayTreeBetaTransactionIndex--;
        return tx;
    }

    public FatArrayTreeBetaTransaction takeFatArrayTreeBetaTransaction(){
        if(!transactionPoolingEnabled || poolFatArrayTreeBetaTransactionIndex == -1){
            return null;
        }

        FatArrayTreeBetaTransaction tx = poolFatArrayTreeBetaTransaction[poolFatArrayTreeBetaTransactionIndex];
        poolFatArrayTreeBetaTransaction[poolFatArrayTreeBetaTransactionIndex]=null;
        poolFatArrayTreeBetaTransactionIndex--;
        return tx;
    }

  
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
