package org.multiverse.instrumentation.asm;

import org.multiverse.annotations.*;
import org.multiverse.api.LogLevel;
import org.multiverse.instrumentation.metadata.*;
import org.multiverse.utils.IOUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.multiverse.instrumentation.asm.AsmUtils.isFinal;
import static org.multiverse.instrumentation.asm.AsmUtils.isStatic;

/**
 * An Asm based {@link org.multiverse.instrumentation.metadata.ClassMetadataExtractor}.
 *
 * @author Peter Veentjer.
 */
public final class AsmClassMetadataExtractor implements ClassMetadataExtractor, Opcodes {

    private MetadataRepository metadataRepository;
    private FamilyNameStrategy familyNameStrategy;

    public AsmClassMetadataExtractor() {
        this(new CompactFamilyNameStrategy());
    }

    public AsmClassMetadataExtractor(FamilyNameStrategy familyNameStrategy) {
        if (familyNameStrategy == null) {
            throw new NullPointerException();
        }
        this.familyNameStrategy = familyNameStrategy;
    }

    @Override
    public void init(MetadataRepository metadataRepository) {
        if (metadataRepository == null) {
            throw new NullPointerException();
        }
        this.metadataRepository = metadataRepository;
    }

    @Override
    public ClassMetadata extract(String className, ClassLoader classLoader) {
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
                ClassMetadata superClassMetadata = metadataRepository.loadClassMetadata(classLoader, classNode.superName);
                classMetadata.setSuperClassMetadata(superClassMetadata);
            }

            for (String interfaceName : (List<String>) classNode.interfaces) {
                ClassMetadata interfaceMetadata = metadataRepository.loadClassMetadata(classLoader, interfaceName);
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
            IOUtils.closeQuietly(is);
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

        extractSetterMetadata(methodMetadata, methodNode);
        extractGetterMetadata(methodMetadata, methodNode);
    }

    private void extractSetterMetadata(MethodMetadata methodMetadata, MethodNode methodNode) {
        if (methodMetadata.isStatic() || methodMetadata.isNative() || methodMetadata.isAbstract()) {
            return;
        }

        Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
        if (argTypes.length != 1) {
            return;
        }

        Type retType = Type.getReturnType(methodNode.desc);
        if (!Type.VOID_TYPE.equals(retType)) {
            return;
        }

        List<AbstractInsnNode> filteredInstructions = filterNoOps(methodNode.instructions);
        if (filteredInstructions.size() != 4) {
            return;
        }

        if (filteredInstructions.get(0).getOpcode() != ALOAD) {
            return;
        }

        VarInsnNode insn1 = (VarInsnNode) filteredInstructions.get(0);
        if (insn1.var != 0) {
            return;
        }

        if (filteredInstructions.get(1).getType() != AbstractInsnNode.VAR_INSN) {
            return;
        }

        VarInsnNode insn2 = (VarInsnNode) filteredInstructions.get(1);
        if (insn2.var != 1) {
            return;
        }

        if (filteredInstructions.get(2).getOpcode() != PUTFIELD) {
            return;
        }
        FieldInsnNode insn3 = (FieldInsnNode) filteredInstructions.get(2);

        if (filteredInstructions.get(3).getOpcode() != RETURN) {
            return;
        }

        FieldMetadata field = methodMetadata.getClassMetadata().getFieldMetadata(insn3.name);
        methodMetadata.setGetterSetter(MethodType.setter, field);
    }

