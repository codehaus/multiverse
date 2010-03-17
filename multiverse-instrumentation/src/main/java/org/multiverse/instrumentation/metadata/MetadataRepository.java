package org.multiverse.instrumentation.metadata;

import org.multiverse.instrumentation.asm.AsmClassMetadataExtractor;
import org.objectweb.asm.Type;

import java.util.HashMap;

/**
 * A repository for storing metadata about classes (and interfaces) and their internal structure.
 * <p/>
 * Once metadata is extracted, it is stored so it can be reused.
 *
 * @author Peter Veentjer.
 */
public final class MetadataRepository {

    private final HashMap<Multikey, ClassMetadata> map = new HashMap<Multikey, ClassMetadata>();

    private final ClassMetadataExtractor extractor;

    public MetadataRepository() {
        this(new AsmClassMetadataExtractor());
    }

    /**
     * Creates a MetadataRepository with the given ClassMetadataExtractor .
     *
     * @param extractor the ClassMetadataExtractor used to extra metadata for classes that have not
     *                  been inspected.
     */
    public MetadataRepository(ClassMetadataExtractor extractor) {
        if (extractor == null) {
            throw new NullPointerException();
        }

        this.extractor = extractor;
        this.extractor.init(this);
    }

    public ClassMetadata getClassMetadata(Class clazz) {
        if (clazz == null) {
            throw new NullPointerException();
        }

        String name = Type.getInternalName(clazz);
        return getClassMetadata(clazz.getClassLoader(), name);
    }

    /**
     * Retries the ClassMetadata for a class.
     *
     * @param classLoader
     * @param className   the className of the class to look for.
     * @return the ClassMetadata (will never be null).
     */
    public ClassMetadata getClassMetadata(ClassLoader classLoader, String className) {
        if (className == null) {
            throw new NullPointerException();
        }

        Multikey key = new Multikey(classLoader, className);

        ClassMetadata classMetadata = map.get(key);
        if (classMetadata == null) {
            classMetadata = extractor.extract(className, classLoader);
            map.put(key, classMetadata);
        }

        return classMetadata;
    }
}
