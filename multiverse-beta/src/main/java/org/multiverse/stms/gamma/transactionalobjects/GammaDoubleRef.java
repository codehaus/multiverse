package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.DoubleFunction;
import org.multiverse.api.predicates.DoublePredicate;
import org.multiverse.api.references.DoubleRef;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

public final class GammaDoubleRef extends AbstractGammaRef implements DoubleRef {

    public GammaDoubleRef(GammaStm stm) {
        this(stm, 0);
    }

    public GammaDoubleRef(GammaStm stm, double value) {
        super(stm, TYPE_LONG);
    }

    @Override
    public final double set(final double value) {
        return set(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final double set(final Transaction tx, final double value) {
        return set(asGammaTransaction(tx), value);
    }

    public final double set(final GammaTransaction tx, final double value) {
        throw new TodoException();
    }

    @Override
    public final double get() {
        return get(getRequiredThreadLocalGammaTransaction());
    }

    @Override
    public final double get(final Transaction tx) {
        return get(asGammaTransaction(tx));
    }

    public final double get(final GammaTransaction tx) {
        throw new TodoException();
    }

    @Override
    public final double atomicGet() {
        throw new TodoException();
    }

    @Override
    public final double atomicWeakGet() {
        throw new TodoException();
    }

    @Override
    public final double atomicSet(final double newValue) {
        throw new TodoException();
    }

    @Override
    public final double atomicGetAndSet(final double newValue) {
        throw new TodoException();
    }

    @Override
    public final double getAndSet(final double value) {
        return getAndSet(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final double getAndSet(final Transaction tx, final double value) {
        return getAndSet(asGammaTransaction(tx), value);
    }

    public final double getAndSet(final GammaTransaction tx, final double value) {
        throw new TodoException();
    }

    @Override
    public final void commute(final DoubleFunction function) {
        commute(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final void commute(final Transaction tx, final DoubleFunction function) {
        commute(asGammaTransaction(tx), function);
    }

    public final void commute(final GammaTransaction tx, final DoubleFunction function) {
        throw new TodoException();
    }

    @Override
    public final double atomicAlterAndGet(final DoubleFunction function) {
        throw new TodoException();
    }

    @Override
    public final double alterAndGet(final DoubleFunction function) {
        return alterAndGet(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final double alterAndGet(final Transaction tx, final DoubleFunction function) {
        return alterAndGet(asGammaTransaction(tx),function);
    }

    public final double alterAndGet(final GammaTransaction tx, final DoubleFunction function) {
       throw new TodoException();
    }

    @Override
    public final double atomicGetAndAlter(final DoubleFunction function) {
        throw new TodoException();
    }

    @Override
    public final double getAndAlter(final DoubleFunction function) {
        return getAndAlter(getRequiredThreadLocalGammaTransaction(), function);
    }

    @Override
    public final double getAndAlter(final Transaction tx, final DoubleFunction function) {
        return getAndAlter(asGammaTransaction(tx),function);
    }

    public final double getAndAlter(final GammaTransaction tx, final DoubleFunction function){
         throw new TodoException();
    }

    @Override
    public final boolean atomicCompareAndSet(final double expectedValue, final double newValue) {
        throw new TodoException();
    }

    @Override
    public final double atomicGetAndIncrement(final double amount) {
        throw new TodoException();
    }

    @Override
    public final double getAndIncrement(final double amount) {
        return getAndIncrement(getRequiredThreadLocalGammaTransaction(), amount);
    }

    @Override
    public final double getAndIncrement(final Transaction tx, final double amount) {
        return getAndIncrement(asGammaTransaction(tx), amount);
    }

    public final double getAndIncrement(final GammaTransaction tx, final double amount){
        throw new TodoException();
    }

    @Override
    public final double atomicIncrementAndGet(final double amount) {
        throw new TodoException();
    }

    @Override
    public final double incrementAndGet(final double amount) {
        return incrementAndGet(getRequiredThreadLocalGammaTransaction(), amount);
    }

    @Override
    public final double incrementAndGet(final Transaction tx, final double amount) {
        return incrementAndGet(asGammaTransaction(tx),amount);
    }

    public final double incrementAndGet(final GammaTransaction tx, final double amount){
        throw new TodoException();
    }

    @Override
    public final void await(final double value) {
        await(getRequiredThreadLocalGammaTransaction(), value);
    }

    @Override
    public final void await(final Transaction tx, final double value) {
        await(asGammaTransaction(tx),value);
    }

    public final void await(final GammaTransaction tx, final double value){
        throw new TodoException();
    }

    @Override
    public final void await(final DoublePredicate predicate) {
        await(getRequiredThreadLocalGammaTransaction(), predicate);
    }

    @Override
    public final void await(final Transaction tx, final DoublePredicate predicate) {
        await(asGammaTransaction(tx),predicate);
    }

    public final void await(final GammaTransaction tx, final DoublePredicate predicate){
        throw new TodoException();
    }

    @Override
    public String toDebugString() {
        throw new TodoException();
    }
}
