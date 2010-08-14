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

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public final class LeanArrayBetaTransaction extends AbstractLeanBetaTransaction {

    public final static AtomicLong conflictScan = new AtomicLong();

    private final LocalConflictCounter localConflictCounter;
    private final Tranlocal[] array;
    private int firstFreeIndex = 0;
    private boolean hasReads;
    private boolean hasUntrackedReads;

    public LeanArrayBetaTransaction(final BetaStm stm) {
        this(new BetaTransactionConfig(stm));
    }

    public LeanArrayBetaTransaction(final BetaTransactionConfig config) {
        super(POOL_TRANSACTIONTYPE_LEAN_ARRAY, config);
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new Tranlocal[config.maxArrayTransactionSize];
        this.remainingTimeoutNs = config.timeoutNs;
    }

    public final LocalConflictCounter getLocalConflictCounter() {
        return localConflictCounter;
    }

    @Override
    public void start() {
        start(getThreadLocalBetaObjectPool());
    }

    @Override
    public void start(final BetaObjectPool pool) {
        if (status != NEW) {
            switch (status) {
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
    }

    @Override
    public <E> RefTranlocal<E> openForRead(
            final Ref<E> ref, boolean lock, final BetaObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't read from already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't read from already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't read from already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        //a read on a null ref, always return a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if (index >= 0) {
            //we are lucky, at already is attached to the session
            RefTranlocal<E> found = (RefTranlocal<E>) array[index];

            if (lock) {
                if (!ref.tryLockAndCheckConflict(this, config.spinCount, found)) {
                    throw abortOnReadConflict(pool);
                }
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
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        if (!hasReads) {
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        RefTranlocal<E> read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if (lock || config.trackReads || !read.isPermanent) {
            array[firstFreeIndex] = read;
            firstFreeIndex++;
        } else {
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public <E> RefTranlocal<E> openForWrite(
            final Ref<E> ref, boolean lock, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't write to already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't write to already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't write to already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                    format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if (index != -1) {
            RefTranlocal<E> result = (RefTranlocal<E>) array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            if (lock) {
                if (!ref.tryLockAndCheckConflict(this, config.spinCount, result)) {
                    throw abortOnReadConflict(pool);
                }
            }

            //and open it for write if needed.
            if (result.isCommitted) {
                result = result.openForWrite(pool);
                array[0] = result;
            }
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        //only if the size currently is 0, we are going to initialize the localConflictCounter,
        //not before. So the localConflictCounter is set at the latest moment possible. It is
        //very important that this is done before the actual reading since we don't want to loose
        //a conflict.
        if (!hasReads) {
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final RefTranlocal<E> read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        RefTranlocal<E> result = pool.take(ref);
        if (result == null) {
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
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't write fresh object on already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't write fresh object on already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't write fresh object on already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        int index = indexOf(ref);
        if (index >= 0) {
            RefTranlocal<E> result = (RefTranlocal<E>) array[index];

            if (result.isCommitted || result.read != null) {
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

        if (ref.unsafeLoad() != null) {
            abort();
            throw new IllegalArgumentException(
                    format("Can't open for construction a previous committed object on transaction '%s'", config.familyName));
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        //open the tranlocal for writing.
        RefTranlocal<E> result = pool.take(ref);
        if (result == null) {
            result = new RefTranlocal<E>(ref);
        }
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public <E> void commute(Ref<E> ref, BetaObjectPool pool, Function<E> function) {
        throw new TodoException();
    }


    @Override
    public IntRefTranlocal openForRead(
            final IntRef ref, boolean lock, final BetaObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't read from already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't read from already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't read from already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        //a read on a null ref, always return a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if (index >= 0) {
            //we are lucky, at already is attached to the session
            IntRefTranlocal found = (IntRefTranlocal) array[index];

            if (lock) {
                if (!ref.tryLockAndCheckConflict(this, config.spinCount, found)) {
                    throw abortOnReadConflict(pool);
                }
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
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        if (!hasReads) {
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        IntRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if (lock || config.trackReads || !read.isPermanent) {
            array[firstFreeIndex] = read;
            firstFreeIndex++;
        } else {
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public IntRefTranlocal openForWrite(
            final IntRef ref, boolean lock, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't write to already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't write to already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't write to already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                    format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if (index != -1) {
            IntRefTranlocal result = (IntRefTranlocal) array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            if (lock) {
                if (!ref.tryLockAndCheckConflict(this, config.spinCount, result)) {
                    throw abortOnReadConflict(pool);
                }
            }

            //and open it for write if needed.
            if (result.isCommitted) {
                result = result.openForWrite(pool);
                array[0] = result;
            }
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        //only if the size currently is 0, we are going to initialize the localConflictCounter,
        //not before. So the localConflictCounter is set at the latest moment possible. It is
        //very important that this is done before the actual reading since we don't want to loose
        //a conflict.
        if (!hasReads) {
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final IntRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        IntRefTranlocal result = pool.take(ref);
        if (result == null) {
            result = new IntRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final IntRefTranlocal openForConstruction(
            final IntRef ref, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't write fresh object on already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't write fresh object on already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't write fresh object on already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        int index = indexOf(ref);
        if (index >= 0) {
            IntRefTranlocal result = (IntRefTranlocal) array[index];

            if (result.isCommitted || result.read != null) {
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

        if (ref.unsafeLoad() != null) {
            abort();
            throw new IllegalArgumentException(
                    format("Can't open for construction a previous committed object on transaction '%s'", config.familyName));
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        //open the tranlocal for writing.
        IntRefTranlocal result = pool.take(ref);
        if (result == null) {
            result = new IntRefTranlocal(ref);
        }
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public void commute(IntRef ref, BetaObjectPool pool, IntFunction function) {
        throw new TodoException();
    }


    @Override
    public LongRefTranlocal openForRead(
            final LongRef ref, boolean lock, final BetaObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't read from already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't read from already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't read from already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        //a read on a null ref, always return a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if (index >= 0) {
            //we are lucky, at already is attached to the session
            LongRefTranlocal found = (LongRefTranlocal) array[index];

            if (lock) {
                if (!ref.tryLockAndCheckConflict(this, config.spinCount, found)) {
                    throw abortOnReadConflict(pool);
                }
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
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        if (!hasReads) {
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        LongRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if (lock || config.trackReads || !read.isPermanent) {
            array[firstFreeIndex] = read;
            firstFreeIndex++;
        } else {
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public LongRefTranlocal openForWrite(
            final LongRef ref, boolean lock, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't write to already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't write to already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't write to already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                    format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if (index != -1) {
            LongRefTranlocal result = (LongRefTranlocal) array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            if (lock) {
                if (!ref.tryLockAndCheckConflict(this, config.spinCount, result)) {
                    throw abortOnReadConflict(pool);
                }
            }

            //and open it for write if needed.
            if (result.isCommitted) {
                result = result.openForWrite(pool);
                array[0] = result;
            }
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        //only if the size currently is 0, we are going to initialize the localConflictCounter,
        //not before. So the localConflictCounter is set at the latest moment possible. It is
        //very important that this is done before the actual reading since we don't want to loose
        //a conflict.
        if (!hasReads) {
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final LongRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        LongRefTranlocal result = pool.take(ref);
        if (result == null) {
            result = new LongRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final LongRefTranlocal openForConstruction(
            final LongRef ref, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't write fresh object on already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't write fresh object on already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't write fresh object on already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        int index = indexOf(ref);
        if (index >= 0) {
            LongRefTranlocal result = (LongRefTranlocal) array[index];

            if (result.isCommitted || result.read != null) {
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

        if (ref.unsafeLoad() != null) {
            abort();
            throw new IllegalArgumentException(
                    format("Can't open for construction a previous committed object on transaction '%s'", config.familyName));
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        //open the tranlocal for writing.
        LongRefTranlocal result = pool.take(ref);
        if (result == null) {
            result = new LongRefTranlocal(ref);
        }
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public void commute(LongRef ref, BetaObjectPool pool, LongFunction function) {
        throw new TodoException();
    }


    @Override
    public Tranlocal openForRead(
            final BetaTransactionalObject ref, boolean lock, final BetaObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't read from already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't read from already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't read from already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        //a read on a null ref, always return a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if (index >= 0) {
            //we are lucky, at already is attached to the session
            Tranlocal found = (Tranlocal) array[index];

            if (lock) {
                if (!ref.tryLockAndCheckConflict(this, config.spinCount, found)) {
                    throw abortOnReadConflict(pool);
                }
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
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        if (!hasReads) {
            localConflictCounter.reset();
            hasReads = true;
        }

        //none is found in this transaction, lets load it.
        Tranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if (lock || config.trackReads || !read.isPermanent) {
            array[firstFreeIndex] = read;
            firstFreeIndex++;
        } else {
            hasUntrackedReads = true;
        }

        return read;
    }

    @Override
    public Tranlocal openForWrite(
            final BetaTransactionalObject ref, boolean lock, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't write to already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't write to already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't write to already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                    format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if (index != -1) {
            Tranlocal result = (Tranlocal) array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            if (lock) {
                if (!ref.tryLockAndCheckConflict(this, config.spinCount, result)) {
                    throw abortOnReadConflict(pool);
                }
            }

            //and open it for write if needed.
            if (result.isCommitted) {
                result = result.openForWrite(pool);
                array[0] = result;
            }
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        //only if the size currently is 0, we are going to initialize the localConflictCounter,
        //not before. So the localConflictCounter is set at the latest moment possible. It is
        //very important that this is done before the actual reading since we don't want to loose
        //a conflict.
        if (!hasReads) {
            localConflictCounter.reset();
            hasReads = true;
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final Tranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        Tranlocal result = read.openForWrite(pool);
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final Tranlocal openForConstruction(
            final BetaTransactionalObject ref, final BetaObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    abort();
                    throw new PreparedTransactionException(
                            format("Can't write fresh object on already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't write fresh object on already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                            format("Can't write fresh object on already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        int index = indexOf(ref);
        if (index >= 0) {
            Tranlocal result = (Tranlocal) array[index];

            if (result.isCommitted || result.read != null) {
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

        if (ref.unsafeLoad() != null) {
            abort();
            throw new IllegalArgumentException(
                    format("Can't open for construction a previous committed object on transaction '%s'", config.familyName));
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool, array.length + 1);
        }

        //open the tranlocal for writing.
        Tranlocal result = ref.openForConstruction(pool);
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    public void commute(BetaTransactionalObject ref, BetaObjectPool pool, Function function) {
        throw new TodoException();
    }


    /**
     * Finds the index of the tranlocal that has the ref as owner. Return -1 if not found.
     *
     * @param owner the owner of the tranlocal to look for.
     * @return the index of the tranlocal, or -1 if not found.
     */
    private int indexOf(BetaTransactionalObject owner) {
        assert owner != null;

        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];
            if (tranlocal.owner == owner) {
                return k;
            }
        }

        return -1;
    }

    private boolean hasReadConflict() {
        if (config.lockReads) {
            return false;
        }

        if (hasUntrackedReads) {
            return localConflictCounter.syncAndCheckConflict();
        }

        if (firstFreeIndex == 0) {
            return false;
        }

        if (!localConflictCounter.syncAndCheckConflict()) {
            return false;
        }

        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];

            if (tranlocal.owner.hasReadConflict(tranlocal, this)) {
                return true;
            }
        }

        return false;
    }

    // ============================= abort ===================================

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
                final int _firstFreeIndex = firstFreeIndex;

                for (int k = 0; k < _firstFreeIndex; k++) {
                    Tranlocal tranlocal = array[k];
                    //abort could be expensive.
                    tranlocal.owner.abort(this, tranlocal, pool);
                }
                status = ABORTED;
                break;
            case ABORTED:
                break;
            case COMMITTED:
                throw new DeadTransactionException(
                        format("Can't abort already committed transaction '%s'", config.familyName));
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
        if (status != ACTIVE && status != PREPARED) {
            switch (status) {
                case ABORTED:
                    throw new DeadTransactionException(
                            format("Can't commit already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    return;
                default:
                    throw new IllegalStateException();
            }
        }

        Listeners[] listeners = null;

        if (firstFreeIndex > 0) {
            if (config.dirtyCheck) {
                if (status == ACTIVE && !doPrepareDirty()) {
                    throw abortOnWriteConflict(pool);
                }

                listeners = commitDirty(pool);
            } else {
                if (status == ACTIVE && !doPrepareAll()) {
                    throw abortOnWriteConflict(pool);
                }

                listeners = commitAll(pool);
            }
        }

        status = COMMITTED;

        if (listeners != null) {
            Listeners.openAll(listeners, pool);
        }
    }

    private Listeners[] commitAll(final BetaObjectPool pool) {
        Listeners[] listenersArray = null;

        int storeIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];
            Listeners listeners = tranlocal.owner.commitAll(tranlocal, this, pool, config.globalConflictCounter);

            if (listeners != null) {
                if (listenersArray == null) {
                    int length = firstFreeIndex - k;
                    listenersArray = pool.takeListenersArray(length);
                    if (listenersArray == null) {
                        listenersArray = new Listeners[length];
                    }
                }
                listenersArray[storeIndex] = listeners;
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
            Listeners listeners = tranlocal.owner.commitDirty(tranlocal, this, pool, config.globalConflictCounter);

            if (listeners != null) {
                if (listenersArray == null) {
                    int length = firstFreeIndex - k;
                    listenersArray = pool.takeListenersArray(length);
                    if (listenersArray == null) {
                        listenersArray = new Listeners[length];
                    }
                }
                listenersArray[storeIndex] = listeners;
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

        if (firstFreeIndex > 0) {
            if (config.dirtyCheck) {
                if (!doPrepareDirty()) {
                    throw abortOnWriteConflict(pool);
                }
            } else {
                if (!doPrepareAll()) {
                    throw abortOnWriteConflict(pool);
                }
            }
        }

        status = PREPARED;
    }

    private boolean doPrepareAll() {
        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];

            if (!tranlocal.isCommitted) {
                if (!tranlocal.owner.tryLockAndCheckConflict(this, config.spinCount, tranlocal)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean doPrepareDirty() {
        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];

            if (!tranlocal.isCommitted && tranlocal.calculateIsDirty()) {
                if (!tranlocal.owner.tryLockAndCheckConflict(this, config.spinCount, tranlocal)) {
                    return false;
                }
            }
        }

        return true;
    }

    // ============================== registerChangeListenerAndAbort ========================

    @Override
    public void registerChangeListenerAndAbort(final Latch listener) {
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

        if (!config.blockingAllowed) {
            abort();
            throw new NoRetryPossibleException(
                    format("Can't block transaction '%s', since it explicitly is configured as non blockable", config.familyName));
        }

        if (firstFreeIndex == 0) {
            abort();
            throw new NoRetryPossibleException(
                    format("Can't block transaction '%s', since there are no tracked reads", config.familyName));
        }

        final long listenerEra = listener.getEra();

        boolean furtherRegistrationNeeded = true;
        boolean atLeastOneRegistration = false;

        for (int k = 0; k < firstFreeIndex; k++) {

            final Tranlocal tranlocal = array[k];
            final BetaTransactionalObject owner = tranlocal.owner;

            if (furtherRegistrationNeeded) {
                switch (owner.registerChangeListener(listener, tranlocal, pool, listenerEra)) {
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

        status = ABORTED;

        if (!atLeastOneRegistration) {
            throw new NoRetryPossibleException(
                    format("Can't block transaction '%s', since there are no tracked reads", config.familyName));
        }
    }

    // ==================== reset ==============================

    @Override
    public boolean softReset() {
        return softReset(getThreadLocalBetaObjectPool());
    }

    @Override
    public final boolean softReset(final BetaObjectPool pool) {
        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
        }

        if (attempt >= config.getMaxRetries()) {
            return false;
        }

        status = ACTIVE;
        attempt++;
        firstFreeIndex = 0;
        hasReads = false;
        hasUntrackedReads = false;
        return true;
    }

    @Override
    public void hardReset() {
        hardReset(getThreadLocalBetaObjectPool());
    }

    @Override
    public void hardReset(final BetaObjectPool pool) {
        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
        }
        status = ACTIVE;
        hasReads = false;
        hasUntrackedReads = false;
        attempt = 1;
        firstFreeIndex = 0;
        remainingTimeoutNs = config.timeoutNs;
    }

    // ==================== init =============================

    @Override
    public void init(BetaTransactionConfig transactionConfig) {
        init(transactionConfig, getThreadLocalBetaObjectPool());
    }

    @Override
    public void init(BetaTransactionConfig transactionConfig, BetaObjectPool pool) {
        if (transactionConfig == null) {
            abort();
            throw new NullPointerException();
        }

        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
        }

        config = transactionConfig;
        hardReset(pool);
    }

    // ================== orelse ============================

    @Override
    public final void startEitherBranch() {
        startEitherBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void startEitherBranch(BetaObjectPool pool) {
        throw new TodoException();
    }

    @Override
    public final void endEitherBranch() {
        endEitherBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void endEitherBranch(BetaObjectPool pool) {
        throw new TodoException();
    }

    @Override
    public final void startOrElseBranch() {
        startOrElseBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void startOrElseBranch(BetaObjectPool pool) {
        throw new TodoException();
    }
}
