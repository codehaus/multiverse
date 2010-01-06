package org.multiverse.utils.latches;

import org.multiverse.TestThread;

public class AwaitUninterruptibleThread extends TestThread {

    private final Latch latch;

    public AwaitUninterruptibleThread(Latch latch){
        this(latch, false);
    }

    public AwaitUninterruptibleThread(Latch latch, boolean startInterrupted) {
        super("AwaitUninterruptibleThread", startInterrupted);
        this.latch = latch;
    }

    public void doRun() {
        latch.awaitUninterruptible();
    }
}
