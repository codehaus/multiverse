package org.multiverse.utils.latches;

import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;

public class TryAwaitThread extends TestThread {
    private final Latch latch;
    private final long timeout;
    private final TimeUnit unit;
    private Boolean result;

    public TryAwaitThread(Latch latch, long timeout, TimeUnit unit){
        super("TryAwaitThread");
        this.latch = latch;
        this.timeout = timeout;
        this.unit = unit;
    }

    public void doRun() throws InterruptedException {
        result = latch.tryAwait(timeout, unit);
    }

    public Boolean isResult() {
        return result;
    }
}
