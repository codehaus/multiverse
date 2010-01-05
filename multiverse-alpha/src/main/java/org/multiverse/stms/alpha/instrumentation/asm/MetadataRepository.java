package org.multiverse.stms.alpha.instrumentation.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

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
        if (className == null) {
            return;
        }

        if (isLoaded(className)) {
            return;
        }

        if (classLoader == null) {
            return;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(format("Extracing metadata from class: %s", className));
        }


        String internalName = Type.getObjectType(className).getInternalName();
        String fileName = internalName + ".class";
        InputStream is = classLoader.getResourceAsStream(fileName);
        if (is == null) {
            return;
        }

        ClassNode node = AsmUtils.loadAsClassNode(classLoader, Type.getObjectType(className).getInternalName());
        MetadataExtractor extractor = new MetadataExtractor(node);
        extractor.extract();
    }

    public boolean isTransactionalMethod(ClassNode owner, MethodNode method) {
        return isTransactionalMethod(owner.name, method.name, method.desc);
    }

    public boolean isTransactionalMethod(String transactionalClass, String name, String desc) {
        ensureMetadataExtracted(transactionalClass);
        String key = "isTxMethod#" + transactionalClass + '#' + name + '#' + desc;
        return getPrepareInfoAsBoolean(key);
    }

    public TransactionalMethodParams getTransactionalMethodParams(ClassNode transactionalClass, MethodNode method) {
        String key = "TransactionalMethodParams#" + transactionalClass.name + '#' + method.name + '#' + method.desc;
        return (TransactionalMethodParams) infoMap.get(key);
    }

    public void setTransactionalMethodParams(ClassNode transactionalClass, MethodNode method, TransactionalMethodParams params) {
        String key = "TransactionalMethodParams#" + transactionalClass.name + '#' + method.name + '#' + method.desc;
        infoMap.put(key, params);
    }

    public void setIsTransactionalMethod(ClassNode transactionalClass, MethodNode method, boolean isTxMethod) {
        String key = "isTxMethod#" + transactionalClass.name + '#' + method.name + '#' + method.desc;
        putBoolean(isTxMethod, key);
    }

    private void putBoolean(boolean value, String key) {
        if (value) {
            infoMap.put(key, value);
        }
    }

    public void setTranlocalName(ClassNode txObject, String tranlocalName) {
        setTranlocalName(txObject.name, tranlocalName);
    }

    public void setTranlocalName(String txObjectName, String tranlocalName) {
        infoMap.put("TranlocalName#" + txObjectName, tranlocalName);
    }

    public String getTranlocalName(ClassNode txObject) {
        return getTranlocalName(txObject.name);
    }

    public String getTranlocalName(String txObjectName) {
        ensureMetadataExtracted(txObjectName);
        return (String) infoMap.get("TranlocalName#" + txObjectName);
    }

    public String getTranlocalSnapshotName(ClassNode txObject) {
        return getTranlocalSnapshotName(txObject.name);
    }

    public void setTranlocalSnapshotName(ClassNode txObject, String tranlocalSnapshotName) {
        setTranlocalSnapshotName(txObject.name, tranlocalSnapshotName);
    }

    public void setTranlocalSnapshotName(String txObject, String tranlocalSnapshotName) {
        ensureMetadataExtracted(txObject);
        infoMap.put("TranlocalSnapshotName#" + txObject, tranlocalSnapshotName);
    }

    public String getTranlocalSnapshotName(String txObjectName) {
        ensureMetadataExtracted(txObjectName);
        String key = "TranlocalSnapshotName#" + txObjectName;
        return (String) infoMap.get(key);
    }

    public boolean isManagedInstanceField(String txObjectName, String fieldName) {
        ensureMetadataExtracted(txObjectName);
        String key = "IsManagedInstanceField#" + txObjectName + '.' + fieldName;
        return getPrepareInfoAsBoolean(key);
    }

    public void setIsManagedInstanceField(ClassNode txObject, FieldNode field, boolean managedField) {
        String key = "IsManagedInstanceField#" + txObject.name + '.' + field.name;
        putBoolean(managedField, key);
    }

    public boolean hasManagedInstanceFields(ClassNode txObject) {
        return isRealTransactionalObject(txObject.name);
    }

    public boolean isTransactionalObject(String className) {
        ensureMetadataExtracted(className);
        String key = "IsTxObject#" + className;
        return getPrepareInfoAsBoolean(key);
    }

    public void setIsTransactionalObject(ClassNode classNode, boolean txObject) {
        String key = "IsTxObject#" + classNode.name;
        putBoolean(txObject, key);
    }

    public boolean isRealTransactionalObject(String className) {
        ensureMetadataExtracted(className);
        String key = "IsRealTxObject#" + className;
        return getPrepareInfoAsBoolean(key);
    }

    public void setIsRealTransactionalObject(ClassNode classNode, boolean hasManagedFields) {
        String key = "IsRealTxObject#" + classNode.name;
        putBoolean(hasManagedFields, key);
    }

    private boolean getPrepareInfoAsBoolean(String key) {
        Object result = infoMap.get(key);
        return result == null ? false : (Boolean) result;
    }

    public List<FieldNode> getManagedInstanceFields(ClassNode classNode) {
        if (!isRealTransactionalObject(classNode.name)) {
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

    public List<MethodNode> getTransactionalMethods(ClassNode classNode) {
        List<MethodNode> result = new LinkedList<MethodNode>();
        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            if (isTransactionalMethod(classNode.name, methodNode.name, methodNode.desc)) {
                result.add(methodNode);
            }
        }
        return result;
    }

    public void setHasTransactionalMethods(ClassNode classNode, boolean hasTxMethods) {
        setHasTransactionalMethods(classNode.name, hasTxMethods);
    }

    public void setHasTransactionalMethods(String className, boolean hasTxMethods) {
        ensureMetadataExtracted(className);
        String key = "HasTxMethods#" + className;
        putBoolean(hasTxMethods, key);
    }

    public boolean hasTransactionalMethods(ClassNode classNode) {
        return hasTransactionalMethods(classNode.name);
    }

    public boolean hasTransactionalMethods(String className) {
        ensureMetadataExtracted(className);
        String key = "HasTxMethods#" + className;
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
