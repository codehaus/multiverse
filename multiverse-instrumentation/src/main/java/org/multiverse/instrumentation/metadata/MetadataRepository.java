package org.multiverse.instrumentation.metadata;

import org.multiverse.instrumentation.asm.AsmClassMetadataExtractor;
import org.objectweb.asm.Type;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A repository for storing metadata about classes (and interfaces) and their internal structure.
 * <p/>
 * Once metadata is extracted, it is stored so it can be reused.
 *
 * @author Peter Veentjer.
 */
public final class MetadataRepository {

    private final ConcurrentMap<Multikey, ClassMetadata> map = new ConcurrentHashMap<Multikey, ClassMetadata>();

    private final ClassMetadataExtractor extractor;

    public MetadataRepository() {
        this(new AsmClassMetadataExtractor());
    }

    /**
     * Creates a MetadataRepository with the given ClassMetadataExtractor .
     *
     * @param extractor the ClassMetadataExtractor used to extra metadata for classes that have not
     *                  been inspected.
     * @throws NullPointerException if extractor is null.
     */
    public MetadataRepository(ClassMetadataExtractor extractor) {
        if (extractor == null) {
            throw new NullPointerException();
        }

        this.extractor = extractor;
        this.extractor.init(this);
    }

    /**
     * Loads the ClassMetadata for the given Clazz.
     *
     * @param clazz the Clazz to get the ClassMetadata for.
     * @return return the loaded ClassMetadata.
     */
    public ClassMetadata loadClassMetadata(Class clazz) {
        if (clazz == null) {
            throw new NullPointerException();
        }

        String name = Type.getInternalName(clazz);
        return loadClassMetadata(clazz.getClassLoader(), name);
    }

    /**
     * Retrieves the ClassMetadata for a class.
     *
     * @param classLoader
     * @param className   the className of the class to look for.
     * @return the ClassMetadata (will never be null).
     */
    public ClassMetadata loadClassMetadata(ClassLoader classLoader, String className) {
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
