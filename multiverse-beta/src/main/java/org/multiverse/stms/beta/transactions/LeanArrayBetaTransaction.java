package org.multiverse.stms.beta.transactions;

import org.multiverse.api.*;
import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.*;
import org.multiverse.api.lifecycle.*;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.conflictcounters.*;

import static org.multiverse.stms.beta.BetaStmUtils.toDebugString;

import java.util.concurrent.atomic.AtomicLong;
import static java.lang.String.format;

public final class LeanArrayBetaTransaction extends AbstractLeanBetaTransaction {

    public final static AtomicLong conflictScan = new AtomicLong();

    private final BetaTranlocal[] array;
    private LocalConflictCounter localConflictCounter;
    private int firstFreeIndex = 0;
    private boolean hasReads;
    private boolean hasUntrackedReads;

    public LeanArrayBetaTransaction(final BetaStm stm) {
        this(new BetaTransactionConfiguration(stm).init());
    }

    public LeanArrayBetaTransaction(final BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_LEAN_ARRAY, config);
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new BetaTranlocal[config.maxArrayTransactionSize];
        this.remainingTimeoutNs = config.timeoutNs;
    }

    @Override
    public final LocalConflictCounter getLocalConflictCounter() {
        return localConflictCounter;
    }

    public void ensureWrites(){
        if(status != ACTIVE){
            throw abortEnsureWrites();
        }

        if(config.writeLockMode!=LOCKMODE_NONE){
            return;
        }

        if(firstFreeIndex == 0){
            return;
        }

        final int spinCount = config.spinCount;
        for(int k=0;k<firstFreeIndex;k++){
            final BetaTranlocal tranlocal = array[k];

            if(tranlocal.isReadonly()){
                continue;
            }

            if(!tranlocal.owner.___tryLockAndCheckConflict(this, spinCount, tranlocal, false)){
                throw abortOnReadConflict();
            }
        }
    }

    @Override
    public final boolean tryLock(BetaTransactionalObject ref, int lockMode){
       if (status != ACTIVE) {
           throw abortTryLock(ref);
       }

       if (ref == null) {
           throw abortTryLockWhenNullReference(ref);
       }

       lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;

       throw new TodoException();
    }


    public final <E> E read(BetaRef<E> ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int index = indexOf(ref);
        if(index != -1){
            BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(    config.trackReads
            || config.isolationLevel!=IsolationLevel.ReadCommitted
            || config.readLockMode!=LOCKMODE_NONE){

            //check if the size is not exceeded.
            if (firstFreeIndex == array.length) {
                throw abortOnTooSmallSize(array.length+1);
            }

            BetaRefTranlocal<E> tranlocal = pool.take(ref);
            tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
            tranlocal.tx = this;
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    @Override
    public <E> BetaRefTranlocal<E> openForRead(
        final BetaRef<E> ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaRefTranlocal<E> tranlocal = pool.take(ref);
        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public <E> BetaRefTranlocal<E> openForWrite(
        final BetaRef<E>  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
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

        BetaRefTranlocal<E> tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final <E> BetaRefTranlocal<E> openForConstruction(
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
            BetaRefTranlocal<E> result = (BetaRefTranlocal<E>)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.getVersion() != BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        BetaRefTranlocal<E> tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
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


    public final  int read(BetaIntRef ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int index = indexOf(ref);
        if(index != -1){
            BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(    config.trackReads
            || config.isolationLevel!=IsolationLevel.ReadCommitted
            || config.readLockMode!=LOCKMODE_NONE){

            //check if the size is not exceeded.
            if (firstFreeIndex == array.length) {
                throw abortOnTooSmallSize(array.length+1);
            }

            BetaIntRefTranlocal tranlocal = pool.take(ref);
            tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
            tranlocal.tx = this;
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    @Override
    public  BetaIntRefTranlocal openForRead(
        final BetaIntRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaIntRefTranlocal tranlocal = pool.take(ref);
        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  BetaIntRefTranlocal openForWrite(
        final BetaIntRef  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
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

        BetaIntRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  BetaIntRefTranlocal openForConstruction(
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
            BetaIntRefTranlocal result = (BetaIntRefTranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.getVersion() != BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        BetaIntRefTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
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


    public final  boolean read(BetaBooleanRef ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int index = indexOf(ref);
        if(index != -1){
            BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(    config.trackReads
            || config.isolationLevel!=IsolationLevel.ReadCommitted
            || config.readLockMode!=LOCKMODE_NONE){

            //check if the size is not exceeded.
            if (firstFreeIndex == array.length) {
                throw abortOnTooSmallSize(array.length+1);
            }

            BetaBooleanRefTranlocal tranlocal = pool.take(ref);
            tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
            tranlocal.tx = this;
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    @Override
    public  BetaBooleanRefTranlocal openForRead(
        final BetaBooleanRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaBooleanRefTranlocal tranlocal = pool.take(ref);
        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  BetaBooleanRefTranlocal openForWrite(
        final BetaBooleanRef  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
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

        BetaBooleanRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  BetaBooleanRefTranlocal openForConstruction(
        final BetaBooleanRef ref) {

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
            BetaBooleanRefTranlocal result = (BetaBooleanRefTranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.getVersion() != BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        BetaBooleanRefTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    public  void commute(
        final BetaBooleanRef ref, final BooleanFunction function){

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


    public final  double read(BetaDoubleRef ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int index = indexOf(ref);
        if(index != -1){
            BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(    config.trackReads
            || config.isolationLevel!=IsolationLevel.ReadCommitted
            || config.readLockMode!=LOCKMODE_NONE){

            //check if the size is not exceeded.
            if (firstFreeIndex == array.length) {
                throw abortOnTooSmallSize(array.length+1);
            }

            BetaDoubleRefTranlocal tranlocal = pool.take(ref);
            tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
            tranlocal.tx = this;
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    @Override
    public  BetaDoubleRefTranlocal openForRead(
        final BetaDoubleRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaDoubleRefTranlocal tranlocal = pool.take(ref);
        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  BetaDoubleRefTranlocal openForWrite(
        final BetaDoubleRef  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
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

        BetaDoubleRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  BetaDoubleRefTranlocal openForConstruction(
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
            BetaDoubleRefTranlocal result = (BetaDoubleRefTranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.getVersion() != BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        BetaDoubleRefTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
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


    public final  long read(BetaLongRef ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int index = indexOf(ref);
        if(index != -1){
            BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(    config.trackReads
            || config.isolationLevel!=IsolationLevel.ReadCommitted
            || config.readLockMode!=LOCKMODE_NONE){

            //check if the size is not exceeded.
            if (firstFreeIndex == array.length) {
                throw abortOnTooSmallSize(array.length+1);
            }

            BetaLongRefTranlocal tranlocal = pool.take(ref);
            tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
            tranlocal.tx = this;
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    @Override
    public  BetaLongRefTranlocal openForRead(
        final BetaLongRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaLongRefTranlocal tranlocal = pool.take(ref);
        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  BetaLongRefTranlocal openForWrite(
        final BetaLongRef  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
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

        BetaLongRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  BetaLongRefTranlocal openForConstruction(
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
            BetaLongRefTranlocal result = (BetaLongRefTranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.getVersion() != BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        BetaLongRefTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
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
    public  BetaTranlocal openForRead(
        final BetaTransactionalObject ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            BetaTranlocal tranlocal = (BetaTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaTranlocal tranlocal = pool.take(ref);
        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  BetaTranlocal openForWrite(
        final BetaTransactionalObject  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            BetaTranlocal tranlocal = (BetaTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
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

        BetaTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  BetaTranlocal openForConstruction(
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
            BetaTranlocal result = (BetaTranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.getVersion() != BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        BetaTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
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
    public BetaTranlocal get(BetaTransactionalObject owner){
        int indexOf = indexOf(owner);
        return indexOf == -1 ? null: array[indexOf];
    }

    @Override
    public BetaTranlocal locate(BetaTransactionalObject owner){
        if (status != ACTIVE) {
           throw abortLocate(owner);
        }

        if(owner == null){
            throw abortLocateWhenNullReference();
        }

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
            final BetaTranlocal tranlocal = array[k];
            if(tranlocal.owner == owner){
                return k;
            }
        }

        return -1;
    }

    private boolean hasReadConflict() {
        if (config.readLockMode!=LOCKMODE_NONE||config.inconsistentReadAllowed) {
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
            final BetaTranlocal tranlocal = array[k];

            if (tranlocal.owner.___hasReadConflict(tranlocal)) {
                return true;
            }
        }

        return false;
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
                    final BetaTranlocal tranlocal = array[k];
                    array[k]=null;
                    tranlocal.owner.___abort(this, tranlocal, pool);
                }
                break;
            case ABORTED:
                break;
            case COMMITTED:
                throw new DeadTransactionException(
                    format("[%s] Failed to execute BetaTransaction.abort, reason: the transaction already is committed",
                        config.familyName));
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
                        format("[%s] Failed to execute BetaTransaction.commit, reason:the transaction already is aborted",
                            config.familyName));
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
            final boolean needsPrepare = status == ACTIVE
                    && hasUpdates
                    && config.readLockMode != LOCKMODE_COMMIT;

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

        int listenersArrayIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            final BetaTranlocal tranlocal = array[k];
            array[k] = null;

            final Listeners listeners = tranlocal.owner.___commitAll(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    final int length = firstFreeIndex - k;
                    listenersArray = pool.takeListenersArray(length);
                }
                listenersArray[listenersArrayIndex]=listeners;
                listenersArrayIndex++;
            }
        }

        return listenersArray;
    }

    private Listeners[] commitDirty() {
        Listeners[] listenersArray = null;

        int listenersArrayIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            final BetaTranlocal tranlocal = array[k];
            array[k] = null;

            //we need to make sure that the dirty flag is set since it could happen that the
            //prepare completes before setting the dirty flags
            if(!tranlocal.isReadonly() && !tranlocal.isDirty()){
                tranlocal.calculateIsDirty();
            }

            final Listeners listeners = tranlocal.owner.___commitDirty(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    final int length = firstFreeIndex - k;
                    listenersArray = pool.takeListenersArray(length);
                }
                listenersArray[listenersArrayIndex]=listeners;
                listenersArrayIndex++;
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
                        format("[%s] Failed to execute BetaTransaction.prepare, reason: the transaction already is aborted",
                            config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("[%s] Failed to execute BetaTransaction.prepare, reason: the transaction already is committed",
                            config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if(abortOnly){
            throw abortOnWriteConflict();
        }

        if(hasUpdates && config.readLockMode != LOCKMODE_COMMIT){
            final boolean success = config.dirtyCheck ? doPrepareDirty() : doPrepareAll();
            if(!success){
                throw abortOnWriteConflict();
            }
        }

        status = PREPARED;
    }

    private boolean doPrepareAll() {
        final int spinCount = config.spinCount;

        for (int k = 0; k < firstFreeIndex; k++) {
            final BetaTranlocal tranlocal = array[k];

            if(!tranlocal.prepareAllUpdates(pool, this, spinCount)) {
                return false;
            }
        }

        return true;
    }

    private boolean doPrepareDirty() {
        final int spinCount = config.spinCount;

        for (int k = 0; k < firstFreeIndex; k++) {
            final BetaTranlocal tranlocal = array[k];

            if(!tranlocal.prepareDirtyUpdates(pool, this, spinCount)) {
                return false;
            }
        }

        return true;
    }

    // ============================== retry ========================

    @Override
    public void retry() {
        if (status != ACTIVE) {
            throw abortOnFaultyStatusOfRetry();
        }

        if(!config.blockingAllowed){
            throw abortOnNoBlockingAllowed();
        }

        if( firstFreeIndex == 0){
            throw abortOnNoRetryPossible();
        }

        listener.reset();
        final long listenerEra = listener.getEra();

        boolean furtherRegistrationNeeded = true;
        boolean atLeastOneRegistration = false;

        for(int k=0; k < firstFreeIndex; k++){

            final BetaTranlocal tranlocal = array[k];
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
            array[k]=null;
        }

        status = ABORTED;

        if(!atLeastOneRegistration){
            throw abortOnNoRetryPossible();
        }

        throw Retry.INSTANCE;
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

}
