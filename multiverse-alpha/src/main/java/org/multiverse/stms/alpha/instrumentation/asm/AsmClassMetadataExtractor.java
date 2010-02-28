package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.annotations.*;
import org.multiverse.stms.alpha.instrumentation.metadata.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.util.List;

import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;

public class AsmClassMetadataExtractor implements ClassMetadataExtractor {

    private MetadataRepository metadataRepository;

    @Override
    public void init(MetadataRepository metadataRepository) {
        if (metadataRepository == null) {
            throw new NullPointerException();
        }
        this.metadataRepository = metadataRepository;
    }

    @Override
    public ClassMetadata extract(String className, ClassLoader classLoader) {
        ClassMetadata classMetadata = new ClassMetadata(className);
        if (classLoader == null) {
            classMetadata.setIgnoredClass(true);
            return classMetadata;
        }

        String fileName = className + ".class";
        InputStream is = classLoader.getResourceAsStream(fileName);
        if (is == null) {
            classMetadata.setIgnoredClass(true);
            return classMetadata;
        }

        ClassNode classNode = AsmUtils.loadAsClassNode(classLoader, className);
        classMetadata.setIsInterface(AsmUtils.isInterface(classNode));

        if (classNode.superName != null) {
            ClassMetadata superClassMetadata = metadataRepository.getClassMetadata(classNode.superName);
            classMetadata.setSuperClassMetadata(superClassMetadata);
        }

        if (isTransactionalObject(classNode)) {
            classMetadata.setIsTransactionalObject(true);
        }

        for (String interfaceName : (List<String>) classNode.interfaces) {
            ClassMetadata interfaceMetadata = metadataRepository.getClassMetadata(interfaceName);
            classMetadata.getInterfaces().add(interfaceMetadata);
        }

        for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
            extractFieldMetadata(classMetadata, fieldNode);
        }

        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            extractMethodMetadata(classMetadata, methodNode);
        }

        return classMetadata;
    }

    private void extractMethodMetadata(ClassMetadata classMetadata, MethodNode methodNode) {
        MethodMetadata methodMetadata = classMetadata.createMethodMetadata(methodNode.name, methodNode.desc);
        methodMetadata.setAccess(methodNode.access);

        TransactionMetadata transactionMetadata = null;

        if (!isInvisibleMethod(methodNode)) {
            if (classMetadata.isTransactionalObject()) {
                if (hasTransactionalMethodAnnotation(methodNode) || hasTransactionalConstructorAnnotation(methodNode)) {
                    transactionMetadata = createTransactionMetadata(classMetadata, methodNode);
                } else if (!isStatic(methodNode)) {
                    transactionMetadata = createDefaultTransactionMetadata(classMetadata, methodNode);
                }
            } else if (hasTransactionalMethodAnnotation(methodNode)) {
                transactionMetadata = createTransactionMetadata(classMetadata, methodNode);
            } else if (hasTransactionalConstructorAnnotation(methodNode)) {
                transactionMetadata = createTransactionMetadata(classMetadata, methodNode);
            }
        }

        methodMetadata.setTransactionalMetadata(transactionMetadata);
        methodMetadata.setAccess(methodNode.access);
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

    private FieldMetadata extractFieldMetadata(ClassMetadata classMetadata, FieldNode field) {
        FieldMetadata fieldMetadata = classMetadata.createFieldMetadata(field.name);
        fieldMetadata.setDesc(field.desc);

        if (isManagedField(classMetadata, field)) {
            fieldMetadata.setIsManagedField(true);
        } else if (isManagedFieldWithFieldGranularity(classMetadata, field)) {
            fieldMetadata.setHasFieldGranularity(true);
        }

        return fieldMetadata;
    }

    private boolean isTransactionalObject(ClassNode classNode) {
        String objectName = Type.getInternalName(Object.class);
        if (classNode.name.equals(objectName)) {
            return false;
        }

        if (hasTransactionalObjectAnnotation(classNode)) {
            return true;
        }

        ClassMetadata superClassMetadata = metadataRepository.getClassMetadata(classNode.superName);
        if (superClassMetadata.isTransactionalObject()) {
            return true;
        }

        for (String interfaceName : (List<String>) classNode.interfaces) {
            ClassMetadata interfaceMetadata = metadataRepository.getClassMetadata(interfaceName);
            if (interfaceMetadata.isTransactionalObject()) {
                return true;
            }
        }

        return false;
    }

    private boolean isManagedFieldWithFieldGranularity(ClassMetadata classMetadata, FieldNode field) {
        if (!classMetadata.isTransactionalObject()) {
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
    private boolean isManagedField(ClassMetadata classMetadata, FieldNode field) {
        if (!classMetadata.isTransactionalObject()) {
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

    private TransactionMetadata createDefaultTransactionMetadata(ClassMetadata classMetadata, MethodNode methodNode) {
        TransactionMetadata params = new TransactionMetadata();
        params.readOnly = false;
        params.automaticReadTracking = true;
        params.allowWriteSkewProblem = true;
        params.interruptible = false;
        params.familyName = createFamilyName(classMetadata.getName(), methodNode.name, methodNode.desc);

        if (methodNode.name.equals("<init>")) {
            params.maxRetryCount = 0;
            params.smartTxLengthSelector = false;
        } else {
            params.maxRetryCount = 1000;
            params.smartTxLengthSelector = true;
        }

        return params;
    }

    private TransactionMetadata createTransactionMetadata(ClassMetadata classMetadata, MethodNode methodNode) {
        TransactionMetadata params = new TransactionMetadata();

        AnnotationNode annotationNode;
        if (methodNode.name.equals("<init>")) {
            annotationNode = AsmUtils.getVisibleAnnotation(methodNode, TransactionalConstructor.class);
        } else {
            annotationNode = AsmUtils.getVisibleAnnotation(methodNode, TransactionalMethod.class);
        }

        params.readOnly = (Boolean) getValue(annotationNode, "readonly", false);
        params.familyName = createFamilyName(classMetadata.getName(), methodNode.name, methodNode.desc);
        params.interruptible = (Boolean) getValue(annotationNode, "interruptible", false);
        params.allowWriteSkewProblem = (Boolean) getValue(annotationNode, "allowWriteSkewProblem", true);
        boolean trackReadsDefault = !params.readOnly;
        params.automaticReadTracking = (Boolean) getValue(annotationNode, "automaticReadTracking", trackReadsDefault);

        if (methodNode.name.equals("<init>")) {
            params.maxRetryCount = (Integer) getValue(annotationNode, "maxRetryCount", 0);
            params.smartTxLengthSelector = false;
        } else {
            params.maxRetryCount = (Integer) getValue(annotationNode, "maxRetryCount", 1000);
            params.smartTxLengthSelector = true;
        }

        return params;
    }

    private String createFamilyName(String className, String methodName, String desc) {
        StringBuffer sb = new StringBuffer();
        sb.append(className.replace("/", "."));
        sb.append('.');
        sb.append(methodName);
        sb.append('(');
        Type[] argTypes = Type.getArgumentTypes(desc);
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
        if (node == null || node.values == null) {
            return defaultValue;
        }

        for (int k = 0; k < node.values.size(); k += 2) {
            String paramName = (String) node.values.get(k);
            if (name.equals(paramName)) {
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
