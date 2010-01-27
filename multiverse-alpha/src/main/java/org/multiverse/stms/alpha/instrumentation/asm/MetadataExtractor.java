package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.annotations.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;

/**
 * An Extractor responsible for collecting information about a ClassNode and storeAndReleaseLock it in the {@link
 * MetadataRepository}. This is one of the first things that should be run, so that the other transformers/factories
 * have their information in place.
 * <p/>
 * An instance should not be reused.
 *
 * @author Peter Veentjer
 */
public final class MetadataExtractor implements Opcodes {

    private final ClassNode classNode;
    private final MetadataRepository metadataRepository;

    private boolean isRealTransactionalObject = false;
    private boolean isTransactionalObject = false;
    private boolean hasTransactionalMethods = false;

    public MetadataExtractor(ClassNode classNode) {
        this.classNode = classNode;
        this.metadataRepository = MetadataRepository.INSTANCE;
    }

    public void extract() {
        metadataRepository.markAsLoaded(classNode);

        if (isTransactionalObject()) {
            isTransactionalObject = true;

            if (metadataRepository.isRealTransactionalObject(classNode.superName)) {
                isRealTransactionalObject = true;
            }
        }

        extractFieldMetadata();
        extractMethodMetadata();

        metadataRepository.setIsTransactionalObject(classNode, isTransactionalObject);
        metadataRepository.setIsRealTransactionalObject(classNode, isRealTransactionalObject);
        metadataRepository.setHasTransactionalMethods(classNode, hasTransactionalMethods);

        if (isRealTransactionalObject) {
            metadataRepository.setTranlocalName(classNode, classNode.name + "__Tranlocal");
            metadataRepository.setTranlocalSnapshotName(classNode, classNode.name + "__TranlocalSnapshot");
        }
    }

    private boolean isTransactionalObject() {
        if (hasTransactionalObjectAnnotation(classNode)) {
            return true;
        }

        if (metadataRepository.isTransactionalObject(classNode.superName)) {
            return true;
        }

        for (String interfaceName : (List<String>) classNode.interfaces) {
            if (metadataRepository.isTransactionalObject(interfaceName)) {
                return true;
            }
        }


        return false;
    }

    private void extractFieldMetadata() {
        for (FieldNode field : (List<FieldNode>) classNode.fields) {
            extractFieldMetadata(field);
        }
    }

    private void extractFieldMetadata(FieldNode field) {
        boolean isManagedField = false;
        boolean isManagedFieldWithFieldGranularity = false;

        if (isManagedField(field)) {
            isRealTransactionalObject = true;
            isManagedField = true;
        } else if (isManagedFieldWithFieldGranularity(field)) {
            isManagedFieldWithFieldGranularity = true;
        }

        metadataRepository.setIsManagedInstanceField(classNode, field, isManagedField);
        metadataRepository.setIsManagedInstanceFieldWithFieldGranularity(classNode, field, isManagedFieldWithFieldGranularity);
    }

    private boolean isManagedFieldWithFieldGranularity(FieldNode field) {
        if (!isTransactionalObject) {
            return false;
        }

        if (!hasFieldGranularity(field)) {
            return false;
        }

        if (isInvisibleField(field)) {
            return false;
        }

        return true;
    }


    /**
     * Checks if the field is a managed field of a transactional object.
     */
    private boolean isManagedField(FieldNode field) {
        if (!isTransactionalObject) {
            return false;
        }

        if (hasFieldGranularity(field)) {
            return false;
        }

        if (isInvisibleField(field)) {
            return false;
        }

        return true;
    }

    private boolean isInvisibleField(FieldNode fieldNode) {
        if (isFinal(fieldNode)) {
            return true;
        }

        if (isExcluded(fieldNode)) {
            return true;
        }

        if (isStatic(fieldNode)) {
            return true;
        }

        if (isSynthetic(fieldNode.access)) {
            return true;
        }

        if (isVolatile(fieldNode)) {
            return true;
        }

        return false;
    }


    private void extractMethodMetadata() {
        for (MethodNode method : (List<MethodNode>) classNode.methods) {
            extractMethodMetadata(method);
        }
    }

    private boolean isInvisibleMethod(MethodNode methodNode) {
        if (isExcluded(methodNode)) {
            return true;
        }

        if (isSynthetic(methodNode.access)) {
            return true;
        }

        return false;
    }

    private void extractMethodMetadata(MethodNode method) {
        boolean isTransactionalMethod = false;
        TransactionalMethodParams params = null;

        if (!isInvisibleMethod(method)) {
            if (isTransactionalObject) {
                if (hasTransactionalMethodAnnotation(method) || hasTransactionalConstructorAnnotation(method)) {
                    isTransactionalMethod = true;
                    params = createTransactionalMethodParams(method);
                } else if (!isStatic(method)) {
                    isTransactionalMethod = true;
                    params = createDefaultTransactionalMethodParams(method);
                }
            } else if (hasTransactionalMethodAnnotation(method)) {
                isTransactionalMethod = true;
                params = createTransactionalMethodParams(method);
            } else if (hasTransactionalConstructorAnnotation(method)) {
                isTransactionalMethod = true;
                params = createTransactionalMethodParams(method);
            }
        }

        if (isTransactionalMethod) {
            hasTransactionalMethods = true;
        }

        metadataRepository.setIsTransactionalMethod(classNode, method, isTransactionalMethod);
        if (isTransactionalMethod) {
            metadataRepository.setTransactionalMethodParams(classNode, method, params);
        }
    }

