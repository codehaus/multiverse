package org.multiverse.api.programmatic;

/**
 * With the ProgrammaticRefFactoryBuilder you can createReference a ProgrammaticRef. This ProgrammaticFactoryBuilder
 * gives the ability to use different configured ProgrammaticReferencesFactories for the same
 * STM. At the moment there is nothing to configure on this ProgrammaticRefFactoryBuilder,
 * but in the future there will be additions like history or durability for example.
 * <p/>
 * It uses the same design as the {@link org.multiverse.api.TransactionFactoryBuilder}.
 * <p/>
 * Methods are threadsafe to call.
 *
 * @author Peter Veentjer
 */
public interface ProgrammaticRefFactoryBuilder {

    /**
     * Builds a new ProgrammaticRefFactory.
     *
     * @return the created ProgrammaticRefFactory.
     */
    ProgrammaticRefFactory build();
}
