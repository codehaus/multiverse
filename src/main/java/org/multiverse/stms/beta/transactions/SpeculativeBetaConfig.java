package org.multiverse.stms.beta.transactions;


/**
 * Transaction configuration that contains the speculative settings for the BetaTransactionConfig.
 * This object is immutable.
 */
public final class SpeculativeBetaConfig {

    private final boolean listenersRequired;
    private final int minimalLength;
    private final boolean commuteRequired;

    public SpeculativeBetaConfig() {
        this(false, false, 1);
    }

    public SpeculativeBetaConfig(boolean listenersRequired, boolean commuteRequired, int minimalLength) {
        if (minimalLength < 0) {
            throw new IllegalArgumentException();
        }
        this.listenersRequired = listenersRequired;
        this.minimalLength = minimalLength;
        this.commuteRequired = commuteRequired;
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

        return new SpeculativeBetaConfig(listenersRequired, commuteRequired, newMinimalLength);
    }

    public SpeculativeBetaConfig createWithListenersRequired() {
        if (listenersRequired) {
            return this;
        }

        return new SpeculativeBetaConfig(true, commuteRequired, minimalLength);
    }

    public SpeculativeBetaConfig createWithCommuteRequired() {
        if (commuteRequired) {
            return this;
        }

        return new SpeculativeBetaConfig(listenersRequired, true, minimalLength);
    }

    @Override
    public String toString() {
        return "SpeculativeBetaConfig{" +
                "listenersRequired=" + listenersRequired +
                ", commuteRequired=" + commuteRequired +
                ", minimalLength=" + minimalLength +
                '}';
    }
}
