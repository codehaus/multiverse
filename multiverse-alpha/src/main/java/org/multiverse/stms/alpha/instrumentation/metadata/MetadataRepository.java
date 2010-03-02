package org.multiverse.stms.alpha.instrumentation.metadata;

import org.multiverse.stms.alpha.instrumentation.asm.AsmClassMetadataExtractor;
import org.multiverse.utils.Multikey;
import org.objectweb.asm.Type;

import java.util.HashMap;

/**
 * todo:
 * always add the classloader and make sure that the information is stored including the classloader
 */
public final class MetadataRepository {

    public final static MetadataRepository INSTANCE = new MetadataRepository();

    private final HashMap<Multikey, ClassMetadata> map = new HashMap<Multikey, ClassMetadata>();

    private final ClassMetadataExtractor classMetadataExtractor;

    private MetadataRepository() {
        classMetadataExtractor = new AsmClassMetadataExtractor();
        classMetadataExtractor.init(this);
    }

    public ClassMetadata getClassMetadata(Class clazz) {
        if (clazz == null) {
            throw new NullPointerException();
        }

        String name = Type.getInternalName(clazz);
        return getClassMetadata(clazz.getClassLoader(), name);
    }

    public ClassMetadata getClassMetadata(ClassLoader classLoader, String className) {
        if (className == null) {
            throw new NullPointerException();
        }

        Multikey key = new Multikey(classLoader, className);

        ClassMetadata classMetadata = map.get(key);
        if (classMetadata == null) {
            classMetadata = classMetadataExtractor.extract(className, classLoader);
            map.put(key, classMetadata);
        }

        return classMetadata;
    }
}
