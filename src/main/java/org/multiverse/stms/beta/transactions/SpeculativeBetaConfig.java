package org.multiverse.stms.beta.transactions;


/**
 * Transaction configuration that contains the speculative settings for the BetaTransactionConfiguration.
 * This object is immutable.
 */
public final class SpeculativeBetaConfig {

    private final boolean listenersRequired;
    private final int minimalLength;
    private final boolean commuteRequired;
    private final boolean orelseRequired;

    public SpeculativeBetaConfig() {
        this(false, false, 1, false);
    }

    public SpeculativeBetaConfig(boolean listenersRequired, boolean commuteRequired, int minimalLength, boolean orelseRequired) {
        if (minimalLength < 0) {
            throw new IllegalArgumentException();
        }
        this.listenersRequired = listenersRequired;
        this.minimalLength = minimalLength;
        this.commuteRequired = commuteRequired;
        this.orelseRequired = orelseRequired;
    }

    public boolean isOrelseRequired() {
        return orelseRequired;
    }

    public boolean isListenerRequired() {
        return listenersRequired;
    }

    public int getMinimalLength() {
        return minimalLength;
    }

    public boolean isCommuteRequired() {
        return commuteRequired;
    }

    public SpeculativeBetaConfig createWithMinimalLength(int newMinimalLength) {
        if (newMinimalLength < 0) {
            throw new IllegalArgumentException();
        }

        if (minimalLength >= newMinimalLength) {
            return this;
        }

        return new SpeculativeBetaConfig(listenersRequired,
                commuteRequired,
                newMinimalLength,
                orelseRequired);
    }

    public SpeculativeBetaConfig createWithListenersRequired() {
        if (listenersRequired) {
            return this;
        }

        return new SpeculativeBetaConfig(true,
                commuteRequired,
                minimalLength,
                orelseRequired);
    }

    public SpeculativeBetaConfig createWithOrElseRequired() {
        if (orelseRequired) {
            return this;
        }

        return new SpeculativeBetaConfig(listenersRequired,
                listenersRequired,
                minimalLength,
                true);
    }

    public SpeculativeBetaConfig createWithCommuteRequired() {
        if (commuteRequired) {
            return this;
        }

        return new SpeculativeBetaConfig(listenersRequired,
                true,
                minimalLength,
                orelseRequired);
    }

    @Override
    public String toString() {
        return "SpeculativeBetaConfig{" +
                "listenersRequired=" + listenersRequired +
                ", commuteRequired=" + commuteRequired +
                ", orelseRequired=" + orelseRequired +
                ", minimalLength=" + minimalLength +
                '}';
    }
}
