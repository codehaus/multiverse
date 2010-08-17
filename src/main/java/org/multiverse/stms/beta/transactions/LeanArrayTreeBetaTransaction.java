package org.multiverse.stms.beta.transactions;

import org.multiverse.api.blocking.Latch;
import org.multiverse.api.blocking.Listeners;
import org.multiverse.api.exceptions.*;
import org.multiverse.functions.Function;
import org.multiverse.functions.IntFunction;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.conflictcounters.LocalConflictCounter;
import org.multiverse.stms.beta.refs.*;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;


/**
 * A {@link BetaTransaction} for arbitrary size transactions.
 *
 * @author Peter Veentjer.
 */
public final class LeanArrayTreeBetaTransaction extends AbstractLeanBetaTransaction {

    public final static AtomicLong conflictScan = new AtomicLong();

    private LocalConflictCounter localConflictCounter;
    private Tranlocal[] array;
    private int size;
    private boolean hasReads;
    private boolean hasUntrackedReads;

    public LeanArrayTreeBetaTransaction(BetaStm stm) {
        this(new BetaTransactionConfig(stm));
    }

    public LeanArrayTreeBetaTransaction(BetaTransactionConfig config) {
        super(POOL_TRANSACTIONTYPE_LEAN_ARRAYTREE, config);
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new Tranlocal[config.minimalArrayTreeSize];
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
                        format("Can't start already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't start already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't start already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        throw new TodoException();
    }

    public int size(){
        return size;
    }

    public float getUsage(){
        return (size * 1.0f)/array.length;
    }


    @Override
    public <E> RefTranlocal<E> openForRead(
        final Ref<E> ref, boolean lock, final BetaObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            throw abortOpenForRead(pool);
        }

        //a read on a null ref, always returns a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int identityHashCode = ref.identityHashCode();
        int index = findAttachedIndex(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            RefTranlocal<E> found = (RefTranlocal<E>)array[index];

            if(found.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final RefTranlocal<E> read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                //make sure that there are no conflicts.
                if (hasReadConflict()) {
                    ref.abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                found.read = read;
                found.evaluateCommutingFunctions(pool);
            }else if(lock && !ref.tryLockAndCheckConflict(this, config.spinCount,found)){
                throw abortOnReadConflict(pool);
            }

            return found;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        final RefTranlocal<E> read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if(lock || !read.isPermanent || config.trackReads){
            attach(ref, read, identityHashCode, pool);
            size++;
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
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException(
                format("Can't open for writing a null transactional object or ref on transaction '%s'",config.familyName));
        }

        //lets find the tranlocal
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];

            if(result.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final RefTranlocal<E> read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if (hasReadConflict()) {
                    ref.abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                result.read = read;
                result.evaluateCommutingFunctions(pool);
                return result;
            }else if(lock && !ref.tryLockAndCheckConflict(this, config.spinCount,result)){
                throw abortOnReadConflict(pool);
            }

            if(!result.isCommitted){
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final RefTranlocal<E> read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        RefTranlocal<E> result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }
        result.read = read;
        result.value = read.value;
        attach(ref, result, identityHashCode, pool);
        size++;
        return result;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final Ref<E> ref, final BetaObjectPool pool) {
        assert pool!=null;

        if (status != ACTIVE) {
            throw abortOpenForConstruction(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't construct new object using readonly transaction '%s'",config.familyName));
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException(
                format("Can't open a null transactionalobject or ref for construction on transaction '%s'",config.familyName));
        }

        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            final RefTranlocal<E> result = (RefTranlocal<E>)array[index];
            if(result.isCommitted || result.read!=null){
                throw new IllegalArgumentException(
                    format("Can't open a previous committed object for construction on transaction '%s'",config.familyName));
            }

            return result;
        }

        if(ref.unsafeLoad()!=null){
            abort();
            throw new IllegalArgumentException(
                format("Can't open a previous committed object of class '%s' for construction on transaction '%s'",
                    config.familyName, ref.getClass().getName()));
        }

        RefTranlocal<E> result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }
        attach(ref, result, identityHashCode, pool);
        size++;
        return result;
    }

    public <E> void commute(
        final Ref<E> ref, final BetaObjectPool pool, final Function<E> function){

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

        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if(index == -1){
            //todo: call to 'openForCommute' can be inlined.
            RefTranlocal<E> result = ref.openForCommute(pool);
            attach(ref, result, identityHashCode, pool);
            result.addCommutingFunction(function, pool);
            size++;
            return;
        }

        RefTranlocal<E> result = (RefTranlocal<E>)array[index];
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            RefTranlocal<E> read = result;
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

        //a read on a null ref, always returns a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int identityHashCode = ref.identityHashCode();
        int index = findAttachedIndex(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            IntRefTranlocal found = (IntRefTranlocal)array[index];

            if(found.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final IntRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                //make sure that there are no conflicts.
                if (hasReadConflict()) {
                    ref.abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                found.read = read;
                found.evaluateCommutingFunctions(pool);
            }else if(lock && !ref.tryLockAndCheckConflict(this, config.spinCount,found)){
                throw abortOnReadConflict(pool);
            }

            return found;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        final IntRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if(lock || !read.isPermanent || config.trackReads){
            attach(ref, read, identityHashCode, pool);
            size++;
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
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException(
                format("Can't open for writing a null transactional object or ref on transaction '%s'",config.familyName));
        }

        //lets find the tranlocal
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            IntRefTranlocal result = (IntRefTranlocal)array[index];

            if(result.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final IntRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if (hasReadConflict()) {
                    ref.abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                result.read = read;
                result.evaluateCommutingFunctions(pool);
                return result;
            }else if(lock && !ref.tryLockAndCheckConflict(this, config.spinCount,result)){
                throw abortOnReadConflict(pool);
            }

            if(!result.isCommitted){
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final IntRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        IntRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }
        result.read = read;
        result.value = read.value;
        attach(ref, result, identityHashCode, pool);
        size++;
        return result;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final IntRef ref, final BetaObjectPool pool) {
        assert pool!=null;

        if (status != ACTIVE) {
            throw abortOpenForConstruction(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't construct new object using readonly transaction '%s'",config.familyName));
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException(
                format("Can't open a null transactionalobject or ref for construction on transaction '%s'",config.familyName));
        }

        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            final IntRefTranlocal result = (IntRefTranlocal)array[index];
            if(result.isCommitted || result.read!=null){
                throw new IllegalArgumentException(
                    format("Can't open a previous committed object for construction on transaction '%s'",config.familyName));
            }

            return result;
        }

        if(ref.unsafeLoad()!=null){
            abort();
            throw new IllegalArgumentException(
                format("Can't open a previous committed object of class '%s' for construction on transaction '%s'",
                    config.familyName, ref.getClass().getName()));
        }

        IntRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }
        attach(ref, result, identityHashCode, pool);
        size++;
        return result;
    }

    public  void commute(
        final IntRef ref, final BetaObjectPool pool, final IntFunction function){

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

        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if(index == -1){
            //todo: call to 'openForCommute' can be inlined.
            IntRefTranlocal result = ref.openForCommute(pool);
            attach(ref, result, identityHashCode, pool);
            result.addCommutingFunction(function, pool);
            size++;
            return;
        }

        IntRefTranlocal result = (IntRefTranlocal)array[index];
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            IntRefTranlocal read = result;
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

        //a read on a null ref, always returns a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int identityHashCode = ref.identityHashCode();
        int index = findAttachedIndex(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            LongRefTranlocal found = (LongRefTranlocal)array[index];

            if(found.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final LongRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                //make sure that there are no conflicts.
                if (hasReadConflict()) {
                    ref.abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                found.read = read;
                found.evaluateCommutingFunctions(pool);
            }else if(lock && !ref.tryLockAndCheckConflict(this, config.spinCount,found)){
                throw abortOnReadConflict(pool);
            }

            return found;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        final LongRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if(lock || !read.isPermanent || config.trackReads){
            attach(ref, read, identityHashCode, pool);
            size++;
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
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException(
                format("Can't open for writing a null transactional object or ref on transaction '%s'",config.familyName));
        }

        //lets find the tranlocal
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            LongRefTranlocal result = (LongRefTranlocal)array[index];

            if(result.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final LongRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if (hasReadConflict()) {
                    ref.abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                result.read = read;
                result.evaluateCommutingFunctions(pool);
                return result;
            }else if(lock && !ref.tryLockAndCheckConflict(this, config.spinCount,result)){
                throw abortOnReadConflict(pool);
            }

            if(!result.isCommitted){
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final LongRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        LongRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }
        result.read = read;
        result.value = read.value;
        attach(ref, result, identityHashCode, pool);
        size++;
        return result;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final LongRef ref, final BetaObjectPool pool) {
        assert pool!=null;

        if (status != ACTIVE) {
            throw abortOpenForConstruction(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't construct new object using readonly transaction '%s'",config.familyName));
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException(
                format("Can't open a null transactionalobject or ref for construction on transaction '%s'",config.familyName));
        }

        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            final LongRefTranlocal result = (LongRefTranlocal)array[index];
            if(result.isCommitted || result.read!=null){
                throw new IllegalArgumentException(
                    format("Can't open a previous committed object for construction on transaction '%s'",config.familyName));
            }

            return result;
        }

        if(ref.unsafeLoad()!=null){
            abort();
            throw new IllegalArgumentException(
                format("Can't open a previous committed object of class '%s' for construction on transaction '%s'",
                    config.familyName, ref.getClass().getName()));
        }

        LongRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }
        attach(ref, result, identityHashCode, pool);
        size++;
        return result;
    }

    public  void commute(
        final LongRef ref, final BetaObjectPool pool, final LongFunction function){

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

        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if(index == -1){
            //todo: call to 'openForCommute' can be inlined.
            LongRefTranlocal result = ref.openForCommute(pool);
            attach(ref, result, identityHashCode, pool);
            result.addCommutingFunction(function, pool);
            size++;
            return;
        }

        LongRefTranlocal result = (LongRefTranlocal)array[index];
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            LongRefTranlocal read = result;
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

        //a read on a null ref, always returns a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int identityHashCode = ref.identityHashCode();
        int index = findAttachedIndex(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            Tranlocal found = (Tranlocal)array[index];

            if(found.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final Tranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                //make sure that there are no conflicts.
                if (hasReadConflict()) {
                    ref.abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                found.read = read;
                found.evaluateCommutingFunctions(pool);
            }else if(lock && !ref.tryLockAndCheckConflict(this, config.spinCount,found)){
                throw abortOnReadConflict(pool);
            }

            return found;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        final Tranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if(lock || !read.isPermanent || config.trackReads){
            attach(ref, read, identityHashCode, pool);
            size++;
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
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException(
                format("Can't open for writing a null transactional object or ref on transaction '%s'",config.familyName));
        }

        //lets find the tranlocal
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            Tranlocal result = (Tranlocal)array[index];

            if(result.isCommuting){
                if(!hasReads){
                    localConflictCounter.reset();
                    hasReads = true;
                }

                final Tranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if (hasReadConflict()) {
                    ref.abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                result.read = read;
                result.evaluateCommutingFunctions(pool);
                return result;
            }else if(lock && !ref.tryLockAndCheckConflict(this, config.spinCount,result)){
                throw abortOnReadConflict(pool);
            }

            if(!result.isCommitted){
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final Tranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if(read.isLocked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        Tranlocal result = read.openForWrite(pool);
        attach(ref, result, identityHashCode, pool);
        size++;
        return result;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref, final BetaObjectPool pool) {
        assert pool!=null;

        if (status != ACTIVE) {
            throw abortOpenForConstruction(pool);
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't construct new object using readonly transaction '%s'",config.familyName));
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException(
                format("Can't open a null transactionalobject or ref for construction on transaction '%s'",config.familyName));
        }

        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            final Tranlocal result = (Tranlocal)array[index];
            if(result.isCommitted || result.read!=null){
                throw new IllegalArgumentException(
                    format("Can't open a previous committed object for construction on transaction '%s'",config.familyName));
            }

            return result;
        }

        if(ref.unsafeLoad()!=null){
            abort();
            throw new IllegalArgumentException(
                format("Can't open a previous committed object of class '%s' for construction on transaction '%s'",
                    config.familyName, ref.getClass().getName()));
        }

        Tranlocal result = ref.openForConstruction(pool);
        attach(ref, result, identityHashCode, pool);
        size++;
        return result;
    }

    public  void commute(
        final BetaTransactionalObject ref, final BetaObjectPool pool, final Function function){

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

        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        if(index == -1){
            //todo: call to 'openForCommute' can be inlined.
            Tranlocal result = ref.openForCommute(pool);
            attach(ref, result, identityHashCode, pool);
            result.addCommutingFunction(function, pool);
            size++;
            return;
        }

        Tranlocal result = (Tranlocal)array[index];
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            Tranlocal read = result;
            result = read.openForWrite(pool);
            array[index]=result;
         }

         throw new TodoException();
    }


 
    public Tranlocal get(BetaTransactionalObject ref){
        final int indexOf = findAttachedIndex(ref, ref.identityHashCode());
        if(indexOf == -1){
            return null;
        }

        return array[indexOf];
    }

    public int findAttachedIndex(final BetaTransactionalObject ref, final int hash){
        int jump = 0;
        boolean goLeft = true;

        do{
            int offset = goLeft?-jump:jump;
            int index = (hash + offset) % array.length;

            Tranlocal current = array[index];
            if(current == null){
                return -1;
            }

            if(current.owner == ref){
                return index;
            }

            int currentHash = current.owner.identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0 ? 1 : jump*2;
        }while(jump < array.length);

        return -1;
    }

    private void attach(final BetaTransactionalObject ref, final Tranlocal tranlocal, final int hash, final BetaObjectPool pool){
        int jump = 0;
        boolean goLeft = true;

        do{
            int offset = goLeft?-jump:jump;
            int index = (hash + offset) % array.length;

            Tranlocal current = array[index];
            if(current == null){
                array[index] = tranlocal;
                return;
            }

            int currentHash = current.owner.identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0?1:jump*2;
        }while(jump < array.length);

        expand(pool);
        attach(ref, tranlocal, hash, pool);
    }

    private void expand(final BetaObjectPool pool){
        Tranlocal[] oldArray = array;
        int newSize = oldArray.length*2;
        array = pool.takeTranlocalArray(newSize);
        if(array == null){
            array = new Tranlocal[newSize];
        }

        for(int k=0; k < oldArray.length; k++){
            Tranlocal tranlocal = oldArray[k];

            if(tranlocal != null){
               attach(tranlocal.owner, tranlocal, tranlocal.owner.identityHashCode(),pool);
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
            Tranlocal tranlocal = array[k];
            if (tranlocal != null && tranlocal.owner.hasReadConflict(tranlocal, this)) {
                return true;
            }
        }

        return false;
    }

    // ============================== abort ===================================================

    @Override
    public void abort() {
        abort(getThreadLocalBetaObjectPool());
    }

    @Override
    public void abort(final BetaObjectPool pool) {
        switch (status) {
            case ACTIVE:
                //fall through
            case PREPARED:
                status = ABORTED;
                if(size>0){
                    for (int k = 0; k < array.length; k++) {
                        Tranlocal tranlocal = array[k];
                        if(tranlocal != null){
                            tranlocal.owner.abort(this, tranlocal, pool);
                        }
                    }
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

    // ============================== commit ==================================================

    @Override
    public void commit() {
        commit(getThreadLocalBetaObjectPool());
    }

    @Override
    public void commit(final BetaObjectPool pool) {
        if (status != ACTIVE && status != PREPARED) {
            switch (status) {
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't commit already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    return;
                default:
                    throw new IllegalStateException();
            }
        }

        if(abortOnly){
            throw abortOnWriteConflict(pool);
        }

        Listeners[] listenersArray = null;
        if(size>0){
            if(config.dirtyCheck){
                if(status == ACTIVE && !doPrepareDirty(pool)){
                    throw abortOnWriteConflict(pool);
                }

                listenersArray = commitDirty(pool);
            }else{
                if(status == ACTIVE && !doPrepareAll(pool)){
                    throw abortOnWriteConflict(pool);
                }

                listenersArray = commitAll(pool);
            }
        }
        status = COMMITTED;

        if(listenersArray != null){
            Listeners.openAll(listenersArray, pool);
        }
    }

    private Listeners[] commitAll(final BetaObjectPool pool) {
        Listeners[] listenersArray = null;

        int listenersArrayIndex = 0;
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];
            if(tranlocal != null){
                Listeners listeners = tranlocal.owner.commitAll(tranlocal, this, pool, config.globalConflictCounter);

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

    private Listeners[] commitDirty(final BetaObjectPool pool) {
        Listeners[] listenersArray = null;

        int listenersArrayIndex = 0;
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];
            if(tranlocal != null){
                 Listeners listeners = tranlocal.owner.commitDirty(tranlocal, this, pool, config.globalConflictCounter);

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

    // ============================== prepare ==================================================

    @Override
    public void prepare() {
        prepare(getThreadLocalBetaObjectPool());
    }

    @Override
    public void prepare(BetaObjectPool pool) {
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                     return;
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't prepare already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't prepare already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();

            }
        }

        if(abortOnly){
            throw abortOnWriteConflict(pool);
        }

        if(size>0){
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
    }

    private boolean doPrepareAll(final BetaObjectPool pool) {
        final int spinCount = config.spinCount;

        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];

            if (tranlocal==null || tranlocal.isCommitted){
                continue;
            }

            if(tranlocal.isCommuting){
                Tranlocal read = tranlocal.owner.lockAndLoad(spinCount, this);

                if(read.isLocked){
                    return false;
                }

                tranlocal.read = read;
                tranlocal.evaluateCommutingFunctions(pool);
            }

            if(!tranlocal.owner.tryLockAndCheckConflict(this, spinCount, tranlocal)){
               return false;
            }
        }

        return true;
    }

    private boolean doPrepareDirty(final BetaObjectPool pool) {
        final int spinCount = config.spinCount;

        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];

            if(tranlocal == null || tranlocal.isCommitted){
                continue;
            }

            if(tranlocal.isCommuting){
                Tranlocal read = tranlocal.owner.lockAndLoad(spinCount, this);

                if(read.isLocked){
                    return false;
                }

                tranlocal.read = read;
                tranlocal.evaluateCommutingFunctions(pool);
            }else if (tranlocal.calculateIsDirty()) {
                if(!tranlocal.owner.tryLockAndCheckConflict(this, spinCount, tranlocal)){
                    return false;
                }
            }
        }

        return true;
    }

    // ============================ registerChangeListener ===============================

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

        if( size == 0){
            abort();
            throw new NoRetryPossibleException(
                format("Can't block transaction '%s', since there are no tracked reads",config.familyName));
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
                        switch(owner.registerChangeListener(listener, tranlocal, pool, listenerEra)){
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

                    owner.abort(this, tranlocal, pool);
                }
            }
        }

        status = ABORTED;
        if(!atLeastOneRegistration){
            throw new NoRetryPossibleException(
                format("Can't block transaction '%s', since there are no tracked reads",
                    config.familyName));
        }
    }

    // ============================== reset ========================================

    @Override
    public boolean softReset(){
        return softReset(getThreadLocalBetaObjectPool());
    }

    @Override
    public boolean softReset(final BetaObjectPool pool) {
        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
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
        hasUntrackedReads = false;
        size = 0;
        attempt++;
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
        hasUntrackedReads = false;
        attempt = 1;
        remainingTimeoutNs = config.timeoutNs;
        size = 0;
    }

    // ============================== init =======================================

    @Override
    public void init(BetaTransactionConfig transactionConfig){
        init(transactionConfig, getThreadLocalBetaObjectPool());
    }

    @Override
    public void init(BetaTransactionConfig transactionConfig, BetaObjectPool pool){
        if(transactionConfig == null){
            abort();
            throw new NullPointerException();
        }

        if(status == ACTIVE || status == PREPARED){
            abort(pool);
        }

        this.config = transactionConfig;
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
