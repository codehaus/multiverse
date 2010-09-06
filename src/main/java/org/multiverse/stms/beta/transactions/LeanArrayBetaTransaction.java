package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Watch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.DoubleFunction;
import org.multiverse.api.functions.Function;
import org.multiverse.api.functions.IntFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.conflictcounters.LocalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.*;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

public final class LeanArrayBetaTransaction extends AbstractLeanBetaTransaction {

    public final static AtomicLong conflictScan = new AtomicLong();

    private final Tranlocal[] array;
    private LocalConflictCounter localConflictCounter;    
    private int firstFreeIndex = 0;
    private boolean hasReads;
    private boolean hasUpdates;
    private boolean hasUntrackedReads;

    public LeanArrayBetaTransaction(final BetaStm stm) {
        this(new BetaTransactionConfiguration(stm).init());
    }

    public LeanArrayBetaTransaction(final BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_LEAN_ARRAY, config);
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new Tranlocal[config.maxArrayTransactionSize];
        this.remainingTimeoutNs = config.timeoutNs;
    }

    public final LocalConflictCounter getLocalConflictCounter() {
        return localConflictCounter;
    }


    @Override
    public <E> RefTranlocal<E> openForRead(
        final BetaRef<E> ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            RefTranlocal<E> found = (RefTranlocal<E>)array[index];

            if (lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, found)){
                throw abortOnReadConflict();
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
            throw abortOnTooSmallSize(array.length+1);
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
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
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
        final BetaRef<E>  ref, boolean lock) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lock = lock || config.lockWrites;
        final int index = indexOf(ref);
        if(index != -1){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict();
            }else if(!result.isCommitted){
                return result;
            }

            result = result.openForWrite(pool);
            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            //if (index > 0) {
            //    array[index] = array[0];
            //    array[index] = result;
            //}
            hasUpdates = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

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
        RefTranlocal<E>  result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }

        result.read = read;
        result.value = read.value;
        hasUpdates = true;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final BetaRef<E> ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];

            if(result.isCommitted || result.read!= null){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___unsafeLoad() != null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        RefTranlocal<E> result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }
        result.isDirty = DIRTY_TRUE;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public <E> void commute(
        final BetaRef<E> ref, final Function<E> function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }

        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
      }


    @Override
    public  IntRefTranlocal openForRead(
        final BetaIntRef ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            IntRefTranlocal found = (IntRefTranlocal)array[index];

            if (lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, found)){
                throw abortOnReadConflict();
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
            throw abortOnTooSmallSize(array.length+1);
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
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
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
        final BetaIntRef  ref, boolean lock) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lock = lock || config.lockWrites;
        final int index = indexOf(ref);
        if(index != -1){
            IntRefTranlocal result = (IntRefTranlocal)array[index];
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict();
            }else if(!result.isCommitted){
                return result;
            }

            result = result.openForWrite(pool);
            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            //if (index > 0) {
            //    array[index] = array[0];
            //    array[index] = result;
            //}
            hasUpdates = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

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
        IntRefTranlocal  result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        hasUpdates = true;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final BetaIntRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            IntRefTranlocal result = (IntRefTranlocal)array[index];

            if(result.isCommitted || result.read!= null){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___unsafeLoad() != null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        IntRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }
        result.isDirty = DIRTY_TRUE;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public  void commute(
        final BetaIntRef ref, final IntFunction function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }

        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
      }


    @Override
    public  DoubleRefTranlocal openForRead(
        final BetaDoubleRef ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            DoubleRefTranlocal found = (DoubleRefTranlocal)array[index];

            if (lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, found)){
                throw abortOnReadConflict();
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
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        DoubleRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
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
    public  DoubleRefTranlocal openForWrite(
        final BetaDoubleRef  ref, boolean lock) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lock = lock || config.lockWrites;
        final int index = indexOf(ref);
        if(index != -1){
            DoubleRefTranlocal result = (DoubleRefTranlocal)array[index];
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict();
            }else if(!result.isCommitted){
                return result;
            }

            result = result.openForWrite(pool);
            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            //if (index > 0) {
            //    array[index] = array[0];
            //    array[index] = result;
            //}
            hasUpdates = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final DoubleRefTranlocal read = lock
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
        DoubleRefTranlocal  result =  pool.take(ref);
        if(result == null){
            result = new DoubleRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        hasUpdates = true;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  DoubleRefTranlocal openForConstruction(
        final BetaDoubleRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            DoubleRefTranlocal result = (DoubleRefTranlocal)array[index];

            if(result.isCommitted || result.read!= null){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___unsafeLoad() != null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        DoubleRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new DoubleRefTranlocal(ref);
        }
        result.isDirty = DIRTY_TRUE;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public  void commute(
        final BetaDoubleRef ref, final DoubleFunction function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }

        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
      }


    @Override
    public  LongRefTranlocal openForRead(
        final BetaLongRef ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            LongRefTranlocal found = (LongRefTranlocal)array[index];

            if (lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, found)){
                throw abortOnReadConflict();
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
            throw abortOnTooSmallSize(array.length+1);
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
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
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
        final BetaLongRef  ref, boolean lock) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lock = lock || config.lockWrites;
        final int index = indexOf(ref);
        if(index != -1){
            LongRefTranlocal result = (LongRefTranlocal)array[index];
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict();
            }else if(!result.isCommitted){
                return result;
            }

            result = result.openForWrite(pool);
            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            //if (index > 0) {
            //    array[index] = array[0];
            //    array[index] = result;
            //}
            hasUpdates = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

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
        LongRefTranlocal  result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        hasUpdates = true;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final BetaLongRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            LongRefTranlocal result = (LongRefTranlocal)array[index];

            if(result.isCommitted || result.read!= null){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___unsafeLoad() != null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        LongRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }
        result.isDirty = DIRTY_TRUE;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public  void commute(
        final BetaLongRef ref, final LongFunction function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }

        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
      }


    @Override
    public  Tranlocal openForRead(
        final BetaTransactionalObject ref, boolean lock) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            Tranlocal found = (Tranlocal)array[index];

            if (lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, found)){
                throw abortOnReadConflict();
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
            throw abortOnTooSmallSize(array.length+1);
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
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
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
        final BetaTransactionalObject  ref, boolean lock) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lock = lock || config.lockWrites;
        final int index = indexOf(ref);
        if(index != -1){
            Tranlocal result = (Tranlocal)array[index];
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict();
            }else if(!result.isCommitted){
                return result;
            }

            result = result.openForWrite(pool);
            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            //if (index > 0) {
            //    array[index] = array[0];
            //    array[index] = result;
            //}
            hasUpdates = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

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
        Tranlocal  result = read.openForWrite(pool);
        hasUpdates = true;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            Tranlocal result = (Tranlocal)array[index];

            if(result.isCommitted || result.read!= null){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___unsafeLoad() != null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        Tranlocal result = ref.___openForConstruction(pool);
        result.isDirty = DIRTY_TRUE;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public  void commute(
        final BetaTransactionalObject ref, final Function function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }

        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
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
            final Tranlocal tranlocal = array[k];
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
            final Tranlocal tranlocal = array[k];

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
        switch (status) {
            case ACTIVE:
                //fall through
            case PREPARED:
                status = ABORTED;                
                for (int k = 0; k < firstFreeIndex; k++) {
                    final Tranlocal tranlocal = array[k];
                    tranlocal.owner.___abort(this, tranlocal, pool);
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

    // ================================== commit =================================

    @Override
    public final void commit() {
        if (status != ACTIVE && status != PREPARED) {
            switch (status) {
                case ABORTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't commit an already aborted transaction",config.familyName));
                case COMMITTED:
                    return;
                default:
                    throw new IllegalStateException();
            }
        }

        if(abortOnly){
            throw abortOnWriteConflict();
        }
            
        Listeners[] listeners = null;

        if (firstFreeIndex > 0) {
            final boolean needsPrepare = status == ACTIVE && hasUpdates;
            if(config.dirtyCheck){
                if(needsPrepare && !doPrepareDirty()){
                    throw abortOnWriteConflict();
                }

                listeners = commitDirty();
            }else{
                if(needsPrepare && !doPrepareAll()){
                     throw abortOnWriteConflict();
                }

                listeners = commitAll();
            }
        }

        status = COMMITTED;

        if(listeners != null){
            Listeners.openAll(listeners, pool);
        }
    }

    private Listeners[] commitAll() {
        Listeners[] listenersArray = null;

        int storeIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            final Tranlocal tranlocal = array[k];
            final Listeners listeners = tranlocal.owner.___commitAll(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    final int length = firstFreeIndex - k;
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

    private Listeners[] commitDirty() {
        Listeners[] listenersArray = null;

        int storeIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            final Tranlocal tranlocal = array[k];

            if(tranlocal.isDirty == DIRTY_UNKNOWN){
                tranlocal.calculateIsDirty();
            }

            final Listeners listeners = tranlocal.owner.___commitDirty(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    final int length = firstFreeIndex - k;
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
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                     //won't harm to call it more than once.
                     return;
                case ABORTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't prepare already aborted transaction", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't prepare already committed transaction", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if(abortOnly){
            throw abortOnWriteConflict();
        }

        if(hasUpdates){
            if(config.dirtyCheck){
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
    }

    private boolean doPrepareAll() {
        if(config.lockWrites){
            return true;
        }

        final int spinCount = config.spinCount;

        for (int k = 0; k < firstFreeIndex; k++) {
            final Tranlocal tranlocal = array[k];

            if(tranlocal.isCommitted){
                continue;
            }

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

        for (int k = 0; k < firstFreeIndex; k++) {
            final Tranlocal tranlocal = array[k];

            if(tranlocal.isCommitted){
                continue;
            }

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
    public void registerChangeListenerAndAbort(final Latch listener) {
        if (status != ACTIVE) {
            throw abortOnFaultyStatusOfRegisterChangeListenerAndAbort();
        }

        if(!config.blockingAllowed){
            throw abortOnNoBlockingAllowed();
        }

        if( firstFreeIndex == 0){
            throw abortOnNoRetryPossible();
        }

        final long listenerEra = listener.getEra();

        boolean furtherRegistrationNeeded = true;
        boolean atLeastOneRegistration = false;

        for(int k=0; k < firstFreeIndex; k++){

            final Tranlocal tranlocal = array[k];
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

        status = ABORTED;

        if(!atLeastOneRegistration){
            throw abortOnNoRetryPossible();
        }
    }

    // ==================== reset ==============================

    @Override
    public final boolean softReset() {
        if (status == ACTIVE || status == PREPARED) {
            abort();
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
        hasUpdates = false;
        return true;
    }

    @Override
    public void hardReset(){
        if (status == ACTIVE || status == PREPARED) {
            abort();
        }
        status = ACTIVE;
        abortOnly = false;
        hasReads = false;
        hasUpdates = false;
        hasUntrackedReads = false;
        attempt=1;
        firstFreeIndex = 0;
        remainingTimeoutNs = config.timeoutNs;        
    }

    // ==================== init =============================

    @Override
    public void init(BetaTransactionConfiguration transactionConfig){
        if(transactionConfig == null){
            abort();
            throw new NullPointerException();
        }

        if(status == ACTIVE || status == PREPARED){
            abort();
        }

        config = transactionConfig;
        hardReset();
    }

    // ================== orelse ============================

    @Override
    public final void startEitherBranch(){
        config.needsOrelse();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
    }

    @Override
    public final void endEitherBranch(){
        abort();
        throw new IllegalStateException();
    }

    @Override
    public final void startOrElseBranch(){
        abort();
        throw new IllegalStateException();
    }
}
