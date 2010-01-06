package org.multiverse.stms.alpha.instrumentation.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import static java.lang.String.format;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A repository (singleton) that stores alle metadata needed for the instrumentation process.
 *
 * @author Peter Veentjer
 */
public final class MetadataRepository {

    private final static Logger logger = Logger.getLogger(MetadataRepository.class.getName());

    public final static MetadataRepository INSTANCE = new MetadataRepository();

    private final Map<String, Object> infoMap = new HashMap<String, Object>();

    public volatile static ClassLoader classLoader;

    /**
     * todo: code needs to be cleaned up
     *
     * @param className
     */
    public void ensureMetadataExtracted(String className) {
        if (isLoaded(className)) {
            return;
        }

        if (classLoader == null) {
            return;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(format("Extracing metadata from class: %s", className));
        }

        String fileName = Type.getObjectType(className).getInternalName() + ".class";
        InputStream is = classLoader.getResourceAsStream(fileName);
        if (is == null) {
            return;
        }

        ClassNode node = AsmUtils.loadAsClassNode(classLoader, Type.getObjectType(className).getInternalName());
        MetadataExtractor extractor = new MetadataExtractor(node);
        extractor.extract();
    }

    public boolean isAtomicMethod(ClassNode owner, MethodNode method) {
        return isAtomicMethod(owner.name, method.name, method.desc);
    }

    public boolean isAtomicMethod(String atomicClass, String name, String desc) {
        ensureMetadataExtracted(atomicClass);
        String key = "IsAtomicMethod#" + atomicClass + "#" + name + "#" + desc;
        return getPrepareInfoAsBoolean(key);
    }

    public AtomicMethodParams getAtomicMethodParams(ClassNode atomicClass, MethodNode method) {
        String key = "AtomicMethodParams#" + atomicClass.name + "#" + method.name + "#" + method.desc;
        return (AtomicMethodParams) infoMap.get(key);
    }

    public void setAtomicMethodParams(ClassNode atomicClass, MethodNode method, AtomicMethodParams params) {
        String key = "AtomicMethodParams#" + atomicClass.name + "#" + method.name + "#" + method.desc;
        infoMap.put(key, params);
    }

    public void setIsAtomicMethod(ClassNode atomicClass, MethodNode method, boolean isAtomicMethod) {
        String key = "IsAtomicMethod#" + atomicClass.name + "#" + method.name + "#" + method.desc;
        putBoolean(isAtomicMethod, key);
    }

    private void putBoolean(boolean value, String key) {
        if (value) {
            infoMap.put(key, value);
        }
    }

    public void setTranlocalName(ClassNode atomicObject, String tranlocalName) {
        setTranlocalName(atomicObject.name, tranlocalName);
    }

    public void setTranlocalName(String atomicObjectName, String tranlocalName) {
        infoMap.put("TranlocalName#" + atomicObjectName, tranlocalName);
    }

    public String getTranlocalName(ClassNode atomicObject) {
        return getTranlocalName(atomicObject.name);
    }

    public String getTranlocalName(String atomicObjectName) {
        ensureMetadataExtracted(atomicObjectName);
        return (String) infoMap.get("TranlocalName#" + atomicObjectName);
    }

    public String getTranlocalSnapshotName(ClassNode atomicObject) {
        return getTranlocalSnapshotName(atomicObject.name);
    }

    public void setTranlocalSnapshotName(ClassNode atomicObject, String tranlocalSnapshotName) {
        setTranlocalSnapshotName(atomicObject.name, tranlocalSnapshotName);
    }

    public void setTranlocalSnapshotName(String atomicObject, String tranlocalSnapshotName) {
        ensureMetadataExtracted(atomicObject);
        infoMap.put("TranlocalSnapshotName#" + atomicObject, tranlocalSnapshotName);
    }

    public String getTranlocalSnapshotName(String atomicObjectName) {
        ensureMetadataExtracted(atomicObjectName);
        String key = "TranlocalSnapshotName#" + atomicObjectName;
        return (String) infoMap.get(key);
    }

    public boolean isManagedInstanceField(String atomicObjectName, String fieldName) {
        ensureMetadataExtracted(atomicObjectName);
        String key = "IsManagedInstanceField#" + atomicObjectName + "." + fieldName;
        return getPrepareInfoAsBoolean(key);
    }

    public void setIsManagedInstanceField(ClassNode atomicObject, FieldNode field, boolean managedField) {
        String key = "IsManagedInstanceField#" + atomicObject.name + "." + field.name;
        putBoolean(managedField, key);
    }

    public boolean hasManagedInstanceFields(ClassNode atomicObject) {
        return isRealAtomicObject(atomicObject.name);
    }

    public boolean isAtomicObject(String className) {
        ensureMetadataExtracted(className);
        String key = "IsAtomicObject#" + className;
        return getPrepareInfoAsBoolean(key);
    }

    public void setIsAtomicObject(ClassNode classNode, boolean atomicObject) {
        String key = "IsAtomicObject#" + classNode.name;
        putBoolean(atomicObject, key);
    }

    public boolean isRealAtomicObject(String className) {
        ensureMetadataExtracted(className);
        String key = "IsRealAtomicObject#" + className;
        return getPrepareInfoAsBoolean(key);
    }

    public void setIsRealAtomicObject(ClassNode classNode, boolean hasManagedFields) {
        String key = "IsRealAtomicObject#" + classNode.name;
        putBoolean(hasManagedFields, key);
    }

    private boolean getPrepareInfoAsBoolean(String key) {
        Object result = infoMap.get(key);
        return result == null ? false : (Boolean) result;
    }

    public List<FieldNode> getManagedInstanceFields(ClassNode classNode) {
        if (!isRealAtomicObject(classNode.name)) {
            return new LinkedList<FieldNode>();
        }

        List<FieldNode> fields = new LinkedList<FieldNode>();
        for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
            if (isManagedInstanceField(classNode.name, fieldNode.name)) {
                fields.add(fieldNode);
            }
        }
        return fields;
    }

    public List<MethodNode> getAtomicMethods(ClassNode classNode) {
        List<MethodNode> result = new LinkedList<MethodNode>();
        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            if (isAtomicMethod(classNode.name, methodNode.name, methodNode.desc)) {
                result.add(methodNode);
            }
        }
        return result;
    }

    public void setHasAtomicMethods(ClassNode classNode, boolean hasAtomicMethods) {
        setHasAtomicMethods(classNode.name, hasAtomicMethods);
    }

    public void setHasAtomicMethods(String className, boolean hasAtomicMethods) {
        ensureMetadataExtracted(className);
        String key = "HasAtomicMethods#" + className;
        putBoolean(hasAtomicMethods, key);
    }

    public boolean hasAtomicMethods(ClassNode classNode) {
        return hasAtomicMethods(classNode.name);
    }

    public boolean hasAtomicMethods(String className) {
        ensureMetadataExtracted(className);
        String key = "HasAtomicMethods#" + className;
        return getPrepareInfoAsBoolean(key);
    }

    public void signalLoaded(ClassNode classNode) {
        String key = "Prepared#" + classNode.name;
        putBoolean(true, key);
    }

    public boolean isLoaded(ClassNode classNode) {
        return isLoaded(classNode.name);
    }

    public boolean isLoaded(String className) {
        String key = "Prepared#" + className;
        return getPrepareInfoAsBoolean(key);
    }
}
