package org.multiverse.stms.gamma.transactions;

public final class SpeculativeGammaConfiguration {

    public final boolean areListenersRequired;
    public final int minimalLength;
    public final boolean isCommuteRequired;
    public final boolean isOrelseRequired;
    public final boolean isFat;

    public SpeculativeGammaConfiguration(boolean fat) {
        this(fat, false, false, 1, false);
    }

    public SpeculativeGammaConfiguration(
            final boolean fat,
            final boolean areListenersRequired,
            final boolean commuteRequired,
            final int minimalLength,
            final boolean orelseRequired) {

        if (minimalLength < 0) {
            throw new IllegalArgumentException();
        }
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
                newMinimalLength,
                isOrelseRequired);
    }

    public SpeculativeGammaConfiguration createWithListenersRequired() {
        if (areListenersRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                true,
                isCommuteRequired,
                minimalLength,
                isOrelseRequired);
    }

    public SpeculativeGammaConfiguration createWithOrElseRequired() {
        if (isOrelseRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                areListenersRequired,
                areListenersRequired,
                minimalLength,
                true);
    }

    public SpeculativeGammaConfiguration createWithCommuteRequired() {
        if (isCommuteRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true,
                areListenersRequired,
                true,
                minimalLength,
                isOrelseRequired);
    }

    @Override
    public String toString() {
        return "SpeculativeGammaConfiguration{" +
                "areListenersRequired=" + areListenersRequired +
                ", isCommuteRequired=" + isCommuteRequired +
                ", isOrelseRequired=" + isOrelseRequired +
                ", minimalLength=" + minimalLength +
                ", isFat=" + isFat +
                '}';
    }
}
