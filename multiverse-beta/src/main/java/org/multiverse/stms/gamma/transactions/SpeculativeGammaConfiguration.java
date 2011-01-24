package org.multiverse.stms.gamma.transactions;

public final class SpeculativeGammaConfiguration {

    public final boolean areListenersRequired;
    public final int minimalLength;
    public final boolean isCommuteRequired;
    public final boolean isOrelseRequired;
    public final boolean isNonRefTypeRequired;
    public final boolean isFat;
    public final boolean areLocksRequired;
    public final boolean areConstructedObjectsRequired;
    public final boolean isRichMansConflictScanRequired;

    public SpeculativeGammaConfiguration(boolean fat) {
        this(fat, fat, fat, fat, fat, fat, fat, fat, fat ? Integer.MAX_VALUE : 1);
    }

    public SpeculativeGammaConfiguration(
            final boolean fat,
            final boolean areListenersRequired,
            final boolean isCommuteRequired,
            final boolean isNonRefTypeRequired,
            final boolean isOrelseRequired,
            final boolean areLocksRequired,
            final boolean areConstructedObjectsRequired,
            final boolean isRichMansConflictScanRequired,
            final int minimalLength) {

        if (minimalLength < 0) {
            throw new IllegalArgumentException();
        }
        this.isNonRefTypeRequired = isNonRefTypeRequired;
        this.areListenersRequired = areListenersRequired;
        this.minimalLength = minimalLength;
        this.isCommuteRequired = isCommuteRequired;
        this.isOrelseRequired = isOrelseRequired;
        this.isFat = fat;
        this.areLocksRequired = areLocksRequired;
        this.isRichMansConflictScanRequired = isRichMansConflictScanRequired;
        this.areConstructedObjectsRequired = areConstructedObjectsRequired;
    }

    public SpeculativeGammaConfiguration createWithMinimalLength(int newMinimalLength) {
        if (newMinimalLength < 0) {
            throw new IllegalArgumentException();
        }

        if (minimalLength >= newMinimalLength) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                isFat,
                areListenersRequired,
                isCommuteRequired,
                isNonRefTypeRequired,
                isOrelseRequired,
                areLocksRequired,
                areConstructedObjectsRequired,
                isRichMansConflictScanRequired,
                newMinimalLength
        );
    }

    public SpeculativeGammaConfiguration createWithLocksRequired() {
        if (areLocksRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                areListenersRequired,
                isCommuteRequired,
                isNonRefTypeRequired,
                isOrelseRequired,
                true,
                areConstructedObjectsRequired,
                isRichMansConflictScanRequired,
                minimalLength
        );
    }

    public SpeculativeGammaConfiguration createWithConstructedObjectsRequired() {
        if (areConstructedObjectsRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                areListenersRequired,
                isCommuteRequired,
                isNonRefTypeRequired,
                isOrelseRequired,
                areLocksRequired,
                true,
                isRichMansConflictScanRequired,
                minimalLength
        );
    }

    public SpeculativeGammaConfiguration createWithListenersRequired() {
        if (areListenersRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                true,
                isCommuteRequired,
                isNonRefTypeRequired,
                isOrelseRequired,
                areLocksRequired,
                areConstructedObjectsRequired,
                isRichMansConflictScanRequired,
                minimalLength
        );
    }

    public SpeculativeGammaConfiguration createWithOrElseRequired() {
        if (isOrelseRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                areListenersRequired,
                isCommuteRequired,
                isNonRefTypeRequired,
                true,
                areLocksRequired,
                areConstructedObjectsRequired,
                isRichMansConflictScanRequired,
                minimalLength);
    }

    public SpeculativeGammaConfiguration createWithNonRefType() {
        if (isNonRefTypeRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                areListenersRequired,
                isCommuteRequired,
                true,
                isOrelseRequired,
                areLocksRequired,
                areConstructedObjectsRequired,
                isRichMansConflictScanRequired,
                minimalLength
        );
    }

    public SpeculativeGammaConfiguration createWithCommuteRequired() {
        if (isCommuteRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                areListenersRequired,
                true,
                isNonRefTypeRequired,
                isOrelseRequired,
                areLocksRequired,
                areConstructedObjectsRequired,
                isRichMansConflictScanRequired,
                minimalLength
        );
    }

    public SpeculativeGammaConfiguration createWithRichMansConflictScan() {
        if (isRichMansConflictScanRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                areListenersRequired,
                isCommuteRequired,
                isNonRefTypeRequired,
                isOrelseRequired,
                areLocksRequired,
                areConstructedObjectsRequired,
                true,
                minimalLength
        );
    }

    @Override
    public String toString() {
        return "SpeculativeGammaConfiguration{" +
                "areListenersRequired=" + areListenersRequired +
                ", isCommuteRequired=" + isCommuteRequired +
                ", isNonRefTypeRequired=" + isNonRefTypeRequired +
                ", isOrelseRequired=" + isOrelseRequired +
                ", areLocksRequired=" + areLocksRequired +
                ", minimalLength=" + minimalLength +
                ", richMansConflictScan=" + isRichMansConflictScanRequired +
                ", constructedObjectsRequired=" + areConstructedObjectsRequired +
                ", isFat=" + isFat +
                '}';
    }


}
