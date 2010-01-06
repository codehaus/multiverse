package org.multiverse.utils.latches;

import org.multiverse.TestThread;

public class AwaitThread extends TestThread {
    private final Latch latch;

    public AwaitThread(Latch latch){
        this(latch,false);
    }

    public AwaitThread(Latch latch, boolean startInterrupted) {
        super("AwaitThread",startInterrupted);
        this.latch = latch;
    }

    public void doRun() throws InterruptedException {
        latch.await();
    }
}
