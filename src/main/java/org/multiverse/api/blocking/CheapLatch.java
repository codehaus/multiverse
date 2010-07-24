package org.multiverse.api.blocking;

import static java.lang.String.format;

/**
 * @author Peter Veentjer
 */
public final class CheapLatch implements Latch {

    private volatile long era = Long.MIN_VALUE;
    private volatile boolean isOpen = false;

    @Override
    public void open(final long expectedEra) {
        if (isOpen && expectedEra == era) {
            return;
        }

        synchronized (this) {
            if (expectedEra != era) {
                return;
            }

            isOpen = true;
            notifyAll();
        }
    }

    @Override
    public void await(final long expectedEra) {
        if (isOpen || expectedEra != era) {
            return;
        }

        for(int k=0;k<150;k++){
            if(isOpen){
                return;
            }
        }

        boolean restoreInterrupt = false;

        synchronized (this) {
            while (!isOpen && era == expectedEra) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    restoreInterrupt = true;
                }
            }
        }

        if (restoreInterrupt) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public long getEra() {
        return era;
    }

    @Override
    public long reset() {
        long newEra;

        synchronized (this) {
            isOpen = false;

            era++;
            newEra = era;
        }

        return newEra;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public String toString() {
        return format("CheapLatch(open=%s)", isOpen);
    }
}
