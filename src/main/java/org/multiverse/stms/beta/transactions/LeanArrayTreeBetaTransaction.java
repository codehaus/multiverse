package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Watch;
import org.multiverse.api.blocking.DefaultRetryLatch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.*;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.conflictcounters.LocalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.*;

import static java.lang.String.format;


/**
 * A {@link BetaTransaction} for arbitrary size transactions.
 *
 * @author Peter Veentjer.
 */
public final class LeanArrayTreeBetaTransaction extends AbstractLeanBetaTransaction {

    private Tranlocal[] array;
    private LocalConflictCounter localConflictCounter;
    private int size;
    private boolean hasReads;
    private boolean hasUntrackedReads;

    public LeanArrayTreeBetaTransaction(BetaStm stm) {
        this(new BetaTransactionConfiguration(stm).init());
    }

    public LeanArrayTreeBetaTransaction(BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_LEAN_ARRAYTREE, config);
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new Tranlocal[config.minimalArrayTreeSize];
        this.remainingTimeoutNs = config.timeoutNs;
    }

    @Override
    public final LocalConflictCounter getLocalConflictCounter() {
        return localConflictCounter;
    }

    public int size(){
        return size;
    }

    public float getUsage(){
        return (size * 1.0f)/array.length;
    }

    public final boolean tryLock(BetaTransactionalObject ref, int lockMode){
        throw new TodoException();
    }

    public void ensureWrites(){
        if(status != ACTIVE){
            throw abortEnsureWrites();
        }

        if(config.writeLockMode!=LOCKMODE_NONE){
            return;
        }

        if(size == 0){
            return;
        }

        final int spinCount = config.spinCount;
        for(int k=0;k<array.length;k++){
            final Tranlocal tranlocal = array[k];

            if(tranlocal==null || tranlocal.isReadonly()){
                continue;
            }

            if(!tranlocal.owner.___tryLockAndCheckConflict(this, spinCount, tranlocal, false)){
                throw abortOnReadConflict();
            }
        }
    }

    public final <E> E read(BetaRef<E> ref){
        throw new TodoException();
    }

    public final <E> RefTranlocal<E> open(BetaRef<E> ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index != -1){
            return (RefTranlocal<E>)array[index];
        }

        RefTranlocal<E> tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public <E> RefTranlocal<E> openForRead(
        final BetaRef<E> ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            RefTranlocal<E> tranlocal = (RefTranlocal<E>)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        RefTranlocal<E> tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public <E> RefTranlocal<E> openForWrite(
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

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            RefTranlocal<E> tranlocal = (RefTranlocal<E>)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                tranlocal.setStatus(STATUS_UPDATE);
                hasUpdates = true;
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        RefTranlocal<E> tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final BetaRef<E> ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final RefTranlocal<E> tranlocal = (RefTranlocal<E>)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        RefTranlocal<E> tranlocal =  pool.take(ref);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
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
        throw new TodoException();
    }

    public final  IntRefTranlocal open(BetaIntRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index != -1){
            return (IntRefTranlocal)array[index];
        }

        IntRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public  IntRefTranlocal openForRead(
        final BetaIntRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            IntRefTranlocal tranlocal = (IntRefTranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        IntRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  IntRefTranlocal openForWrite(
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

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            IntRefTranlocal tranlocal = (IntRefTranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                tranlocal.setStatus(STATUS_UPDATE);
                hasUpdates = true;
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        IntRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final BetaIntRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final IntRefTranlocal tranlocal = (IntRefTranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        IntRefTranlocal tranlocal =  pool.take(ref);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
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
        throw new TodoException();
    }

    public final  BooleanRefTranlocal open(BetaBooleanRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index != -1){
            return (BooleanRefTranlocal)array[index];
        }

        BooleanRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public  BooleanRefTranlocal openForRead(
        final BetaBooleanRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BooleanRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  BooleanRefTranlocal openForWrite(
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

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                tranlocal.setStatus(STATUS_UPDATE);
                hasUpdates = true;
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BooleanRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  BooleanRefTranlocal openForConstruction(
        final BetaBooleanRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        BooleanRefTranlocal tranlocal =  pool.take(ref);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
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
        throw new TodoException();
    }

    public final  DoubleRefTranlocal open(BetaDoubleRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index != -1){
            return (DoubleRefTranlocal)array[index];
        }

        DoubleRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public  DoubleRefTranlocal openForRead(
        final BetaDoubleRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            DoubleRefTranlocal tranlocal = (DoubleRefTranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        DoubleRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  DoubleRefTranlocal openForWrite(
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

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            DoubleRefTranlocal tranlocal = (DoubleRefTranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                tranlocal.setStatus(STATUS_UPDATE);
                hasUpdates = true;
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        DoubleRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  DoubleRefTranlocal openForConstruction(
        final BetaDoubleRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final DoubleRefTranlocal tranlocal = (DoubleRefTranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        DoubleRefTranlocal tranlocal =  pool.take(ref);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
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
        throw new TodoException();
    }

    public final  LongRefTranlocal open(BetaLongRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index != -1){
            return (LongRefTranlocal)array[index];
        }

        LongRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public  LongRefTranlocal openForRead(
        final BetaLongRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            LongRefTranlocal tranlocal = (LongRefTranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        LongRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  LongRefTranlocal openForWrite(
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

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            LongRefTranlocal tranlocal = (LongRefTranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                tranlocal.setStatus(STATUS_UPDATE);
                hasUpdates = true;
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        LongRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final BetaLongRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final LongRefTranlocal tranlocal = (LongRefTranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        LongRefTranlocal tranlocal =  pool.take(ref);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
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

    public final  Tranlocal open(BetaTransactionalObject ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index != -1){
            return (Tranlocal)array[index];
        }

        Tranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public  Tranlocal openForRead(
        final BetaTransactionalObject ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            Tranlocal tranlocal = (Tranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        Tranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  Tranlocal openForWrite(
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

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            Tranlocal tranlocal = (Tranlocal)array[index];

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                tranlocal.setStatus(STATUS_UPDATE);
                hasUpdates = true;
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        Tranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final Tranlocal tranlocal = (Tranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        Tranlocal tranlocal =  pool.take(ref);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
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
    public Tranlocal get(BetaTransactionalObject ref){
        final int indexOf = indexOf(ref, ref.___identityHashCode());
        if(indexOf == -1){
            return null;
        }

        return array[indexOf];
    }

    public int indexOf(final BetaTransactionalObject ref, final int hash){
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

        for(int k=0; k < oldArray.length; k++){
            final Tranlocal tranlocal = oldArray[k];

            if(tranlocal != null){
               attach(tranlocal.owner, tranlocal, tranlocal.owner.___identityHashCode());
            }
        }

        pool.putTranlocalArray(oldArray);
    }

    private boolean hasReadConflict() {
        if(config.readLockMode!=LOCKMODE_NONE || config.inconsistentReadAllowed) {
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
            if (tranlocal != null && tranlocal.owner.___hasReadConflict(tranlocal)) {
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
                if(size > 0){
                    for (int k = 0; k < array.length; k++) {
                        final Tranlocal tranlocal = array[k];
                        if(tranlocal != null){
                            array[k] = null;
                            tranlocal.owner.___abort(this, tranlocal, pool);
                        }
                    }
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
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    break;
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

        Listeners[] listenersArray = null;
        if(size > 0){
            final boolean needsPrepare = status == ACTIVE
                && hasUpdates
                && config.readLockMode != LOCKMODE_COMMIT;

            if(config.dirtyCheck){
                if(needsPrepare && !doPrepareDirty()){
                    throw abortOnWriteConflict();
                }

                listenersArray = commitDirty();
            }else{
                if(needsPrepare && !doPrepareAll()){
                    throw abortOnWriteConflict();
                }

                listenersArray = commitAll();
            }
        }
        status = COMMITTED;

        if(listenersArray != null){
            Listeners.openAll(listenersArray, pool);
        }
    }

    private Listeners[] commitAll() {
        Listeners[] listenersArray = null;

        int listenersArrayIndex = 0;
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];

            if(tranlocal == null){
                continue;
            }

            array[k] = null;
            final Listeners listeners = tranlocal.owner.___commitAll(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    int length = array.length - k;
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
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];

            if(tranlocal == null){
                continue;
            }

            array[k] = null;

            if(!tranlocal.isReadonly() && !tranlocal.isDirty()){
                tranlocal.calculateIsDirty();
            }

            final Listeners listeners = tranlocal.owner.___commitDirty(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    int length = array.length - k;
                    listenersArray = pool.takeListenersArray(length);
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
                        format("[%s] Can't prepare an already aborted transaction",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't prepare already committed transaction",config.familyName));
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

        for (int k = 0; k < array.length; k++) {
            final Tranlocal tranlocal = array[k];

            if (tranlocal == null){
                continue;
            }

            if(!tranlocal.prepareAllUpdates(pool, this, spinCount)) {
                return false;
            }
        }

        return true;
    }

    private boolean doPrepareDirty() {
        final int spinCount = config.spinCount;

        for (int k = 0; k < array.length; k++) {
            final Tranlocal tranlocal = array[k];

            if(tranlocal == null){
                continue;
            }

            if(!tranlocal.prepareDirtyUpdates(pool, this, spinCount)) {
                return false;
            }
        }

        return true;
    }

    // ============================ registerChangeListener ===============================

    @Override
    public void retry() {
        if (status != ACTIVE) {
            throw abortOnFaultyStatusOfRetry();
        }

        if(!config.blockingAllowed){
            throw abortOnNoBlockingAllowed();
        }

        if( size == 0){
            throw abortOnNoRetryPossible();
        }

        DefaultRetryLatch listener = pool.takeDefaultRetryLatch();

        try{
            final long listenerEra = listener.getEra();
            boolean furtherRegistrationNeeded = true;
            boolean atLeastOneRegistration = false;
            for(int k=0; k < array.length; k++){
                final Tranlocal tranlocal = array[k];

                if(tranlocal == null){
                    continue;
                }

                array[k]=null;
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

                    owner.___abort(this, tranlocal, pool);
                }
            }

            status = ABORTED;
            if(!atLeastOneRegistration){
                throw abortOnNoRetryPossible();
            }

            awaitUpdate(listener);
            throw Retry.INSTANCE;
        }finally{
            pool.putDefaultRetryLatch(listener);
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

        if(array.length > config.minimalArrayTreeSize){
            pool.putTranlocalArray(array);
            array = pool.takeTranlocalArray(config.minimalArrayTreeSize);
        }

        status = ACTIVE;
        abortOnly = false;
        hasReads = false;
        hasUpdates = false;
        hasUntrackedReads = false;
        size = 0;
        attempt++;
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
        }

        status = ACTIVE;
        abortOnly = false;
        hasUpdates = false;
        hasReads = false;
        hasUntrackedReads = false;
        attempt = 1;
        remainingTimeoutNs = config.timeoutNs;
        size = 0;
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


}
