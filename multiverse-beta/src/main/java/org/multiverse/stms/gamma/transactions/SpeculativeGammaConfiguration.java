package org.multiverse.stms.gamma.transactions;

/**
 * The GammaStm uses a speculative mechanism (if enabled) to learn from executing transactions. Transactions start
 * cheap and with a lot of features disabled, but once the speculation failed, the SpeculativeGammaConfguration
 * is 'updated'.
 *
 * This class is immutable.
 *
 * @author Peter Veentjer.
 */
public final class SpeculativeGammaConfiguration {

    public final boolean areListenersDetected;
    public final int minimalLength;
    public final boolean isCommuteDetected;
    public final boolean isOrelseDetected;
    public final boolean isNonRefTypeDetected;
    public final boolean isFat;
    public final boolean areLocksDetected;
    public final boolean areConstructedObjectsDetected;
    public final boolean isRichMansConflictScanRequired;
    public final boolean isAbortOnlyDetected;

    /**
     * Creates a full speculative SpeculativeGammaConfiguration.
     */
    public SpeculativeGammaConfiguration() {
        this(false, false, false, false, false, false, false, false,false, 1);
    }

    public SpeculativeGammaConfiguration(
            final boolean areListenersDetected,
            final boolean isCommuteDetected,
            final boolean isFat,
            final boolean isNonRefTypeDetected,
            final boolean isOrelseDetected,
            final boolean areLocksDetected,
            final boolean areConstructedObjectsDetected,
            final boolean isRichMansConflictScanRequired,
            final boolean isAbortOnlyDetected,
            final int minimalLength) {

        if (minimalLength < 0) {
            throw new IllegalArgumentException();
        }

        this.isFat = isFat;
        this.areConstructedObjectsDetected = areConstructedObjectsDetected;
        this.areListenersDetected = areListenersDetected;
        this.areLocksDetected = areLocksDetected;
        this.isCommuteDetected = isCommuteDetected;
        this.isRichMansConflictScanRequired = isRichMansConflictScanRequired;
        this.isNonRefTypeDetected = isNonRefTypeDetected;
        this.isOrelseDetected = isOrelseDetected;
        this.minimalLength = minimalLength;
        this.isAbortOnlyDetected = isAbortOnlyDetected;
    }

    public SpeculativeGammaConfiguration newWithMinimalLength(int newMinimalLength) {
        if (newMinimalLength < 0) {
            throw new IllegalArgumentException();
        }

        if (minimalLength >= newMinimalLength) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, isFat, isNonRefTypeDetected, isOrelseDetected,
                areLocksDetected, areConstructedObjectsDetected, isRichMansConflictScanRequired,
                isAbortOnlyDetected, newMinimalLength);
    }

    public SpeculativeGammaConfiguration newWithLocksRequired() {
        if (areLocksDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, isNonRefTypeDetected, isOrelseDetected, true,
                areConstructedObjectsDetected, isRichMansConflictScanRequired,isAbortOnlyDetected, minimalLength);
    }

      public SpeculativeGammaConfiguration newWithAbortOnly() {
        if (isAbortOnlyDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired,true, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithConstructedObjectsRequired() {
        if (areConstructedObjectsDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                true, isRichMansConflictScanRequired,isAbortOnlyDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithListenersRequired() {
        if (areListenersDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, isCommuteDetected, true, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired,isAbortOnlyDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithOrElseRequired() {
        if (isOrelseDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, isNonRefTypeDetected, true, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired,isAbortOnlyDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithNonRefType() {
        if (isNonRefTypeDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, true, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired,isAbortOnlyDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithCommuteRequired() {
        if (isCommuteDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, true, true, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired,isAbortOnlyDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithRichMansConflictScan() {
        if (isRichMansConflictScanRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, true,isAbortOnlyDetected, minimalLength);
    }

    @Override
    public String toString() {
        return "SpeculativeGammaConfiguration{" +
                "areListenersRequired=" + areListenersDetected +
                ", isCommuteDetected=" + isCommuteDetected +
                ", isNonRefTypeDetected=" + isNonRefTypeDetected +
                ", isOrelseDetected=" + isOrelseDetected +
                ", isOrelseDetected=" + isOrelseDetected +
                ", minimalLength=" + minimalLength +
                ", isRichMansConflictScanDetected=" + isRichMansConflictScanRequired +
                ", areConstructedObjectsDetected=" + areConstructedObjectsDetected +
                ", isAbortOnlyDetected=" + isAbortOnlyDetected +
                ", isFat=" + isFat +
                '}';
    }
}
