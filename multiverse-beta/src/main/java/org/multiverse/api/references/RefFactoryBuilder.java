package org.multiverse.api.references;

/**
 * A Builder for creating RefFactory.
 * <p/>
 * A RefFactoryBuilder is considered immutable.
 *
 * @author Peter Veentjer.
 */
public interface RefFactoryBuilder {

    /**
     * Builds a RefFactory.
     *
     * @return the build reference factory.
     */
    RefFactory build();
}
