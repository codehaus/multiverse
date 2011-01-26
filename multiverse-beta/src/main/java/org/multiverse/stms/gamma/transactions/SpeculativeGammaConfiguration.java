package org.multiverse.stms.gamma.transactions;

/**
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

    /**
     * Creates a full speculative SpeculativeGammaConfiguration.
     */
    public SpeculativeGammaConfiguration() {
        this(false, false, false, false, false, false, false, false, 1);
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
    }

    public SpeculativeGammaConfiguration newWithMinimalLength(int newMinimalLength) {
        if (newMinimalLength < 0) {
            throw new IllegalArgumentException();
        }

        if (minimalLength >= newMinimalLength) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, isFat, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired, newMinimalLength);
    }

    public SpeculativeGammaConfiguration newWithLocksRequired() {
        if (areLocksDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, isNonRefTypeDetected, isOrelseDetected, true,
                areConstructedObjectsDetected, isRichMansConflictScanRequired, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithConstructedObjectsRequired() {
        if (areConstructedObjectsDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                true, isRichMansConflictScanRequired, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithListenersRequired() {
        if (areListenersDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, isCommuteDetected, true, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithOrElseRequired() {
        if (isOrelseDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, isNonRefTypeDetected, true, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithNonRefType() {
        if (isNonRefTypeDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, true, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithCommuteRequired() {
        if (isCommuteDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, true, true, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, isRichMansConflictScanRequired, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithRichMansConflictScan() {
        if (isRichMansConflictScanRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                areListenersDetected, isCommuteDetected, true, isNonRefTypeDetected, isOrelseDetected, areLocksDetected,
                areConstructedObjectsDetected, true, minimalLength);
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
                ", isFat=" + isFat +
                '}';
    }
}