    private void ensureNoTxMethodAccessModifierViolation(MethodNode method) {
        boolean hasTxMethodAnnotation = hasTransactionalMethodAnnotation(method);
        boolean hasInvalidAccessModifier = hasCorrectMethodAccessForTransactionalMethod(method.access);

        if (hasTxMethodAnnotation && !hasInvalidAccessModifier) {
            String msg = "Invalid access modifier for method:" + classNode.name + "." + method.name + method.desc +
                    " (native, abstract and synthetic not allowed) ";
            throw new RuntimeException(msg);
        }
    }

    private TransactionalMethodParams createDefaultTransactionalMethodParams(MethodNode method) {
        if (method.name.equals("<init>")) {
            TransactionalMethodParams params = new TransactionalMethodParams();
            params.readOnly = false;
            params.automaticReadTracking = true;
            params.preventWriteSkew = false;
            params.maxRetryCount = 0;
            params.familyName = createDefaultFamilyName(method);
            params.interruptible = false;
            params.smartTxLengthSelector = false;
            return params;
        } else {
            TransactionalMethodParams params = new TransactionalMethodParams();
            params.readOnly = false;
            params.automaticReadTracking = true;
            params.preventWriteSkew = true;
            params.maxRetryCount = 1000;
            params.familyName = createDefaultFamilyName(method);
            params.interruptible = false;
            params.smartTxLengthSelector = true;
            return params;
        }
    }

    private TransactionalMethodParams createTransactionalMethodParams(MethodNode method) {
        if (method.name.equals("<init>")) {
            AnnotationNode txMethodAnnotation = AsmUtils.getVisibleAnnotation(method, TransactionalConstructor.class);
            TransactionalMethodParams params = new TransactionalMethodParams();

            params.familyName = (String) getValue(txMethodAnnotation, "familyName", createDefaultFamilyName(method));
            params.automaticReadTracking = (Boolean) getValue(txMethodAnnotation, "automaticReadTracking", true);
            params.interruptible = (Boolean) getValue(txMethodAnnotation, "interruptible", false);
            params.preventWriteSkew = (Boolean) getValue(txMethodAnnotation, "preventWriteSkew", false);
            params.smartTxLengthSelector = false;
            params.readOnly = (Boolean) getValue(txMethodAnnotation, "readonly", false);
            return params;
        } else {
            AnnotationNode txMethodAnnotation = AsmUtils.getVisibleAnnotation(method, TransactionalMethod.class);
            TransactionalMethodParams params = new TransactionalMethodParams();
            params.readOnly = (Boolean) getValue(txMethodAnnotation, "readonly", false);
            params.familyName = (String) getValue(txMethodAnnotation, "familyName", createDefaultFamilyName(method));
            params.maxRetryCount = (Integer) getValue(txMethodAnnotation, "maxRetryCount", 1000);
            boolean trackReadsDefault = !params.readOnly;
            params.automaticReadTracking = (Boolean) getValue(txMethodAnnotation,
                    "automaticReadTracking",
                    trackReadsDefault);
            params.interruptible = (Boolean) getValue(txMethodAnnotation, "interruptible", false);
            boolean preventWriteSkewDefault = false;
            params.preventWriteSkew = (Boolean) getValue(txMethodAnnotation,
                    "preventWriteSkew",
                    preventWriteSkewDefault);
            params.smartTxLengthSelector = true;
            return params;
        }
    }

    private String createDefaultFamilyName(MethodNode method) {
        StringBuffer sb = new StringBuffer();
        sb.append(classNode.name.replace("/", "."));
        sb.append('.');
        sb.append(method.name);
        sb.append('(');
        Type[] argTypes = Type.getArgumentTypes(method.desc);
        for (int k = 0; k < argTypes.length; k++) {
            sb.append(argTypes[k].getClassName());
            if (k < argTypes.length - 1) {
                sb.append(',');
            }
        }
        sb.append(')');

        return sb.toString();
    }


    private static Object getValue(AnnotationNode node, String name, Object defaultValue) {
        if (node.values == null) {
            return defaultValue;
        }

        for (int k = 0; k < node.values.size(); k += 2) {
            String found = (String) node.values.get(k);
            if (name.equals(found)) {
                return node.values.get(k + 1);
            }
        }

        return defaultValue;
    }

    private static boolean hasCorrectMethodAccessForTransactionalMethod(int access) {
        return !(AsmUtils.isSynthetic(access) || isNative(access));
    }

    public static boolean isExcluded(FieldNode field) {
        return hasVisibleAnnotation(field, Exclude.class);
    }

    public static boolean isExcluded(MethodNode methodNode) {
        return hasVisibleAnnotation(methodNode, Exclude.class);
    }

    public static boolean hasFieldGranularity(FieldNode field) {
        return hasVisibleAnnotation(field, FieldGranularity.class);
    }

    public static boolean hasTransactionalMethodAnnotation(MethodNode methodNode) {
        return hasVisibleAnnotation(methodNode, TransactionalMethod.class);
    }

    public static boolean hasTransactionalConstructorAnnotation(MethodNode methodNode) {
        return hasVisibleAnnotation(methodNode, TransactionalConstructor.class);
    }

    public static boolean hasTransactionalObjectAnnotation(ClassNode classNode) {
        return hasVisibleAnnotation(classNode, TransactionalObject.class);
    }

    public static boolean isSynthetic(int access) {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public static boolean isVolatile(FieldNode fieldNode) {
        return (fieldNode.access & Opcodes.ACC_VOLATILE) != 0;
    }
}
