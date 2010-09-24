package org.multiverse.stms.beta.transactions;


/**
 * Transaction configuration that contains the speculative settings for the BetaTransactionConfiguration.
 * This object is immutable.
 */
public final class SpeculativeBetaConfiguration {

    public final boolean areListenersRequired;
    public final int minimalLength;
    public final boolean isCommuteRequired;
    public final boolean isOrelseRequired;
    public final boolean isFat;

    public SpeculativeBetaConfiguration(boolean fat) {
        this(fat, false, false, 1, false);
    }

    public SpeculativeBetaConfiguration(
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

    public SpeculativeBetaConfiguration createWithMinimalLength(int newMinimalLength) {
        if (newMinimalLength < 0) {
            throw new IllegalArgumentException();
        }

        if (minimalLength >= newMinimalLength) {
            return this;
        }

        return new SpeculativeBetaConfiguration(
                isFat,
                areListenersRequired,
                isCommuteRequired,
                newMinimalLength,
                isOrelseRequired);
    }

    public SpeculativeBetaConfiguration createWithListenersRequired() {
        if (areListenersRequired) {
            return this;
        }

        return new SpeculativeBetaConfiguration(
                true,
                true,
                isCommuteRequired,
                minimalLength,
                isOrelseRequired);
    }

    public SpeculativeBetaConfiguration createWithOrElseRequired() {
        if (isOrelseRequired) {
            return this;
        }

        return new SpeculativeBetaConfiguration(
                true,
                areListenersRequired,
                areListenersRequired,
                minimalLength,
                true);
    }

    public SpeculativeBetaConfiguration createWithCommuteRequired() {
        if (isCommuteRequired) {
            return this;
        }

        return new SpeculativeBetaConfiguration(
                true,
                areListenersRequired,
                true,
                minimalLength,
                isOrelseRequired);
    }

    @Override
    public String toString() {
        return "SpeculativeBetaConfiguration{" +
                "areListenersRequired=" + areListenersRequired +
                ", isCommuteRequired=" + isCommuteRequired +
                ", isOrelseRequired=" + isOrelseRequired +
                ", minimalLength=" + minimalLength +
                ", isFat=" + isFat +
                '}';
    }
}
