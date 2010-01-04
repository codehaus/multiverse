package org.multiverse.stms.beta;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public class DelayedBetaTranlocal extends BetaTranlocal {

    private List<CommuteTask> ___pendingTasks = new LinkedList<CommuteTask>();

    private boolean ___fixated;

    private final DelayedBetaAtomicObject ___atomicObject;

    private DelayedBetaTranlocal ___origin;

    public int value;

    public DelayedBetaTranlocal(DelayedBetaAtomicObject atomicObject, boolean fixated) {
        this.___atomicObject = atomicObject;
        this.___fixated = fixated;
    }

    public void place(CommuteTask task) {
        if (___fixated) {
            throw new RuntimeException();
        }

        if (task == null) {
            throw new NullPointerException();
        }

        ___pendingTasks.add(task);
    }

    @Override
    public BetaTranlocal cloneForUpdate() {
        return new DelayedBetaTranlocal(___atomicObject, false);
    }

    public boolean ___isFixated() {
        return ___fixated;
    }

    public void fixate(BetaTransaction t) {
        if (___fixated) {
            return;
        }

        ___fixated = true;
        ___origin = (DelayedBetaTranlocal) ___atomicObject.load();
        value = ___origin.value;

        for (CommuteTask task : ___pendingTasks) {
            task.run(t);
        }

        ___pendingTasks.clear();
    }

    @Override
    public BetaTranlocalStatus getStatus() {
        if (___version > 0) {
            return BetaTranlocalStatus.readonly;
        }

        if (!___fixated) {
            return BetaTranlocalStatus.unfixated;
        }

        DelayedBetaTranlocal current = (DelayedBetaTranlocal) ___atomicObject.load();

        if (current == null) {
            return BetaTranlocalStatus.fresh;
        }

        if (current != ___origin) {
            return BetaTranlocalStatus.conflict;
        }

        if (value != ___origin.value) {
            return BetaTranlocalStatus.dirty;
        }

        return BetaTranlocalStatus.clean;
    }
}
