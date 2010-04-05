package org.multiverse.api.programmatic;

/**
 * With the ProgrammaticReferenceFactoryBuilder you can createReference a ProgrammaticReference. This ProgrammaticFactoryBuilder
 * gives the ability to use different configured ProgrammaticReferencesFactories for the same
 * STM. At the moment there is nothing to configure on this ProgrammaticReferenceFactoryBuilder,
 * but in the future there will be additions like history or durability for example.
 * <p/>
 * It uses the same design as the {@link org.multiverse.api.TransactionFactoryBuilder}.
 * <p/>
 * Methods are threadsafe to call.
 *
 * @author Peter Veentjer
 */
public interface ProgrammaticReferenceFactoryBuilder {

    /**
     * Builds a new ProgrammaticReferenceFactory.
     *
     * @return the created ProgrammaticReferenceFactory.
     */
    ProgrammaticReferenceFactory build();
}
