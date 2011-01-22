package org.multiverse.stms.gamma.transactions;

public final class SpeculativeGammaConfiguration {

    public final boolean areListenersRequired;
    public final int minimalLength;
    public final boolean isCommuteRequired;
    public final boolean isOrelseRequired;
    public final boolean isNonRefTypeRequired;
    public final boolean isFat;
    public final boolean areLocksRequired;
    public final boolean constructedObjectsRequired;

    public SpeculativeGammaConfiguration(boolean fat) {
        this(fat, fat, fat, fat, fat, fat, fat, fat ? Integer.MAX_VALUE : 1);
    }

    public SpeculativeGammaConfiguration(
            final boolean fat,
            final boolean areListenersRequired,
            final boolean commuteRequired,
            final boolean nonRefTypeRequired,
            final boolean orelseRequired,
            final boolean areLocksRequired,
            final boolean constructedObjectsRequired,
            final int minimalLength) {

        if (minimalLength < 0) {
            throw new IllegalArgumentException();
        }
        this.isNonRefTypeRequired = nonRefTypeRequired;
        this.areListenersRequired = areListenersRequired;
        this.minimalLength = minimalLength;
        this.isCommuteRequired = commuteRequired;
        this.isOrelseRequired = orelseRequired;
        this.isFat = fat;
        this.areLocksRequired = areLocksRequired;
        this.constructedObjectsRequired = constructedObjectsRequired;
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
                constructedObjectsRequired,
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
                constructedObjectsRequired,
                minimalLength
        );
    }

    public SpeculativeGammaConfiguration createWithConstructedObjectsRequired() {
        if (constructedObjectsRequired) {
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
                constructedObjectsRequired,
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
                constructedObjectsRequired,
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
                constructedObjectsRequired,
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
                constructedObjectsRequired,
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
                ", constructedObjectsRequired=" + constructedObjectsRequired +
                ", isFat=" + isFat +
                '}';
    }
}
