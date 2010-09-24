package org.multiverse.sensors;

import org.multiverse.utils.ToolUnsafe;
import sun.misc.Unsafe;

import static java.lang.String.format;

public final class StripedCounter {

    private static final long serialVersionUID = -2308431214976778248L;

    // setup to use Unsafe.compareAndSwapInt for updates
    private static final Unsafe unsafe = ToolUnsafe.getUnsafe();
    private static final int base = unsafe.arrayBaseOffset(long[].class);
    private static final int scale = unsafe.arrayIndexScale(long[].class);
    private final long[] array;

    public long rawIndex(int i) {
        if (i < 0 || i >= array.length)
            throw new IndexOutOfBoundsException("index " + i);
        return base + (long) i * scale;
    }

    /**
     * Creates a new AtomicLongArray of given length.
     *
     * @param length the length of the array
     */
    public StripedCounter(int length) {
        array = new long[length];
        // must perform at least one volatile write to conform to JMM
        if (length > 0)
            unsafe.putLongVolatile(array, rawIndex(0), 0);
    }

    public long get() {
        long result = 0;

        for (int k = 0; k < array.length; k++) {
            result += unsafe.getLongVolatile(array, rawIndex(k));
        }
        return result;
    }

    public void inc(int random, long count) {
        if (count == 0) {
            return;
        }

        int index = (int) ((Math.abs(random + count)) % array.length);

        while (true) {
            final long rawIndex = rawIndex(index);
            final long current = unsafe.getLongVolatile(array, rawIndex);
            final long next = current + count;

            if (unsafe.compareAndSwapLong(array, rawIndex, current, next)) {
                return;
            }

            index = (int) (Math.abs(random + index + next) % array.length);

            if (index < 0) {
                throw new RuntimeException();
            }
        }
    }

    public void inc(long count) {
        if (count == 0) {
            return;
        }

        int index = (int) ((Math.abs(count)) % array.length);

        while (true) {
            final long rawIndex = rawIndex(index);
            final long current = unsafe.getLongVolatile(array, rawIndex);
            final long next = current + count;

            if (unsafe.compareAndSwapLong(array, rawIndex, current, next)) {
                return;
            }

            index = (int) (Math.abs(index + next) % array.length);

            if (index < 0) {
                throw new RuntimeException();
            }
        }
    }

    public void incAtIndex(int index, long count) {
        if (count == 0) {
            return;
        }

        index = index * 128;
        final long rawIndex = rawIndex(index);
        while (true) {
            final long current = unsafe.getLongVolatile(array, rawIndex);
            final long next = current + count;

            if (unsafe.compareAndSwapLong(array, rawIndex, current, next)) {
                return;
            }
        }
    }


    @Override
    public String toString() {
        return format("StripedCounter(value=%s, width=%s)", get(), array.length);
    }
}