    private void extractGetterMetadata(MethodMetadata methodMetadata, MethodNode methodNode) {
        if (methodMetadata.isStatic() || methodMetadata.isNative() || methodMetadata.isAbstract()) {
            return;
        }

        Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
        if (argTypes.length != 0) {
            return;
        }

        Type retType = Type.getReturnType(methodNode.desc);
        if (Type.VOID_TYPE.equals(retType)) {
            return;
        }

        List<AbstractInsnNode> filteredInstructions = filterNoOps(methodNode.instructions);

        if (filteredInstructions.size() != 3) {
            return;
        }

        AbstractInsnNode instr1 = filteredInstructions.get(0);
        if (instr1.getOpcode() != ALOAD) {
            return;
        }

        VarInsnNode varInsnNode = (VarInsnNode) instr1;
        if (varInsnNode.var != 0) {
            return;
        }

        AbstractInsnNode instr2 = filteredInstructions.get(1);
        if (instr2.getOpcode() != GETFIELD) {
            return;
        }

        AbstractInsnNode instr3 = filteredInstructions.get(2);
        switch (instr3.getOpcode()) {
            case IRETURN:
                break;
            case LRETURN:
                break;
            case FRETURN:
                break;
            case DRETURN:
                break;
            case ARETURN:
                break;
            default:
                return;
        }

        FieldInsnNode fieldInsnNode = (FieldInsnNode) instr2;

        FieldMetadata field = methodMetadata.getClassMetadata().getFieldMetadata(fieldInsnNode.name);
        methodMetadata.setGetterSetter(MethodType.getter, field);
    }

    private List<AbstractInsnNode> filterNoOps(InsnList instructions) {
        List<AbstractInsnNode> result = new LinkedList<AbstractInsnNode>();

        if (instructions == null) {
            return result;
        }

        for (int k = 0; k < instructions.size(); k++) {
            AbstractInsnNode node = instructions.get(k);
            if (node.getOpcode() != -1) {
                result.add(node);
            }
        }

        return result;
    }

    private boolean isInvisibleMethod(MethodNode methodNode) {
        return isExcluded(methodNode)
                || isSynthetic(methodNode.access);
    }

    private FieldMetadata extractFieldMetadata(ClassMetadata classMetadata, FieldNode fieldNode) {
        FieldMetadata fieldMetadata = classMetadata.createFieldMetadata(fieldNode.name);
        fieldMetadata.setAccess(fieldNode.access);
        fieldMetadata.setDesc(fieldNode.desc);

        if (isManagedField(classMetadata, fieldNode)) {
            fieldMetadata.setIsManaged(true);
        } else if (isManagedFieldWithFieldGranularity(classMetadata, fieldNode)) {
            fieldMetadata.setIsManaged(true);
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

        ClassMetadata superClassMetadata = metadataRepository.loadClassMetadata(classLoader, classNode.superName);
        if (superClassMetadata.isTransactionalObject()) {
            return true;
        }

        for (String interfaceName : (List<String>) classNode.interfaces) {
            ClassMetadata interfaceMetadata = metadataRepository.loadClassMetadata(classLoader, interfaceName);
            if (interfaceMetadata.isTransactionalObject()) {
                return true;
            }
        }

        return false;
    }

    private boolean isManagedFieldWithFieldGranularity(ClassMetadata classMetadata, FieldNode field) {
        return classMetadata.isTransactionalObject()
                && hasFieldGranularity(field)
                && !isInvisibleField(field);
    }

    /**
     * Checks if the field is a managed field of a transactional object.
     */
    private boolean isManagedField(ClassMetadata classMetadata, FieldNode field) {
        return classMetadata.isTransactionalObject()
                && !isInvisibleField(field)
                && !hasFieldGranularity(field);
    }

    private boolean isInvisibleField(FieldNode fieldNode) {
        return isExcluded(fieldNode)
                || isFinal(fieldNode)
                || isStatic(fieldNode)
                || isSynthetic(fieldNode.access)
                || isVolatile(fieldNode);
    }

    private TransactionMetadata createDefaultTransactionMetadata(ClassMetadata classMetadata, MethodNode methodNode) {
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);
        boolean throwsInterruptedException = methodMetadata.checkIfSpecificTransactionIsThrown(InterruptedException.class);

        TransactionMetadata transactionMetadata = new TransactionMetadata();

        if (methodNode.name.equals("<init>")) {
            transactionMetadata.maxRetries = 0;
            transactionMetadata.speculativeConfigurationEnabled = false;
        } else {
            transactionMetadata.maxRetries = 1000;
            transactionMetadata.speculativeConfigurationEnabled = true;
        }

        transactionMetadata.logLevel = LogLevel.none;
        transactionMetadata.readOnly = null;
        transactionMetadata.trackReads = null;
        transactionMetadata.writeSkew = true;
        transactionMetadata.interruptible = throwsInterruptedException;
        transactionMetadata.familyName = familyNameStrategy.create(classMetadata.getName(), methodNode.name, methodNode.desc);
        transactionMetadata.timeoutNs = Long.MAX_VALUE;
        return transactionMetadata;
    }

