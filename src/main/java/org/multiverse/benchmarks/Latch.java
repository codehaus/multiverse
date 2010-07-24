package org.multiverse.benchmarks;

/**
 * @author Peter Veentjer
 */
public final class Latch {

    private boolean isOpen = false;

    public void open(){
        synchronized (this){
            isOpen=true;
            notifyAll();
        }
    }

    public void await(){
        synchronized (this){
            while(!isOpen){
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
