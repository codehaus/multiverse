package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Watch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.blocking.Listeners;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.functions.Function;
import org.multiverse.functions.IntFunction;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.conflictcounters.LocalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.*;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public final class FatArrayBetaTransaction extends AbstractFatBetaTransaction {

    public final static AtomicLong conflictScan = new AtomicLong();

    private final LocalConflictCounter localConflictCounter;
    private final Tranlocal[] array;
    private int firstFreeIndex = 0;
    private boolean hasReads;
    private boolean hasUntrackedReads;

    public FatArrayBetaTransaction(final BetaStm stm) {
        this(new BetaTransactionConfiguration(stm));
    }

    public FatArrayBetaTransaction(final BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_FAT_ARRAY, config);
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new Tranlocal[config.maxArrayTransactionSize];
        this.remainingTimeoutNs = config.timeoutNs;
    }

    public final LocalConflictCounter getLocalConflictCounter() {
        return localConflictCounter;
    }

    @Override
    public void start(){
        start(getThreadLocalBetaObjectPool());
    }

    @Override
    public void start(final BetaObjectPool pool){
        if(status != NEW){
            switch(status){
                case ACTIVE:
                    //it can't do harm to start an already started transaction
                    return;
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                        format("Can't start already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't start already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't start already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Override
    public <E> RefTranlocal<E> openForRead(
        final Ref<E> ref, boolean lock, final BetaObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            throw abortOpenForRead(pool);
        }

        //a read on a null ref, always return a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if(index > -1){
            //we are lucky, at already is attached to the session
            RefTranlocal<E> found = (RefTranlocal<E>)array[index];

            if(found.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final RefTranlocal<E> read = lock
                    ? ref.___lockAndLoad(config.spinCount, this)
                    : ref.___load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                //make sure that there are no conflicts.
                if (hasReadConflict()) {
                    ref.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                found.read = read;
                found.evaluateCommutingFunctions(pool);
            }else
            if (lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, found)){
                throw abortOnReadConflict(pool);
            }

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[index] = found;
            }

            return found;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        RefTranlocal<E> read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if( lock || config.trackReads || !read.isPermanent){
            array[firstFreeIndex] = read;
            firstFreeIndex++;
        }else{
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public <E> RefTranlocal<E> openForWrite(
        final Ref<E>  ref, boolean lock, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
           throw abortOpenForWrite(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if(index != -1){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];

            if(result.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final RefTranlocal<E> read = lock
                    ? ref.___lockAndLoad(config.spinCount, this)
                    : ref.___load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if (hasReadConflict()) {
                    ref.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                result.read = read;
                result.evaluateCommutingFunctions(pool);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }else if(!result.isCommitted){
                return result;
            }

            result = result.openForWrite(pool);
            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            //if (index > 0) {
            //    array[index] = array[0];
            //    array[index] = result;
            //}

            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        //only if the size currently is 0, we are going to initialize the localConflictCounter,
        //not before. So the localConflictCounter is set at the latest moment possible. It is
        //very important that this is done before the actual reading since we don't want to loose
        //a conflict.
        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final RefTranlocal<E> read = lock
            ? ref.___lockAndLoad(config.spinCount, this) 
            : ref.___load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        RefTranlocal<E>  result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }

        result.read = read;
        result.value = read.value;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final Ref<E> ref, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
          throw abortOpenForConstruction(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];

            if(result.isCommitted || result.read!= null){
                abort();
                throw new IllegalArgumentException(
                    format("Can't open a previous committed object of class '%s' for construction on transaction '%s'",
                        config.familyName, ref.getClass().getName()));

            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___unsafeLoad() != null){
            abort();
            throw new IllegalArgumentException(
                format("Can't open for construction a previous committed object on transaction '%s'",config.familyName));
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        //open the tranlocal for writing.
        RefTranlocal<E> result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public <E> void commute(
        final Ref<E> ref, final BetaObjectPool pool, Function<E> function){

        if (status != ACTIVE) {
            throw abortCommute(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        final int index = indexOf(ref);
        if(index == -1){
            if(firstFreeIndex == array.length) {
                throw abortOnTooSmallSize(pool, array.length+1);
            }

            //todo: call to 'openForCommute' can be inlined.
            RefTranlocal<E> result = ref.___openForCommute(pool);
            array[firstFreeIndex]=result;
            result.addCommutingFunction(function, pool);
            firstFreeIndex++;
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
            array[index]=result;
        }

        result.value = function.call(result.value);
      }


    @Override
    public  IntRefTranlocal openForRead(
        final IntRef ref, boolean lock, final BetaObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            throw abortOpenForRead(pool);
        }

        //a read on a null ref, always return a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if(index > -1){
            //we are lucky, at already is attached to the session
            IntRefTranlocal found = (IntRefTranlocal)array[index];

            if(found.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final IntRefTranlocal read = lock
                    ? ref.___lockAndLoad(config.spinCount, this)
                    : ref.___load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                //make sure that there are no conflicts.
                if (hasReadConflict()) {
                    ref.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                found.read = read;
                found.evaluateCommutingFunctions(pool);
            }else
            if (lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, found)){
                throw abortOnReadConflict(pool);
            }

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[index] = found;
            }

            return found;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        IntRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if( lock || config.trackReads || !read.isPermanent){
            array[firstFreeIndex] = read;
            firstFreeIndex++;
        }else{
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public  IntRefTranlocal openForWrite(
        final IntRef  ref, boolean lock, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
           throw abortOpenForWrite(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if(index != -1){
            IntRefTranlocal result = (IntRefTranlocal)array[index];

            if(result.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final IntRefTranlocal read = lock
                    ? ref.___lockAndLoad(config.spinCount, this)
                    : ref.___load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if (hasReadConflict()) {
                    ref.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                result.read = read;
                result.evaluateCommutingFunctions(pool);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }else if(!result.isCommitted){
                return result;
            }

            result = result.openForWrite(pool);
            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            //if (index > 0) {
            //    array[index] = array[0];
            //    array[index] = result;
            //}

            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        //only if the size currently is 0, we are going to initialize the localConflictCounter,
        //not before. So the localConflictCounter is set at the latest moment possible. It is
        //very important that this is done before the actual reading since we don't want to loose
        //a conflict.
        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final IntRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this) 
            : ref.___load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        IntRefTranlocal  result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final IntRef ref, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
          throw abortOpenForConstruction(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            IntRefTranlocal result = (IntRefTranlocal)array[index];

            if(result.isCommitted || result.read!= null){
                abort();
                throw new IllegalArgumentException(
                    format("Can't open a previous committed object of class '%s' for construction on transaction '%s'",
                        config.familyName, ref.getClass().getName()));

            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___unsafeLoad() != null){
            abort();
            throw new IllegalArgumentException(
                format("Can't open for construction a previous committed object on transaction '%s'",config.familyName));
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        //open the tranlocal for writing.
        IntRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public  void commute(
        final IntRef ref, final BetaObjectPool pool, IntFunction function){

        if (status != ACTIVE) {
            throw abortCommute(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        final int index = indexOf(ref);
        if(index == -1){
            if(firstFreeIndex == array.length) {
                throw abortOnTooSmallSize(pool, array.length+1);
            }

            //todo: call to 'openForCommute' can be inlined.
            IntRefTranlocal result = ref.___openForCommute(pool);
            array[firstFreeIndex]=result;
            result.addCommutingFunction(function, pool);
            firstFreeIndex++;
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
            array[index]=result;
        }

        result.value = function.call(result.value);
      }


    @Override
    public  LongRefTranlocal openForRead(
        final LongRef ref, boolean lock, final BetaObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            throw abortOpenForRead(pool);
        }

        //a read on a null ref, always return a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if(index > -1){
            //we are lucky, at already is attached to the session
            LongRefTranlocal found = (LongRefTranlocal)array[index];

            if(found.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final LongRefTranlocal read = lock
                    ? ref.___lockAndLoad(config.spinCount, this)
                    : ref.___load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                //make sure that there are no conflicts.
                if (hasReadConflict()) {
                    ref.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                found.read = read;
                found.evaluateCommutingFunctions(pool);
            }else
            if (lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, found)){
                throw abortOnReadConflict(pool);
            }

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[index] = found;
            }

            return found;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        LongRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if( lock || config.trackReads || !read.isPermanent){
            array[firstFreeIndex] = read;
            firstFreeIndex++;
        }else{
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public  LongRefTranlocal openForWrite(
        final LongRef  ref, boolean lock, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
           throw abortOpenForWrite(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if(index != -1){
            LongRefTranlocal result = (LongRefTranlocal)array[index];

            if(result.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final LongRefTranlocal read = lock
                    ? ref.___lockAndLoad(config.spinCount, this)
                    : ref.___load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if (hasReadConflict()) {
                    ref.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                result.read = read;
                result.evaluateCommutingFunctions(pool);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }else if(!result.isCommitted){
                return result;
            }

            result = result.openForWrite(pool);
            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            //if (index > 0) {
            //    array[index] = array[0];
            //    array[index] = result;
            //}

            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        //only if the size currently is 0, we are going to initialize the localConflictCounter,
        //not before. So the localConflictCounter is set at the latest moment possible. It is
        //very important that this is done before the actual reading since we don't want to loose
        //a conflict.
        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final LongRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this) 
            : ref.___load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        LongRefTranlocal  result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final LongRef ref, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
          throw abortOpenForConstruction(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            LongRefTranlocal result = (LongRefTranlocal)array[index];

            if(result.isCommitted || result.read!= null){
                abort();
                throw new IllegalArgumentException(
                    format("Can't open a previous committed object of class '%s' for construction on transaction '%s'",
                        config.familyName, ref.getClass().getName()));

            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___unsafeLoad() != null){
            abort();
            throw new IllegalArgumentException(
                format("Can't open for construction a previous committed object on transaction '%s'",config.familyName));
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        //open the tranlocal for writing.
        LongRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public  void commute(
        final LongRef ref, final BetaObjectPool pool, LongFunction function){

        if (status != ACTIVE) {
            throw abortCommute(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        final int index = indexOf(ref);
        if(index == -1){
            if(firstFreeIndex == array.length) {
                throw abortOnTooSmallSize(pool, array.length+1);
            }

            //todo: call to 'openForCommute' can be inlined.
            LongRefTranlocal result = ref.___openForCommute(pool);
            array[firstFreeIndex]=result;
            result.addCommutingFunction(function, pool);
            firstFreeIndex++;
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
            array[index]=result;
        }

        result.value = function.call(result.value);
      }


    @Override
    public  Tranlocal openForRead(
        final BetaTransactionalObject ref, boolean lock, final BetaObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            throw abortOpenForRead(pool);
        }

        //a read on a null ref, always return a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if(index > -1){
            //we are lucky, at already is attached to the session
            Tranlocal found = (Tranlocal)array[index];

            if(found.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final Tranlocal read = lock
                    ? ref.___lockAndLoad(config.spinCount, this)
                    : ref.___load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                //make sure that there are no conflicts.
                if (hasReadConflict()) {
                    ref.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                found.read = read;
                found.evaluateCommutingFunctions(pool);
            }else
            if (lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, found)){
                throw abortOnReadConflict(pool);
            }

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[index] = found;
            }

            return found;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        Tranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if( lock || config.trackReads || !read.isPermanent){
            array[firstFreeIndex] = read;
            firstFreeIndex++;
        }else{
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public  Tranlocal openForWrite(
        final BetaTransactionalObject  ref, boolean lock, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
           throw abortOpenForWrite(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if(index != -1){
            Tranlocal result = (Tranlocal)array[index];

            if(result.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final Tranlocal read = lock
                    ? ref.___lockAndLoad(config.spinCount, this)
                    : ref.___load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if (hasReadConflict()) {
                    ref.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                result.read = read;
                result.evaluateCommutingFunctions(pool);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }else if(!result.isCommitted){
                return result;
            }

            result = result.openForWrite(pool);
            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            //if (index > 0) {
            //    array[index] = array[0];
            //    array[index] = result;
            //}

            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        //only if the size currently is 0, we are going to initialize the localConflictCounter,
        //not before. So the localConflictCounter is set at the latest moment possible. It is
        //very important that this is done before the actual reading since we don't want to loose
        //a conflict.
        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final Tranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this) 
            : ref.___load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        Tranlocal  result = read.openForWrite(pool);
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
          throw abortOpenForConstruction(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            Tranlocal result = (Tranlocal)array[index];

            if(result.isCommitted || result.read!= null){
                abort();
                throw new IllegalArgumentException(
                    format("Can't open a previous committed object of class '%s' for construction on transaction '%s'",
                        config.familyName, ref.getClass().getName()));

            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___unsafeLoad() != null){
            abort();
            throw new IllegalArgumentException(
                format("Can't open for construction a previous committed object on transaction '%s'",config.familyName));
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length+1);
        }

        //open the tranlocal for writing.
        Tranlocal result = ref.___openForConstruction(pool);
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public  void commute(
        final BetaTransactionalObject ref, final BetaObjectPool pool, Function function){

        if (status != ACTIVE) {
            throw abortCommute(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        final int index = indexOf(ref);
        if(index == -1){
            if(firstFreeIndex == array.length) {
                throw abortOnTooSmallSize(pool, array.length+1);
            }

            //todo: call to 'openForCommute' can be inlined.
            Tranlocal result = ref.___openForCommute(pool);
            array[firstFreeIndex]=result;
            result.addCommutingFunction(function, pool);
            firstFreeIndex++;
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
            array[index]=result;
        }

        throw new TodoException();
      }

 
    @Override
    public Tranlocal get(BetaTransactionalObject owner){
        int indexOf = indexOf(owner);
        return indexOf == -1 ? null: array[indexOf];
    }

    /**
     * Finds the index of the tranlocal that has the ref as owner. Return -1 if not found.
     *
     * @param owner the owner of the tranlocal to look for.
     * @return the index of the tranlocal, or -1 if not found.
     */
    private int indexOf(BetaTransactionalObject owner){
        assert owner!=null;

        for(int k=0; k < firstFreeIndex; k++){
            Tranlocal tranlocal = array[k];
            if(tranlocal.owner == owner){
                return k;
            }
        }

        return -1;
    }

    private boolean hasReadConflict() {
        if (config.lockReads) {
            return false;
        }

        if(hasUntrackedReads){
            return localConflictCounter.syncAndCheckConflict();
        }

        if(firstFreeIndex == 0){
            return false;
        }

        if (!localConflictCounter.syncAndCheckConflict()) {
            return false;
        }

        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];

            if (tranlocal.owner.___hasReadConflict(tranlocal, this)) {
                return true;
            }
        }

        return false;
    }

    // ============================= addWatch ===================================

    public void addWatch(BetaTransactionalObject object, Watch watch){
        throw new TodoException();
    }

    // ============================== abort ==================================

    @Override
    public void abort() {
        abort(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void abort(final BetaObjectPool pool) {
        switch (status) {
            case ACTIVE:
                //fall through
            case PREPARED:
                status = ABORTED;                
                for (int k = 0; k < firstFreeIndex; k++) {
                    Tranlocal tranlocal = array[k];
                    tranlocal.owner.___abort(this, tranlocal, pool);
                }
                if(permanentListeners != null){
                    notifyListeners(permanentListeners, TransactionLifecycleEvent.PostAbort);
                }

                if(normalListeners != null){
                    notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);
                }
                break;
            case ABORTED:
                break;
            case COMMITTED:
                throw new DeadTransactionException(
                    format("Can't abort already committed transaction '%s'",config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    // ================================== commit =================================

    @Override
    public void commit() {
         commit(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void commit(final BetaObjectPool pool) {
        if(status == COMMITTED){
            return;
        }

        prepare();
    
        Listeners[] listeners = null;

        if (firstFreeIndex > 0) {
            if(config.dirtyCheck){
                listeners = commitDirty(pool);
            }else{
                listeners = commitAll(pool);
            }
        }

        status = COMMITTED;

        if(listeners != null){
            Listeners.openAll(listeners, pool);
        }

        if(permanentListeners != null){
            notifyListeners(permanentListeners, TransactionLifecycleEvent.PostCommit);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostCommit);
        }
    }

    private Listeners[] commitAll(final BetaObjectPool pool) {
        Listeners[] listenersArray = null;

        int storeIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];
            Listeners listeners = tranlocal.owner.___commitAll(tranlocal, this, pool, config.globalConflictCounter);

            if(listeners != null){
                if(listenersArray == null){
                    int length = firstFreeIndex - k;
                    listenersArray = pool.takeListenersArray(length);
                    if(listenersArray == null){
                        listenersArray = new Listeners[length];
                    }
                }
                listenersArray[storeIndex]=listeners;
                storeIndex++;
            }
        }

        return listenersArray;
    }

    private Listeners[] commitDirty(final BetaObjectPool pool) {
        Listeners[] listenersArray = null;

        int storeIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];
            Listeners listeners = tranlocal.owner.___commitDirty(tranlocal, this, pool, config.globalConflictCounter);

            if(listeners != null){
                if(listenersArray == null){
                    int length = firstFreeIndex - k;
                    listenersArray = pool.takeListenersArray(length);
                    if(listenersArray == null){
                        listenersArray = new Listeners[length];
                    }
                }
                listenersArray[storeIndex]=listeners;
                storeIndex++;
            }
        }

        return listenersArray;
    }

    // ========================= prepare ================================

    @Override
    public void prepare() {
        prepare(getThreadLocalBetaObjectPool());
    }

    @Override
    public void prepare(BetaObjectPool pool) {
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                     //won't harm to call it more than once.
                     return;
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't prepare already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't prepare already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        boolean abort = true;
        try{
            if(permanentListeners != null){
                notifyListeners(permanentListeners, TransactionLifecycleEvent.PrePrepare);
            }

            if(normalListeners != null){
                notifyListeners(normalListeners, TransactionLifecycleEvent.PrePrepare);
            }

            if(abortOnly){
                throw abortOnWriteConflict(pool);
            }
             
            if(firstFreeIndex > 0){
                if(config.dirtyCheck){
                    if(!doPrepareDirty(pool)){
                        throw abortOnWriteConflict(pool);
                    }
                }else{
                    if(!doPrepareAll(pool)){
                        throw abortOnWriteConflict(pool);
                    }
                }
            }

            status = PREPARED;
            abort = false;
        }finally{
            if(abort){
                abort(pool);
            }
        }
    }

    private boolean doPrepareAll(final BetaObjectPool pool) {
        if(config.lockWrites){
            return true;
        }

        int spinCount = config.spinCount;

        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];

            if(tranlocal.isCommitted){
                continue;
            }

            if(tranlocal.isCommuting){
                Tranlocal read = tranlocal.owner.___lockAndLoad(spinCount, this);

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

    private boolean doPrepareDirty(final BetaObjectPool pool) {
        if(config.lockWrites){
            return true;
        }

        int spinCount = config.spinCount;

        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];

            if(tranlocal.isCommitted){
                continue;
            }


            if(tranlocal.isCommuting){
                Tranlocal read = tranlocal.owner.___lockAndLoad(spinCount, this);

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

    // ============================== registerChangeListenerAndAbort ========================

    @Override
    public void registerChangeListenerAndAbort(final Latch listener){
        registerChangeListenerAndAbort(listener, getThreadLocalBetaObjectPool());
    }

    @Override
    public void registerChangeListenerAndAbort(final Latch listener, final BetaObjectPool pool) {
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                        format("Can't block on already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't block on already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't block on already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if(!config.blockingAllowed){
            abort();
            throw new NoRetryPossibleException(
                format("Can't block transaction '%s', since it explicitly is configured as non blockable",config.familyName));
        }

        if( firstFreeIndex == 0){
            abort();
            throw new NoRetryPossibleException(
                format("Can't block transaction '%s', since there are no tracked reads",config.familyName));
        }

        final long listenerEra = listener.getEra();

        boolean furtherRegistrationNeeded = true;
        boolean atLeastOneRegistration = false;

        for(int k=0; k < firstFreeIndex; k++){

            final Tranlocal tranlocal = array[k];
            final BetaTransactionalObject owner = tranlocal.owner;

            if(furtherRegistrationNeeded){
                switch(owner.___registerChangeListener(listener, tranlocal, pool, listenerEra)){
                    case BetaTransactionalObject.REGISTRATION_DONE:
                        atLeastOneRegistration = true;
                        break;
                    case BetaTransactionalObject.REGISTRATION_NOT_NEEDED:
                        furtherRegistrationNeeded = false;
                        atLeastOneRegistration = true;
                        break;
                    case BetaTransactionalObject.REGISTRATION_NONE:
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            owner.___abort(this, tranlocal, pool);
        }

        status = ABORTED;
        if(permanentListeners != null){
            notifyListeners(permanentListeners, TransactionLifecycleEvent.PostAbort);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);
        }

        if(!atLeastOneRegistration){
            throw new NoRetryPossibleException(
                format("Can't block transaction '%s', since there are no tracked reads",config.familyName));
        }
    }

    // ==================== reset ==============================

    @Override
    public boolean softReset(){
        return softReset(getThreadLocalBetaObjectPool());
    }

    @Override
    public final boolean softReset(final BetaObjectPool pool) {
        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
        }

        if(attempt>=config.getMaxRetries()){
            return false;
        }

        status = ACTIVE;
        abortOnly = false;
        attempt++;
        firstFreeIndex = 0;
        hasReads = false;
        hasUntrackedReads = false;
        if(normalListeners != null){
            normalListeners.clear();
        }
        return true;
    }

    @Override
    public void hardReset(){
        hardReset(getThreadLocalBetaObjectPool());
    }

    @Override
    public void hardReset(final BetaObjectPool pool){
        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
        }
        status = ACTIVE;
        abortOnly = false;
        hasReads = false;
        hasUntrackedReads = false;
        attempt=1;
        firstFreeIndex = 0;
        remainingTimeoutNs = config.timeoutNs;        
        if(normalListeners !=null){
            pool.putArrayList(normalListeners);
            normalListeners = null;
        }

        if(permanentListeners!=null){
            pool.putArrayList(permanentListeners);
            permanentListeners = null;
        }
    }

    // ==================== init =============================

    @Override
    public void init(BetaTransactionConfiguration transactionConfig){
        init(transactionConfig, getThreadLocalBetaObjectPool());
    }

    @Override
    public void init(BetaTransactionConfiguration transactionConfig, BetaObjectPool pool){
        if(transactionConfig == null){
            abort();
            throw new NullPointerException();
        }

        if(status == ACTIVE || status == PREPARED){
            abort(pool);
        }

        config = transactionConfig;
        hardReset(pool);
    }

    // ================== orelse ============================

    @Override
    public final void startEitherBranch(){
        startEitherBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void startEitherBranch(BetaObjectPool pool){
        throw new TodoException();
    }

    @Override
    public final void endEitherBranch(){
        endEitherBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void endEitherBranch(BetaObjectPool pool){
        throw new TodoException();
    }

    @Override
    public final void startOrElseBranch(){
        startOrElseBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void startOrElseBranch(BetaObjectPool pool){
        throw new TodoException();
    }
}