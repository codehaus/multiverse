package org.multiverse.instrumentation.metadata;

import java.util.Arrays;

public final class Multikey {
    private final Object[] keys;

    public Multikey(Object... keys) {
        this.keys = keys;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Multikey(");
        for (int k = 0; k < keys.length; k++) {
            sb.append(keys[k] == null ? "null" : keys[k].toString());
            if (k < keys.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public int hashCode() {
        int hashCode = 0;
        for (Object key : keys) {
            hashCode = (hashCode * 31) + (key == null ? 0 : key.hashCode());
        }
        return hashCode;
    }

    public boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof Multikey)) {
            return false;
        }

        Multikey that = (Multikey) thatObj;
        return Arrays.equals(that.keys, this.keys);
    }
}
