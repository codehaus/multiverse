package org.multiverse.stms.gamma.transactions;

public final class SpeculativeGammaConfiguration {

    public final boolean areListenersRequired;
    public final int minimalLength;
    public final boolean isCommuteRequired;
    public final boolean isOrelseRequired;
    public final boolean isNonRefTypeRequired;
    public final boolean isFat;

    public SpeculativeGammaConfiguration(boolean fat) {
        this(fat, false, false, false, false, 1);
    }

    public SpeculativeGammaConfiguration(
            final boolean fat,
            final boolean areListenersRequired,
            final boolean commuteRequired,
            final boolean nonRefTypeRequired,
            final boolean orelseRequired,
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
                newMinimalLength
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
                ", minimalLength=" + minimalLength +
                ", isFat=" + isFat +
                '}';
    }
}
