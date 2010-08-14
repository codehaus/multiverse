package org.multiverse.actors;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author Peter Veentjer
 */
public abstract class Actor {

    private final BlockingQueue mailbox = new ArrayBlockingQueue(1000, false);
    private final Thread thread = new WorkerThread();
    private final String SHUTDOWN = "shutdown";

    public final void start() {
        thread.start();
    }

    public final void send(Object item) throws InterruptedException {
        mailbox.put(item);
    }

    public final void shutdown() throws InterruptedException {
        mailbox.put(SHUTDOWN);
        thread.join();
    }

    public abstract void process(Object item);

    private class WorkerThread extends Thread {

        @Override
        public void run() {

            try {
                boolean again = true;
                do {
                    Object item = mailbox.take();
                    if (item == SHUTDOWN) {
                        again = false;
                    }else{
                        process(item);
                    }
                } while (again);
            } catch (InterruptedException e) {
                //we are done
            }
        }
    }
}
