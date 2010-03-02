package org.multiverse.stms.alpha.instrumentation.metadata;

/**
 * Responsible for extracting {@link ClassMetadata} from a class.
 * <p/>
 * A reference to the MetadataRepository is needed in this ClassMetadataExtractor so that it can do requests
 * for metadata itself (for example for loading super class metadata for a class its super).
 *
 * @author Peter Veentjer.
 */
public interface ClassMetadataExtractor {

    /**
     * Initializes this ClassMetadataExtractor with the provided MetadataRepository so it can do requests for
     * metadata itself.
     *
     * @param metadataRepository the repository for retrieving metadata.
     */
    void init(MetadataRepository metadataRepository);

    /**
     * Extracts the {@link ClassMetadata} for a class.
     *
     * @param className   the internal name of the class.
     * @param classLoader the ClassLoader to use for loading the class definition.
     * @return the created ClassMetadata.
     */
    ClassMetadata extract(String className, ClassLoader classLoader);
}
