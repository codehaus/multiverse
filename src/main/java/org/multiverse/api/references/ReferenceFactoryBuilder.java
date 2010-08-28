package org.multiverse.api.references;

/**
 * A Builder for creating ReferenceFactory.
 *
 * A ReferenceFactoryBuilder is considered immutable.
 *
 * @author Peter Veentjer.
 */
public interface ReferenceFactoryBuilder {

    /**
     * Builds a ReferenceFactory.
     *
     * @return the build reference factory.
     */
    ReferenceFactory build();
}
