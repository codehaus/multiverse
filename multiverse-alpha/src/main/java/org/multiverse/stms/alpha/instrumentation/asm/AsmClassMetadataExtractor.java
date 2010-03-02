package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.annotations.*;
import org.multiverse.stms.alpha.instrumentation.metadata.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;

/**
 * An Asm based {@link ClassMetadataExtractor}.
 *
 * @author Peter Veentjer.
 */
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
        if (metadataRepository == null) {
            throw new IllegalStateException("metadataRepository is not initialized");
        }

        if (className == null) {
            throw new NullPointerException();
        }

        ClassMetadata classMetadata = new ClassMetadata(className);
        ClassNode classNode = loadClassNode(className, classLoader);

        if (classNode == null) {
            classMetadata.setIgnoredClass(true);
        } else {

            classMetadata.setAccess(classNode.access);

            if (isTransactional(classLoader, classNode)) {
                classMetadata.setIsTransactionalObject(true);
            }

            if (classNode.superName != null) {
                ClassMetadata superClassMetadata = metadataRepository.getClassMetadata(classLoader, classNode.superName);
                classMetadata.setSuperClassMetadata(superClassMetadata);
            }

            for (String interfaceName : (List<String>) classNode.interfaces) {
                ClassMetadata interfaceMetadata = metadataRepository.getClassMetadata(classLoader, interfaceName);
                classMetadata.getInterfaces().add(interfaceMetadata);
            }

            for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
                extractFieldMetadata(classMetadata, fieldNode);
            }

            for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
                extractMethodMetadata(classMetadata, methodNode);
            }
        }

        return classMetadata;
    }

    private ClassNode loadClassNode(String className, ClassLoader classLoader) {
        if (classLoader == null) {
            return null;
        }

        if (!existsClass(className, classLoader)) {
            return null;
        }

        return AsmUtils.loadAsClassNode(classLoader, className);
    }

    private boolean existsClass(String className, ClassLoader classLoader) {
        String fileName = className + ".class";
        InputStream is = classLoader.getResourceAsStream(fileName);
        if (is == null) {
            return false;
        } else {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close the inputstream", e);
            }
            return true;
        }
    }

    private void extractMethodMetadata(ClassMetadata classMetadata, MethodNode methodNode) {
        MethodMetadata methodMetadata = classMetadata.createMethodMetadata(methodNode.name, methodNode.desc);
        methodMetadata.setAccess(methodNode.access);

        if (methodNode.exceptions != null) {
            for (String exception : (List<String>) methodNode.exceptions) {
                methodMetadata.addException(exception);
            }
        }

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

    private FieldMetadata extractFieldMetadata(ClassMetadata classMetadata, FieldNode fieldNode) {
        FieldMetadata fieldMetadata = classMetadata.createFieldMetadata(fieldNode.name);
        fieldMetadata.setAccess(fieldNode.access);
        fieldMetadata.setDesc(fieldNode.desc);

        if (isManagedField(classMetadata, fieldNode)) {
            fieldMetadata.setIsManagedField(true);
        } else if (isManagedFieldWithFieldGranularity(classMetadata, fieldNode)) {
            fieldMetadata.setHasFieldGranularity(true);
        }

        return fieldMetadata;
    }

    /**
     * Checks if the class is transactional.
     * <p/>
     * A class is transactional if:
     * - one of the implementing interfaces is transactional (recursive)
     * - the parent is transactional (recursive)
     *
     * @param classLoader the ClassLoader that was used to load the ClassNode.
     * @param classNode   the ClassNode to check
     * @return true if it is transactional, false otherwise.
     */
    private boolean isTransactional(ClassLoader classLoader, ClassNode classNode) {
        String objectName = Type.getInternalName(Object.class);
        if (classNode.name.equals(objectName)) {
            return false;
        }

        if (hasTransactionalObjectAnnotation(classNode)) {
            return true;
        }

        ClassMetadata superClassMetadata = metadataRepository.getClassMetadata(classLoader, classNode.superName);
        if (superClassMetadata.isTransactionalObject()) {
            return true;
        }

        for (String interfaceName : (List<String>) classNode.interfaces) {
            ClassMetadata interfaceMetadata = metadataRepository.getClassMetadata(classLoader, interfaceName);
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
        if (isExcluded(fieldNode)) {
            return true;
        }

        if (isFinal(fieldNode)) {
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
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);
        boolean throwsInterruptedException = methodMetadata.checkIfSpecificTransactionIsThrown(InterruptedException.class);

        TransactionMetadata transactionMetadata = new TransactionMetadata();
        transactionMetadata.readOnly = false;
        transactionMetadata.automaticReadTracking = true;
        transactionMetadata.allowWriteSkewProblem = true;
        transactionMetadata.interruptible = throwsInterruptedException;
        transactionMetadata.familyName = createFamilyName(classMetadata.getName(), methodNode.name, methodNode.desc);
        transactionMetadata.timeout = -1;
        transactionMetadata.timeoutTimeUnit = TimeUnit.SECONDS;

        if (methodNode.name.equals("<init>")) {
            transactionMetadata.maxRetryCount = 0;
            transactionMetadata.smartTxLengthSelector = false;
        } else {
            transactionMetadata.maxRetryCount = 1000;
            transactionMetadata.smartTxLengthSelector = true;
        }

        return transactionMetadata;
    }

    private TransactionMetadata createTransactionMetadata(ClassMetadata classMetadata, MethodNode methodNode) {
        TransactionMetadata transactionMetadata = new TransactionMetadata();

        AnnotationNode annotationNode;
        if (methodNode.name.equals("<init>")) {
            annotationNode = AsmUtils.getVisibleAnnotation(methodNode, TransactionalConstructor.class);
        } else {
            annotationNode = AsmUtils.getVisibleAnnotation(methodNode, TransactionalMethod.class);
        }

        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);
        boolean throwsInterruptedException = methodMetadata.checkIfSpecificTransactionIsThrown(InterruptedException.class);
        transactionMetadata.readOnly = (Boolean) getValue(annotationNode, "readonly", false);
        transactionMetadata.familyName = createFamilyName(classMetadata.getName(), methodNode.name, methodNode.desc);
        transactionMetadata.interruptible = (Boolean) getValue(annotationNode, "interruptible", throwsInterruptedException);
        transactionMetadata.allowWriteSkewProblem = (Boolean) getValue(annotationNode, "allowWriteSkewProblem", true);
        boolean trackReadsDefault = !transactionMetadata.readOnly;
        transactionMetadata.automaticReadTracking = (Boolean) getValue(annotationNode, "automaticReadTracking", trackReadsDefault);
        transactionMetadata.timeout = ((Number) getValue(annotationNode, "timeout", -1)).longValue();
        transactionMetadata.timeoutTimeUnit = (TimeUnit) getValue(annotationNode, "timeoutTimeUnit", TimeUnit.SECONDS);

        if (methodNode.name.equals("<init>")) {
            transactionMetadata.maxRetryCount = (Integer) getValue(annotationNode, "maxRetryCount", 0);
            transactionMetadata.smartTxLengthSelector = false;
        } else {
            transactionMetadata.maxRetryCount = (Integer) getValue(annotationNode, "maxRetryCount", 1000);
            transactionMetadata.smartTxLengthSelector = true;
        }

        return transactionMetadata;
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
