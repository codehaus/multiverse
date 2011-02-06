package org.multiverse.stms.gamma.transactions;

/**
 * The GammaStm uses a speculative mechanism (if enabled) to learn from executing transactions. Transactions start
 * cheap and with a lot of features disabled, but once the speculation failed, the SpeculativeGammaConfguration
 * is 'updated'.
 * <p/>
 * This class is immutable.
 *
 * @author Peter Veentjer.
 */
@SuppressWarnings({"ClassWithTooManyFields"})
public final class SpeculativeGammaConfiguration {

    public final boolean listenersDetected;
    public final boolean commuteDetected;
    public final boolean orelseDetected;
    public final boolean nonRefTypeDetected;
    public final boolean fat;
    public final boolean locksDetected;
    public final boolean constructedObjectsDetected;
    public final boolean isRichMansConflictScanRequired;
    public final boolean abortOnlyDetected;
    public final boolean ensureDetected;
    public final int minimalLength;

    /**
     * Creates a full speculative SpeculativeGammaConfiguration.
     */
    public SpeculativeGammaConfiguration() {
        this(false, false, false, false, false, false, false, false, false, false, 1);
    }

    public SpeculativeGammaConfiguration(
            final boolean isFat,
            final boolean listenersDetected,
            final boolean isCommuteDetected,
            final boolean isNonRefTypeDetected,
            final boolean isOrelseDetected,
            final boolean locksDetected,
            final boolean constructedObjectsDetected,
            final boolean isRichMansConflictScanRequired,
            final boolean isAbortOnlyDetected,
            final boolean ensureDetected,
            final int minimalLength) {

        if (minimalLength < 0) {
            throw new IllegalArgumentException();
        }

        this.fat = isFat;
        this.constructedObjectsDetected = constructedObjectsDetected;
        this.listenersDetected = listenersDetected;
        this.locksDetected = locksDetected;
        this.commuteDetected = isCommuteDetected;
        this.isRichMansConflictScanRequired = isRichMansConflictScanRequired;
        this.nonRefTypeDetected = isNonRefTypeDetected;
        this.orelseDetected = isOrelseDetected;
        this.minimalLength = minimalLength;
        this.abortOnlyDetected = isAbortOnlyDetected;
        this.ensureDetected = ensureDetected;
    }

    public SpeculativeGammaConfiguration newWithMinimalLength(int newMinimalLength) {
        if (newMinimalLength < 0) {
            throw new IllegalArgumentException();
        }

        if (minimalLength >= newMinimalLength) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                fat, listenersDetected, commuteDetected, nonRefTypeDetected, orelseDetected,
                locksDetected, constructedObjectsDetected, isRichMansConflictScanRequired,
                abortOnlyDetected, ensureDetected, newMinimalLength);
    }

    public SpeculativeGammaConfiguration newWithLocks() {
        if (locksDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, listenersDetected, commuteDetected, nonRefTypeDetected, orelseDetected, true,
                constructedObjectsDetected, isRichMansConflictScanRequired, abortOnlyDetected, ensureDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithAbortOnly() {
        if (abortOnlyDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, listenersDetected, commuteDetected, nonRefTypeDetected, orelseDetected, locksDetected,
                constructedObjectsDetected, isRichMansConflictScanRequired, true, ensureDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithConstructedObjects() {
        if (constructedObjectsDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, listenersDetected, commuteDetected, nonRefTypeDetected, orelseDetected, locksDetected,
                true, isRichMansConflictScanRequired, abortOnlyDetected, ensureDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithListeners() {
        if (listenersDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, true, commuteDetected, nonRefTypeDetected, orelseDetected, locksDetected,
                constructedObjectsDetected, isRichMansConflictScanRequired, abortOnlyDetected, ensureDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithOrElse() {
        if (orelseDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, listenersDetected, commuteDetected, nonRefTypeDetected, true, locksDetected,
                constructedObjectsDetected, isRichMansConflictScanRequired, abortOnlyDetected, ensureDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithNonRefType() {
        if (nonRefTypeDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, listenersDetected, commuteDetected, true, orelseDetected, locksDetected,
                constructedObjectsDetected, isRichMansConflictScanRequired, abortOnlyDetected, ensureDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithCommute() {
        if (commuteDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, listenersDetected, true, nonRefTypeDetected, orelseDetected, locksDetected,
                constructedObjectsDetected, isRichMansConflictScanRequired, abortOnlyDetected, ensureDetected, minimalLength);
    }

    public SpeculativeGammaConfiguration newWithRichMansConflictScan() {
        if (isRichMansConflictScanRequired) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, listenersDetected, commuteDetected, nonRefTypeDetected, orelseDetected, locksDetected,
                constructedObjectsDetected, true, abortOnlyDetected, ensureDetected, minimalLength);
    }


    public SpeculativeGammaConfiguration newWithEnsure() {
        if (ensureDetected) {
            return this;
        }

        return new SpeculativeGammaConfiguration(
                true, listenersDetected, commuteDetected, nonRefTypeDetected, orelseDetected, locksDetected,
                constructedObjectsDetected, true, abortOnlyDetected, true, minimalLength);
    }

    @Override
    public String toString() {
        return "SpeculativeGammaConfiguration{" +
                "areListenersRequired=" + listenersDetected +
                ", isCommuteDetected=" + commuteDetected +
                ", isNonRefTypeDetected=" + nonRefTypeDetected +
                ", isOrelseDetected=" + orelseDetected +
                ", isOrelseDetected=" + orelseDetected +
                ", minimalLength=" + minimalLength +
                ", isRichMansConflictScanDetected=" + isRichMansConflictScanRequired +
                ", areConstructedObjectsDetected=" + constructedObjectsDetected +
                ", abortOnlyDetected=" + abortOnlyDetected +
                ", ensureDetected=" + ensureDetected +
                ", isFat=" + fat +
                '}';
    }

}
