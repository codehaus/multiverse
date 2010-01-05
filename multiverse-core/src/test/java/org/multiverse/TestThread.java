package org.multiverse;

import static org.junit.Assert.assertNull;

/**
 * A TestThread that tracks if any throwable has been thrown by a thread.
 *
 * @author Peter Veentjer.
 */
public abstract class TestThread extends Thread {

    private volatile Throwable throwable;
    private volatile Boolean endedWithInterruptStatus;
    private final boolean startInterrupted;

    public TestThread(){
        this("TestThread");
    }

    public TestThread(String name) {
        this(name,false);
    }

    public TestThread(String name, boolean startInterrupted){
        super(name);
        this.startInterrupted = startInterrupted;
   }

    public boolean doesStartInterrupted() {
        return startInterrupted;
    }

    public Boolean hasEndedWithInterruptStatus() {
        return endedWithInterruptStatus;
    }

    @Override
    public final void run(){
        if(startInterrupted){
            interrupt();
        }
        
        try{
            doRun();
        }catch(Throwable ex){
            System.out.printf("Thread %s has thrown an exception\n", getName());
            ex.printStackTrace();
            this.throwable = ex;
        }finally{
            endedWithInterruptStatus = isInterrupted();
        }
    }

    public abstract void doRun()throws Exception;

    public Throwable getThrowable() {
        return throwable;
    }

    public void assertNothingThrown() {
        assertNull(throwable);
    }

}
