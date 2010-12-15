package org.multiverse.actors;

/**
 * @author Peter Veentjer
 */
public abstract class Actor {

    private final ImprovedBlockingQueue mailbox = new ImprovedBlockingQueue(1000);
    private final Thread thread = new WorkerThread();
    private final String SHUTDOWN = "shutdown";

    public final void start() {
        thread.start();
    }

    public final void send(Object item) throws InterruptedException {
        mailbox.put(item);
    }

    ImprovedBlockingQueue.PutClosure putClosure = mailbox.createPutClosure();

    public final void sendHack(Object item) throws InterruptedException {
        putClosure.item = item;
        mailbox.put(putClosure);
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
                    } else {
                        process(item);
                    }
                } while (again);
            } catch (InterruptedException e) {
                //we are done
            }
        }
    }
}
