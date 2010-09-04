package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Watch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.Function;
import org.multiverse.api.functions.IntFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.conflictcounters.LocalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.*;

import java.util.Arrays;

import static java.lang.String.format;


/**
 * A {@link BetaTransaction} for arbitrary size transactions.
 *
 * @author Peter Veentjer.
 */
public final class FatArrayTreeBetaTransaction extends AbstractFatBetaTransaction {

    private Tranlocal[] array;        
    private LocalConflictCounter localConflictCounter;
    private int size;
    private boolean hasReads;
    private boolean hasUntrackedReads;
    private boolean hasUpdates;
    private boolean evaluatingCommute;

    public FatArrayTreeBetaTransaction(BetaStm stm) {
        this(new BetaTransactionConfiguration(stm));
    }

    public FatArrayTreeBetaTransaction(BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_FAT_ARRAYTREE, config);
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new Tranlocal[config.minimalArrayTreeSize];
        this.remainingTimeoutNs = config.timeoutNs;
    }

    public final LocalConflictCounter getLocalConflictCounter() {
        return localConflictCounter;
    }

    public int size(){
        return size;
    }

    public float getUsage(){
        return (size * 1.0f)/array.length;
    }


    private <E> void flattenCommute(
        final BetaRef<E> ref,
        final RefTranlocal<E> tranlocal,
        final boolean lock){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        final RefTranlocal<E> read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public <E> RefTranlocal<E> openForRead(
        final BetaRef<E> ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;
        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            RefTranlocal<E> found = (RefTranlocal<E>)array[index];


            if(found.isCommuting){
                flattenCommute(ref, found, lock);
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount,found)){
                throw abortOnReadConflict();
            }

            return found;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        final RefTranlocal<E> read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        if(lock || !read.isPermanent || config.trackReads){
            attach(ref, read, identityHashCode);
            size++;
        }else{
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public <E> RefTranlocal<E> openForWrite(
        final BetaRef<E>  ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];

            if(result.isCommuting){
                flattenCommute(ref, result, lock);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount,result)){
                throw abortOnReadConflict();
            }

            if(!result.isCommitted){
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            RefTranlocal<E> read = result;
            result = pool.take(ref);
            if (result == null) {
                result = new RefTranlocal<E>(ref);
            }

            result.value = read.value;
            result.read = read;
            hasUpdates = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final RefTranlocal<E> read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        //open the tranlocal for writing.
        RefTranlocal<E> result = read.openForWrite(pool);
        hasUpdates = true;
        attach(ref, result, identityHashCode);
        size++;        
        return result;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final BetaRef<E> ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            final RefTranlocal<E> result = (RefTranlocal<E>)array[index];
            if(result.isCommitted || result.read!=null){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        RefTranlocal<E> result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }
        result.isDirty = DIRTY_TRUE;
        attach(ref, result, identityHashCode);
        size++;
        return result;
    }

    public <E> void commute(
        final BetaRef<E> ref, final Function<E> function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }    

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if(index == -1){
            //todo: call to 'openForCommute' can be inlined.
            RefTranlocal<E> result = ref.___openForCommute(pool);
            attach(ref, result, identityHashCode);
            result.addCommutingFunction(function, pool);
            hasUpdates = true;
            size++;
            return;
        }

        RefTranlocal<E> result = (RefTranlocal<E>)array[index];
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final RefTranlocal<E> read = result;
            result =  pool.take(ref);
            if(result == null){
                result = new RefTranlocal<E>(ref);
            }
            result.read = read;
            result.value = read.value;
            hasUpdates = true;
            array[index]=result;
         }

         result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaIntRef ref,
        final IntRefTranlocal tranlocal,
        final boolean lock){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        final IntRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public  IntRefTranlocal openForRead(
        final BetaIntRef ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;
        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            IntRefTranlocal found = (IntRefTranlocal)array[index];


            if(found.isCommuting){
                flattenCommute(ref, found, lock);
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount,found)){
                throw abortOnReadConflict();
            }

            return found;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        final IntRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        if(lock || !read.isPermanent || config.trackReads){
            attach(ref, read, identityHashCode);
            size++;
        }else{
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public  IntRefTranlocal openForWrite(
        final BetaIntRef  ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            IntRefTranlocal result = (IntRefTranlocal)array[index];

            if(result.isCommuting){
                flattenCommute(ref, result, lock);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount,result)){
                throw abortOnReadConflict();
            }

            if(!result.isCommitted){
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            IntRefTranlocal read = result;
            result = pool.take(ref);
            if (result == null) {
                result = new IntRefTranlocal(ref);
            }

            result.value = read.value;
            result.read = read;
            hasUpdates = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final IntRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        //open the tranlocal for writing.
        IntRefTranlocal result = read.openForWrite(pool);
        hasUpdates = true;
        attach(ref, result, identityHashCode);
        size++;        
        return result;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final BetaIntRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            final IntRefTranlocal result = (IntRefTranlocal)array[index];
            if(result.isCommitted || result.read!=null){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        IntRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }
        result.isDirty = DIRTY_TRUE;
        attach(ref, result, identityHashCode);
        size++;
        return result;
    }

    public  void commute(
        final BetaIntRef ref, final IntFunction function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }    

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if(index == -1){
            //todo: call to 'openForCommute' can be inlined.
            IntRefTranlocal result = ref.___openForCommute(pool);
            attach(ref, result, identityHashCode);
            result.addCommutingFunction(function, pool);
            hasUpdates = true;
            size++;
            return;
        }

        IntRefTranlocal result = (IntRefTranlocal)array[index];
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final IntRefTranlocal read = result;
            result =  pool.take(ref);
            if(result == null){
                result = new IntRefTranlocal(ref);
            }
            result.read = read;
            result.value = read.value;
            hasUpdates = true;
            array[index]=result;
         }

         result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaLongRef ref,
        final LongRefTranlocal tranlocal,
        final boolean lock){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        final LongRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public  LongRefTranlocal openForRead(
        final BetaLongRef ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;
        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            LongRefTranlocal found = (LongRefTranlocal)array[index];


            if(found.isCommuting){
                flattenCommute(ref, found, lock);
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount,found)){
                throw abortOnReadConflict();
            }

            return found;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        final LongRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        if(lock || !read.isPermanent || config.trackReads){
            attach(ref, read, identityHashCode);
            size++;
        }else{
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public  LongRefTranlocal openForWrite(
        final BetaLongRef  ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            LongRefTranlocal result = (LongRefTranlocal)array[index];

            if(result.isCommuting){
                flattenCommute(ref, result, lock);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount,result)){
                throw abortOnReadConflict();
            }

            if(!result.isCommitted){
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            LongRefTranlocal read = result;
            result = pool.take(ref);
            if (result == null) {
                result = new LongRefTranlocal(ref);
            }

            result.value = read.value;
            result.read = read;
            hasUpdates = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final LongRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        //open the tranlocal for writing.
        LongRefTranlocal result = read.openForWrite(pool);
        hasUpdates = true;
        attach(ref, result, identityHashCode);
        size++;        
        return result;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final BetaLongRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            final LongRefTranlocal result = (LongRefTranlocal)array[index];
            if(result.isCommitted || result.read!=null){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        LongRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }
        result.isDirty = DIRTY_TRUE;
        attach(ref, result, identityHashCode);
        size++;
        return result;
    }

    public  void commute(
        final BetaLongRef ref, final LongFunction function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }    

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if(index == -1){
            //todo: call to 'openForCommute' can be inlined.
            LongRefTranlocal result = ref.___openForCommute(pool);
            attach(ref, result, identityHashCode);
            result.addCommutingFunction(function, pool);
            hasUpdates = true;
            size++;
            return;
        }

        LongRefTranlocal result = (LongRefTranlocal)array[index];
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final LongRefTranlocal read = result;
            result =  pool.take(ref);
            if(result == null){
                result = new LongRefTranlocal(ref);
            }
            result.read = read;
            result.value = read.value;
            hasUpdates = true;
            array[index]=result;
         }

         result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaTransactionalObject ref,
        final Tranlocal tranlocal,
        final boolean lock){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        final Tranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public  Tranlocal openForRead(
        final BetaTransactionalObject ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;
        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            Tranlocal found = (Tranlocal)array[index];


            if(found.isCommuting){
                flattenCommute(ref, found, lock);
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount,found)){
                throw abortOnReadConflict();
            }

            return found;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        final Tranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        if(lock || !read.isPermanent || config.trackReads){
            attach(ref, read, identityHashCode);
            size++;
        }else{
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public  Tranlocal openForWrite(
        final BetaTransactionalObject  ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            Tranlocal result = (Tranlocal)array[index];

            if(result.isCommuting){
                flattenCommute(ref, result, lock);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount,result)){
                throw abortOnReadConflict();
            }

            if(!result.isCommitted){
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            hasUpdates = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final Tranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        //open the tranlocal for writing.
        Tranlocal result = read.openForWrite(pool);
        hasUpdates = true;
        attach(ref, result, identityHashCode);
        size++;        
        return result;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            final Tranlocal result = (Tranlocal)array[index];
            if(result.isCommitted || result.read!=null){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        final Tranlocal result = ref.___openForConstruction(pool);
        result.isDirty = DIRTY_TRUE;
        attach(ref, result, identityHashCode);
        size++;
        return result;
    }

    public  void commute(
        final BetaTransactionalObject ref, final Function function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }    

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if(index == -1){
            //todo: call to 'openForCommute' can be inlined.
            Tranlocal result = ref.___openForCommute(pool);
            attach(ref, result, identityHashCode);
            result.addCommutingFunction(function, pool);
            hasUpdates = true;
            size++;
            return;
        }

        Tranlocal result = (Tranlocal)array[index];
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final Tranlocal read = result;
            result = read.openForWrite(pool);
            hasUpdates = true;
            array[index]=result;
         }

         throw new TodoException();
     }

 
    @Override
    public Tranlocal get(BetaTransactionalObject ref){
        final int indexOf = findAttachedIndex(ref, ref.___identityHashCode());
        if(indexOf == -1){
            return null;
        }

        return array[indexOf];
    }

    public int findAttachedIndex(final BetaTransactionalObject ref, final int hash){
        int jump = 0;
        boolean goLeft = true;

        do{
            final int offset = goLeft?-jump:jump;
            final int index = (hash + offset) % array.length;

            final Tranlocal current = array[index];
            if(current == null){
                return -1;
            }

            if(current.owner == ref){
                return index;
            }

            final int currentHash = current.owner.___identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0 ? 1 : jump*2;
        }while(jump < array.length);

        return -1;
    }

    private void attach(final BetaTransactionalObject ref, final Tranlocal tranlocal, final int hash){
        int jump = 0;
        boolean goLeft = true;

        do{
            final int offset = goLeft?-jump:jump;
            final int index = (hash + offset) % array.length;

            Tranlocal current = array[index];
            if(current == null){
                array[index] = tranlocal;
                return;
            }

            final int currentHash = current.owner.___identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0?1:jump*2;
        }while(jump < array.length);

        expand();
        attach(ref, tranlocal, hash);
    }

    private void expand(){
        Tranlocal[] oldArray = array;
        int newSize = oldArray.length*2;
        array = pool.takeTranlocalArray(newSize);
        if(array == null){
            array = new Tranlocal[newSize];
        }

        for(int k=0; k < oldArray.length; k++){
            final Tranlocal tranlocal = oldArray[k];

            if(tranlocal != null){
               attach(tranlocal.owner, tranlocal, tranlocal.owner.___identityHashCode());
            }
        }

        pool.putTranlocalArray(oldArray);
    }

    private boolean hasReadConflict() {
        if(config.lockReads) {
            return false;
        }

        if(hasUntrackedReads){
            return localConflictCounter.syncAndCheckConflict();
        }

        if(size == 0){
            return false;
        }

        if (!localConflictCounter.syncAndCheckConflict()) {
            return false;
        }

        for (int k = 0; k < array.length; k++) {
            final Tranlocal tranlocal = array[k];
            if (tranlocal != null && tranlocal.owner.___hasReadConflict(tranlocal, this)) {
                return true;
            }
        }

        return false;
    }

    // ============================= addWatch ===================================

    public void addWatch(BetaTransactionalObject object, Watch watch){
        throw new TodoException();
    }

    // ============================== abort ===================================================

    @Override
    public void abort() {
        switch (status) {
            case ACTIVE:
                //fall through
            case PREPARED:
                status = ABORTED;
                if(size>0){
                    for (int k = 0; k < array.length; k++) {
                        final Tranlocal tranlocal = array[k];
                        if(tranlocal != null){
                            tranlocal.owner.___abort(this, tranlocal, pool);
                        }
                    }
                }

                if(config.permanentListeners != null){
                    notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostAbort);
                }

                if(normalListeners != null){
                    notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);
                }
              break;
          case ABORTED:
              break;
            case COMMITTED:
                throw new DeadTransactionException(
                    format("[%s] Can't abort an already committed transaction",config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    // ============================== commit ==================================================

    @Override
    public void commit() {
        if(status == COMMITTED){
            return;
        }

        prepare();

        Listeners[] listenersArray = null;

        if(size>0){
            if(config.dirtyCheck){
                listenersArray = commitDirty();
            }else{
                listenersArray = commitAll();
            }
        }

        status = COMMITTED;

        if(listenersArray != null){
            Listeners.openAll(listenersArray, pool);
        }

        if(config.permanentListeners != null){
            notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostCommit);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostCommit);
        }
    }

    private Listeners[] commitAll() {
        Listeners[] listenersArray = null;

        int listenersArrayIndex = 0;
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];
            if(tranlocal != null){
                final Listeners listeners = tranlocal.owner.___commitAll(tranlocal, this, pool, config.globalConflictCounter);

                if(listeners != null){
                    if(listenersArray == null){
                        int length = array.length - k;
                        listenersArray = pool.takeListenersArray(length);
                        if(listenersArray == null){
                            listenersArray = new Listeners[length];
                        }
                    }
                    listenersArray[listenersArrayIndex]=listeners;
                    listenersArrayIndex++;
                }
            }
        }

        return listenersArray;
    }

    private Listeners[] commitDirty() {
        Listeners[] listenersArray = null;

        int listenersArrayIndex = 0;
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];
            if(tranlocal == null){
                continue;
            }

            if(tranlocal.isDirty == DIRTY_UNKNOWN){
                tranlocal.calculateIsDirty();
            }
            
            final Listeners listeners = tranlocal.owner.___commitDirty(tranlocal, this, pool, config.globalConflictCounter);

            if(listeners != null){
                if(listenersArray == null){
                    int length = array.length - k;
                    listenersArray = pool.takeListenersArray(length);
                    if(listenersArray == null){
                        listenersArray = new Listeners[length];
                    }
                }
                listenersArray[listenersArrayIndex]=listeners;
                listenersArrayIndex++;
            }
        }

        return listenersArray;
    }

    // ============================== prepare ==================================================

    @Override
    public void prepare() {
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                     return;
                case ABORTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't prepare already aborted transaction",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't prepare already committed transaction",config.familyName));
                default:
                    throw new IllegalStateException();

            }
        }

        boolean abort = true;
        try{
            if(config.permanentListeners != null){
                notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PrePrepare);
            }

            if(normalListeners != null){
                notifyListeners(normalListeners, TransactionLifecycleEvent.PrePrepare);
            }

            if(abortOnly){
                throw abortOnWriteConflict();
            }

            if(hasUpdates){
                if(!config.writeSkewAllowed){
                    if(!doPrepareWithWriteSkewPrevention()){
                        throw abortOnWriteConflict();
                    }
                } else if(config.dirtyCheck){
                    if(!doPrepareDirty()){
                        throw abortOnWriteConflict();
                    }
                }else{
                    if(!doPrepareAll()){
                        throw abortOnWriteConflict();
                    }
                }
            }

            status = PREPARED;
            abort = false;
        }finally{
            if(abort){
                abort();
            }
        }
    }

     private boolean doPrepareWithWriteSkewPrevention() {
        if(config.lockReads){
            return true;
        }

        final int spinCount = config.spinCount;
        final boolean dirtyCheck = config.dirtyCheck;
        for (int k = 0; k < array.length; k++) {
            final Tranlocal tranlocal = array[k];

            if(tranlocal == null){
                continue;
            }else if(tranlocal.isCommitted){
                if(!tranlocal.owner.___tryLockAndCheckConflict(this, config.spinCount, tranlocal)){
                    return false;
                }
            }else if(tranlocal.isCommuting){
                final Tranlocal read = tranlocal.owner.___lockAndLoad(spinCount, this);

                if(read.isLocked){
                    return false;
                }

                tranlocal.read = read;
                tranlocal.evaluateCommutingFunctions(pool);
            }else{
                if(dirtyCheck){
                    tranlocal.calculateIsDirty();
                }

                if(!tranlocal.owner.___tryLockAndCheckConflict(this, spinCount, tranlocal)){
                    return false;
                }
            }
        }

        return true;
    }

    private boolean doPrepareAll() {
        if(config.lockWrites){
            return true;
        }

        final int spinCount = config.spinCount;

        for (int k = 0; k < array.length; k++) {
            final Tranlocal tranlocal = array[k];

            if (tranlocal==null || tranlocal.isCommitted){
                continue;
            }

            if(tranlocal.isCommuting){
                final Tranlocal read = tranlocal.owner.___lockAndLoad(spinCount, this);

                if(read.isLocked){
                    return false;
                }

                tranlocal.read = read;
                tranlocal.evaluateCommutingFunctions(pool);
            }else
            if(!tranlocal.owner.___tryLockAndCheckConflict(this, spinCount, tranlocal)){
               return false;
            }
        }

        return true;
    }

    private boolean doPrepareDirty() {
        if(config.lockWrites){
            return true;
        }

        final int spinCount = config.spinCount;

        for (int k = 0; k < array.length; k++) {
            final Tranlocal tranlocal = array[k];

            if(tranlocal == null || tranlocal.isCommitted){
                continue;
            }

            if(tranlocal.isCommuting){
                final Tranlocal read = tranlocal.owner.___lockAndLoad(spinCount, this);

                if(read.isLocked){
                    return false;
                }

                tranlocal.read = read;
                tranlocal.evaluateCommutingFunctions(pool);
            }else
            if (tranlocal.calculateIsDirty()) {
                if(!tranlocal.owner.___tryLockAndCheckConflict(this, spinCount, tranlocal)){
                    return false;
                }
            }
        }

        return true;
    }

    // ============================ registerChangeListener ===============================

    @Override
    public void registerChangeListenerAndAbort(final Latch listener) {
        if (status != ACTIVE) {
            throw abortOnFaultyStatusOfRegisterChangeListenerAndAbort();
        }

        if(!config.blockingAllowed){
            throw abortOnNoBlockingAllowed();
        }

        if( size == 0){
            throw abortOnNoRetryPossible();
        }

        final long listenerEra = listener.getEra();
        boolean furtherRegistrationNeeded = true;
        boolean atLeastOneRegistration = false;
        if(size>0){
            for(int k=0; k < array.length; k++){
                final Tranlocal tranlocal = array[k];

                if(tranlocal != null){
                    final BetaTransactionalObject owner = tranlocal.owner;

                    if(furtherRegistrationNeeded){
                        switch(owner.___registerChangeListener(listener, tranlocal, pool, listenerEra)){
                            case REGISTRATION_DONE:
                                atLeastOneRegistration = true;
                                break;
                            case REGISTRATION_NOT_NEEDED:
                                furtherRegistrationNeeded = false;
                                atLeastOneRegistration = true;
                                break;
                            case REGISTRATION_NONE:
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                    }

                    owner.___abort(this, tranlocal, pool);
                }
            }
        }

        status = ABORTED;
        if(config.permanentListeners != null){
            notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostAbort);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);
        }

        if(!atLeastOneRegistration){
            throw abortOnNoRetryPossible();
        }
    }

    // ============================== reset ========================================

    @Override
    public boolean softReset() {
        if (status == ACTIVE || status == PREPARED) {
            abort();
        }

        if(attempt>=config.getMaxRetries()){
            return false;
        }

        if(array.length>config.minimalArrayTreeSize){
            pool.putTranlocalArray(array);
            array = pool.takeTranlocalArray(config.minimalArrayTreeSize);
            if(array == null){
                array = new Tranlocal[config.minimalArrayTreeSize];
            }
        }else{
            Arrays.fill(array, null);
        }

        status = ACTIVE;
        abortOnly = false;
        hasReads = false;
        hasUpdates = false;
        hasUntrackedReads = false;
        size = 0;
        attempt++;
        evaluatingCommute = false;
        if(normalListeners!=null){
            normalListeners.clear();
        }
        return true;
    }

    @Override
    public void hardReset(){
        if (status == ACTIVE || status == PREPARED) {
            abort();
        }

        if(array.length>config.minimalArrayTreeSize){
            pool.putTranlocalArray(array);
            array = pool.takeTranlocalArray(config.minimalArrayTreeSize);
            if(array == null){
                array = new Tranlocal[config.minimalArrayTreeSize];
            }
        }else{
            Arrays.fill(array, null);
        }

        status = ACTIVE;
        abortOnly = false;
        hasUpdates = false;
        hasReads = false;
        hasUntrackedReads = false;
        attempt = 1;
        remainingTimeoutNs = config.timeoutNs;
        size = 0;
        evaluatingCommute = false;
        if(normalListeners !=null){
            pool.putArrayList(normalListeners);
            normalListeners = null;
        }
    }

    // ============================== init =======================================

    @Override
    public void init(BetaTransactionConfiguration transactionConfig){
        if(transactionConfig == null){
            abort();
            throw new NullPointerException();
        }

        if(status == ACTIVE || status == PREPARED){
            abort();
        }

        this.config = transactionConfig;
        hardReset();
    }

    // ================== orelse ============================

    // ================== orelse ============================

    @Override
    public final void startEitherBranch(){
        throw new TodoException();
    }

    @Override
    public final void endEitherBranch(){
        throw new TodoException();
    }

    @Override
    public final void startOrElseBranch(){
        throw new TodoException();
    }
}
