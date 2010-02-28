package org.multiverse.stms.alpha.instrumentation.metadata;

public interface ClassMetadataExtractor {

    void init(MetadataRepository metadataRepository);

    ClassMetadata extract(String className, ClassLoader classLoader);
}
