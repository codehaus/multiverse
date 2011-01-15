package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.BooleanFunction;
import org.multiverse.api.predicates.BooleanPredicate;
import org.multiverse.api.references.BooleanRef;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

public final class GammaBooleanRef extends AbstractGammaRef implements BooleanRef {

    public GammaBooleanRef(GammaStm stm) {
        this(stm, false);
    }

    public GammaBooleanRef(GammaStm stm, boolean b) {
        super(stm, -1);
        throw new TodoException();
    }

    @Override
    public final boolean set(final boolean value) {
        return set(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final boolean set(final Transaction tx, final boolean value) {
        return set(asGammaTransaction(tx), value);
    }

    public final boolean set(final GammaTransaction tx, final boolean value) {
        throw new TodoException();
    }

    @Override
    public final boolean get() {
        return get(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public final boolean get(final Transaction tx) {
        return get(asGammaTransaction(tx));
    }

    public final boolean get(final GammaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public final boolean atomicGet() {
        throw new TodoException();
    }

    @Override
    public final boolean atomicWeakGet() {
        throw new TodoException();
    }

    @Override
    public final boolean atomicSet(final boolean newValue) {
        throw new TodoException();
    }

    @Override
    public final boolean atomicGetAndSet(final boolean newValue) {
        throw new TodoException();
    }

    @Override
    public final boolean getAndSet(final boolean value) {
        return getAndSet(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final boolean getAndSet(final Transaction tx, final boolean value) {
        return getAndSet(asGammaTransaction(tx), value);
    }

    public final boolean getAndSet(final GammaTransaction tx, final boolean value) {
        throw new TodoException();
    }

    @Override
    public final void commute(final BooleanFunction function) {
        commute(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final void commute(final Transaction tx, final BooleanFunction function) {
        commute(asGammaTransaction(tx), function);
    }

    public final void commute(final GammaTransaction tx, final BooleanFunction function){
        throw new TodoException();
    }

    @Override
    public final boolean atomicAlterAndGet(final BooleanFunction function) {
        throw new TodoException();
    }

    @Override
    public final boolean alterAndGet(final BooleanFunction function) {
        return alterAndGet(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final boolean alterAndGet(final Transaction tx, final BooleanFunction function) {
        return alterAndGet(asGammaTransaction(tx),function);
    }

    public final boolean alterAndGet(final GammaTransaction tx, final BooleanFunction function){
        throw new TodoException();
    }

    @Override
    public final boolean atomicGetAndAlter(final BooleanFunction function) {
        throw new TodoException();
    }

    @Override
    public final boolean getAndAlter(final BooleanFunction function) {
        return getAndAlter(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final boolean getAndAlter(final Transaction tx, final BooleanFunction function) {
        return getAndAlter(asGammaTransaction(tx), function);
    }

    public final boolean getAndAlter(final GammaTransaction tx, final BooleanFunction function) {
        throw new TodoException();
    }

    @Override
    public final boolean atomicCompareAndSet(final boolean expectedValue, final boolean newValue) {
        throw new TodoException();
    }

    @Override
    public final void await(final boolean value) {
        await(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final void await(final Transaction tx, final boolean value) {
        await(asGammaTransaction(tx), value);
    }

    public final void await(final GammaTransaction tx, final boolean value) {
        throw new TodoException();
    }

    @Override
    public final void await(final BooleanPredicate predicate) {
        await(getRequiredThreadLocalGammaTransaction(), predicate);
    }

    @Override
    public final void await(final Transaction tx, final BooleanPredicate predicate) {
        await(asGammaTransaction(tx), predicate);
    }

    public final void await(final GammaTransaction tx, final BooleanPredicate predicate) {
        throw new TodoException();
    }

    @Override
    public final String toDebugString() {
        throw new TodoException();
    }
}
