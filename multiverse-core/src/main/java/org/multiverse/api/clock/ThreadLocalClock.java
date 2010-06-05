package org.multiverse.api.clock;

/**
 * Contains the threadlocal version. This is needed to use the TL2 V5/V6 optimization.
 *
 * The advantage of this optimization is that instead of transaction pounding on the central
 * clock, the clock only is increased locally. This improves scalability but can lead to a
 * higher level of conflicts (with the v6 optimization the level of conflicts can be regulated).
 *
 * @author Peter Veentjer
 */
public final class ThreadLocalClock {

    public final static ThreadLocal<LocalClock> threadlocal = new ThreadLocal<LocalClock>() {
        @Override
        protected LocalClock initialValue() {
            return new LocalClock();
        }
    };

    /**
     * Returns the ThreadLocal Clock.
     *
     * @return the threadlocal clock.
     */
    public static LocalClock getThreadLocalClock() {
        return threadlocal.get();
    }

    //we don't want any instances.

    private ThreadLocalClock() {
    }

    public final static class LocalClock {

        private long version = 1;
        private int unsynced = 0;

        public long getVersion() {
            return version;
        }

        public void set(long update) {
            assert update>=0;
            this.version = update;
        }

        public void incUnsynced(){
            unsynced++;
        }

        public void resetUnsynced(){
            unsynced = 0;
        }

        public int getUnsynced(){
            return unsynced;
        }
    }
}
