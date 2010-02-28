package org.multiverse.stms.alpha.instrumentation.metadata;

import org.multiverse.stms.alpha.instrumentation.asm.AsmClassMetadataExtractor;

import java.util.HashMap;

/**
 * todo:
 * always add the classloader and make sure that the information is stored including the classloader
 */
public final class MetadataRepository {

    public final static MetadataRepository INSTANCE = new MetadataRepository();

    public static ClassLoader classLoader;

    private final HashMap<String, ClassMetadata> map = new HashMap<String, ClassMetadata>();

    private final ClassMetadataExtractor classMetadataExtractor;

    private MetadataRepository() {
        classMetadataExtractor = new AsmClassMetadataExtractor();
        classMetadataExtractor.init(this);
    }

    public ClassMetadata getClassMetadata(String className) {
        if (className == null) {
            throw new NullPointerException();
        }

        ClassMetadata classMetadata = map.get(className);
        if (classMetadata == null) {
            classMetadata = classMetadataExtractor.extract(className, classLoader);
            map.put(className, classMetadata);
        }

        return classMetadata;
    }

    public MethodMetadata getMethodMetadata(String className, String methodName, String desc) {
        ClassMetadata classMetadata = getClassMetadata(className);
        return classMetadata.getMethodMetadata(methodName, desc);
    }
}