    private TransactionMetadata createTransactionMetadata(ClassMetadata classMetadata, MethodNode methodNode) {
        TransactionMetadata txMetadata = new TransactionMetadata();

        AnnotationNode annotationNode;
        if (methodNode.name.equals("<init>")) {
            annotationNode = AsmUtils.getVisibleAnnotation(methodNode, TransactionalConstructor.class);
        } else {
            annotationNode = AsmUtils.getVisibleAnnotation(methodNode, TransactionalMethod.class);
        }

        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);
        boolean throwsInterruptedException = methodMetadata.checkIfSpecificTransactionIsThrown(InterruptedException.class);
        txMetadata.readOnly = (Boolean) getValue(annotationNode, "readonly", null);
        txMetadata.familyName = familyNameStrategy.create(classMetadata.getName(), methodNode.name, methodNode.desc);
        txMetadata.interruptible = (Boolean) getValue(annotationNode, "interruptible", throwsInterruptedException);
        txMetadata.writeSkew = (Boolean) getValue(annotationNode, "writeSkew", true);

        if (txMetadata.writeSkew) {
            txMetadata.trackReads = (Boolean) getValue(annotationNode, "trackReads", null);
        } else {
            Boolean tracking = (Boolean) getValue(annotationNode, "trackReads", null);
            if (tracking == null || tracking) {
                txMetadata.trackReads = true;
            } else {
                //String msg = "method "+methodMetadata.toFullName()+" has automatic readtracking disabled"
                //throw new RuntimeException();
            }
        }

        long timeout = ((Number) getValue(annotationNode, "timeout", Long.MAX_VALUE)).longValue();
        String[] unit = (String[]) getValue(annotationNode, "timeoutTimeUnit", new String[]{null, TimeUnit.SECONDS.name()});
        TimeUnit timeoutTimeUnit = TimeUnit.valueOf(unit[1]);
        if (timeout == Long.MAX_VALUE) {
            txMetadata.timeoutNs = Long.MAX_VALUE;
        } else {
            txMetadata.timeoutNs = timeoutTimeUnit.toNanos(timeout);
        }

        String[] logLevels = (String[]) getValue(annotationNode, "logLevel", new String[]{null, LogLevel.none.name()});
        txMetadata.logLevel = LogLevel.valueOf(logLevels[1]);

        if (methodNode.name.equals("<init>")) {
            txMetadata.maxRetries = (Integer) getValue(annotationNode, "maxRetries", 0);
            txMetadata.speculativeConfigurationEnabled = false;
        } else {
            txMetadata.maxRetries = (Integer) getValue(annotationNode, "maxRetries", 1000);
            txMetadata.speculativeConfigurationEnabled = true;
        }

        return txMetadata;
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
        return AsmUtils.hasVisibleAnnotation(field, NonTransactional.class);
    }

    public static boolean isExcluded(MethodNode methodNode) {
        return AsmUtils.hasVisibleAnnotation(methodNode, NonTransactional.class);
    }

    public static boolean hasFieldGranularity(FieldNode field) {
        return AsmUtils.hasVisibleAnnotation(field, FieldGranularity.class);
    }

    public static boolean hasTransactionalMethodAnnotation(MethodNode methodNode) {
        return AsmUtils.hasVisibleAnnotation(methodNode, TransactionalMethod.class);
    }

    public static boolean hasTransactionalConstructorAnnotation(MethodNode methodNode) {
        return AsmUtils.hasVisibleAnnotation(methodNode, TransactionalConstructor.class);
    }

    public static boolean hasTransactionalObjectAnnotation(ClassNode classNode) {
        return AsmUtils.hasVisibleAnnotation(classNode, TransactionalObject.class);
    }

    public static boolean isSynthetic(int access) {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public static boolean isVolatile(FieldNode fieldNode) {
        return (fieldNode.access & Opcodes.ACC_VOLATILE) != 0;
    }
}
