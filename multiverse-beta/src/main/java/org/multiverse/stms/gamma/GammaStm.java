package org.multiverse.stms.gamma;

import org.multiverse.api.BackoffPolicy;
import org.multiverse.api.OrElseBlock;
import org.multiverse.api.Stm;
import org.multiverse.api.collections.TransactionalCollectionsFactory;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.references.*;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.ArrayGammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactoryBuilder;

public final class GammaStm implements Stm {

    public final int defaultMaxRetries;
    public final int spinCount;
    public final BackoffPolicy defaultBackoffPolicy;

    public final GlobalConflictCounter globalConflictCounter = new GlobalConflictCounter();
    public final GammaRefFactory defaultRefFactory = new GammaRefFactory();

    public GammaStm() {
        this(new GammaStmConfiguration());
    }

    public GammaStm(GammaStmConfiguration config) {
        this.defaultMaxRetries = config.maxRetries;
        this.spinCount = config.spinCount;
        this.defaultBackoffPolicy = config.backoffPolicy;
    }

    @Override
    public GammaTransaction startDefaultTransaction() {
        //todo: correct transaction needs to be selected here
        return new ArrayGammaTransaction(this);
    }

    @Override
    public GammaAtomicBlock getDefaultAtomicBlock() {
        throw new TodoException();
    }

    @Override
    public OrElseBlock createOrElseBlock() {
        throw new TodoException();
    }

    @Override
    public GammaRefFactory getDefaultRefFactory() {
        return defaultRefFactory;
    }

    class GammaRefFactory implements RefFactory {
        @Override
        public <E> Ref<E> newRef(E value) {
            throw new TodoException();
        }

        @Override
        public IntRef newIntRef(int value) {
            throw new TodoException();
        }

        @Override
        public BooleanRef newBooleanRef(boolean value) {
            throw new TodoException();
        }

        @Override
        public DoubleRef newDoubleRef(double value) {
            throw new TodoException();
        }

        @Override
        public GammaLongRef newLongRef(long value) {
            return new GammaLongRef(GammaStm.this, value);
        }
    }

    @Override
    public GammaTransactionFactoryBuilder createTransactionFactoryBuilder() {
        throw new TodoException();
    }

    @Override
    public TransactionalCollectionsFactory getDefaultTransactionalCollectionFactory() {
        throw new TodoException();
    }

    @Override
    public RefFactoryBuilder getRefFactoryBuilder() {
        throw new TodoException();
    }
}
