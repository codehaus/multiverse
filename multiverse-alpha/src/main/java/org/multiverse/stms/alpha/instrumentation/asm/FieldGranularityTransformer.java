package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.transactional.DefaultTransactionalReference;
import org.multiverse.transactional.primitives.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.firstIndexAfterSuper;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.isCategory2;
import static org.objectweb.asm.Type.*;

/**
 *
 */
public class FieldGranularityTransformer implements Opcodes {

    private final ClassNode originalClass;
    private final MetadataRepository metadataRepository;
    private final List<FieldNode> fieldsWithFieldGranularity;

    public FieldGranularityTransformer(ClassNode originalClass) {
        if (originalClass == null) {
            throw new RuntimeException();
        }
        this.originalClass = originalClass;
        this.metadataRepository = MetadataRepository.INSTANCE;
        fieldsWithFieldGranularity = metadataRepository.getManagedInstanceFieldsWithFieldGranularity(originalClass);

    }

    public ClassNode transform() {
        if (fieldsWithFieldGranularity.isEmpty()) {
            return originalClass;
        }

        fixFields();
        fixFieldAccessInMethods();
        addInitializationLogicToConstructors();
        return originalClass;
    }

    private void fixFieldAccessInMethods() {
        for (MethodNode originalMethod : metadataRepository.getTransactionalMethods(originalClass)) {
            MethodNode fixedMethod = fixMethod(originalMethod);
            originalClass.methods.remove(originalMethod);
            originalClass.methods.add(fixedMethod);
        }
    }

    private MethodNode fixMethod(MethodNode originalMethod) {
        CloneMap cloneMap = new CloneMap();

        MethodNode result = new MethodNode();
        result.name = originalMethod.name;
        result.access = originalMethod.access;
        result.desc = originalMethod.desc;
        result.signature = originalMethod.signature;
        result.exceptions = originalMethod.exceptions;
        result.annotationDefault = originalMethod.annotationDefault;
        result.invisibleParameterAnnotations = originalMethod.invisibleParameterAnnotations;
        result.visibleParameterAnnotations = originalMethod.visibleParameterAnnotations;
        result.localVariables = createLocalVariableTable(originalMethod, cloneMap);
        result.tryCatchBlocks = createTryCatchBlocks(originalMethod, cloneMap);
        result.instructions = createInstructions(originalMethod, cloneMap);
        return result;
    }

    private List<LocalVariableNode> createLocalVariableTable(MethodNode originalMethod, CloneMap cloneMap) {
        if (originalMethod.localVariables == null) {
            return null;
        }

        List<LocalVariableNode> localVariableTable = new LinkedList<LocalVariableNode>();
        for (LocalVariableNode original : (List<LocalVariableNode>) originalMethod.localVariables) {
            LocalVariableNode cloned = new LocalVariableNode(
                    original.name,
                    original.desc,
                    original.signature,
                    cloneMap.get(original.start),
                    cloneMap.get(original.end),
                    original.index
            );
            localVariableTable.add(cloned);
        }
        return localVariableTable;
    }

    private List<TryCatchBlockNode> createTryCatchBlocks(MethodNode originalMethod, CloneMap cloneMap) {
        if (originalMethod.tryCatchBlocks == null) {
            return null;
        }

        List<TryCatchBlockNode> tryCatchBlocks = new LinkedList<TryCatchBlockNode>();
        for (TryCatchBlockNode original : (List<TryCatchBlockNode>) originalMethod.tryCatchBlocks) {
            TryCatchBlockNode cloned = new TryCatchBlockNode(
                    cloneMap.get(original.start),
                    cloneMap.get(original.end),
                    cloneMap.get(original.handler),
                    original.type
            );

            tryCatchBlocks.add(cloned);
        }
        return tryCatchBlocks;
    }

    private InsnList createInstructions(MethodNode originalMethod, CloneMap cloneMap) {
        InsnList instructions = new InsnList();

        for (int k = 0; k < originalMethod.instructions.size(); k++) {
            AbstractInsnNode originalInsn = originalMethod.instructions.get(k);
            switch (originalInsn.getOpcode()) {
                case PUTFIELD: {
                    FieldInsnNode fieldInsn = (FieldInsnNode) originalInsn;
                    if (metadataRepository.isManagedInstanceFieldWithFieldGranularity(fieldInsn.owner, fieldInsn.name)) {
                        Type originalFieldType = getType(fieldInsn.desc);
                        boolean fieldIsCategory2 = isCategory2(fieldInsn.desc);
                        if (fieldIsCategory2) {
                            //value(category2), owner,..

                            instructions.add(new InsnNode(DUP2_X1));
                            //[value(category2), owner, value(category2),...]

                            instructions.add(new InsnNode(POP2));
                            //[owner, value(category2), ...]
                        } else {
                            //[value(category1), owner,
                            instructions.add(new InsnNode(SWAP));
                            //[owner, value(category1),..
                        }

                        Class referenceClass = findReferenceClass(fieldInsn.desc);

                        instructions.add(new FieldInsnNode(
                                GETFIELD,
                                fieldInsn.owner,
                                fieldInsn.name,
                                getDescriptor(referenceClass)
                        ));

                        if (fieldIsCategory2) {
                            //[owner, value(category2),..

                            instructions.add(new InsnNode(DUP_X2));
                            //[owner, value(category2), owner

                            instructions.add(new InsnNode(POP));
                            //[value(category2), owner
                        } else {
                            //[owner, value(category1)
                            instructions.add(new InsnNode(SWAP));
                            //[value(category1), owner..
                        }

                        //call the set.
                        if (originalFieldType.getSort() == Type.ARRAY || originalFieldType.getSort() == Type.OBJECT) {
                            String objectDesc = Type.getDescriptor(Object.class);
                            MethodInsnNode methodInsn = new MethodInsnNode(
                                    INVOKEVIRTUAL,
                                    Type.getInternalName(referenceClass),
                                    "set",
                                    format("(%s)%s", objectDesc, objectDesc));
                            instructions.add(methodInsn);
                        } else {
                            MethodInsnNode methodInsn = new MethodInsnNode(
                                    INVOKEVIRTUAL,
                                    Type.getInternalName(referenceClass),
                                    "set",
                                    format("(%s)%s", fieldInsn.desc, fieldInsn.desc));
                            instructions.add(methodInsn);
                        }

                        //pop the unused return value of the set.
                        if (fieldIsCategory2) {
                            instructions.add(new InsnNode(POP2));
                        } else {
                            instructions.add(new InsnNode(POP));
                        }
                    } else {
                        instructions.add(originalInsn.clone(cloneMap));
                    }
                }
                break;
                case GETFIELD: {
                    FieldInsnNode fieldInsn = (FieldInsnNode) originalInsn;
                    if (metadataRepository.isManagedInstanceFieldWithFieldGranularity(fieldInsn.owner, fieldInsn.name)) {
                        Class refClass = findReferenceClass(fieldInsn.desc);

                        instructions.add(new FieldInsnNode(
                                GETFIELD,
                                fieldInsn.owner,
                                fieldInsn.name,
                                getDescriptor(refClass)
                        ));

                        Type originalFieldType = getType(fieldInsn.desc);
                        if (originalFieldType.getSort() == Type.ARRAY || originalFieldType.getSort() == Type.OBJECT) {
                            instructions.add(new MethodInsnNode(
                                    INVOKEVIRTUAL,
                                    getInternalName(refClass),
                                    "get",
                                    format("()%s", getDescriptor(Object.class))
                            ));

                            if (!originalFieldType.equals(Type.getType(Object.class))) {
                                instructions.add(new TypeInsnNode(CHECKCAST, originalFieldType.getInternalName()));
                            }
                        } else {
                            instructions.add(new MethodInsnNode(
                                    INVOKEVIRTUAL,
                                    getInternalName(refClass),
                                    "get",
                                    format("()%s", fieldInsn.desc)
                            ));
                        }
                    } else {
                        instructions.add(originalInsn.clone(cloneMap));
                    }
                }
                break;
                default:
                    instructions.add(originalInsn.clone(cloneMap));
                    break;
            }
        }

        return instructions;
    }

    private void addInitializationLogicToConstructors() {
        if (fieldsWithFieldGranularity.isEmpty()) {
            return;
        }

        for (MethodNode methodNode : metadataRepository.getTransactionalMethods(originalClass)) {
            if (methodNode.name.equals("<init>")) {
                int firstAfterSuper = firstIndexAfterSuper(methodNode, originalClass.superName);

                if (firstAfterSuper >= 0) {
                    InsnList extraInstructions = new InsnList();
                    for (FieldNode originalField : fieldsWithFieldGranularity) {
                        extraInstructions.add(new VarInsnNode(ALOAD, 0));

                        Class referenceClass = findReferenceClass(originalField.desc);
                        extraInstructions.add(new TypeInsnNode(NEW, getInternalName(referenceClass)));
                        extraInstructions.add(new InsnNode(DUP));

                        String owner = getInternalName(referenceClass);
                        extraInstructions.add(new MethodInsnNode(INVOKESPECIAL, owner, "<init>", "()V"));

                        extraInstructions.add(new FieldInsnNode(
                                PUTFIELD,
                                originalClass.name,
                                originalField.name,
                                Type.getDescriptor(referenceClass)));
                    }

                    AbstractInsnNode first = methodNode.instructions.get(firstAfterSuper);
                    methodNode.instructions.insert(first, extraInstructions);
                }
            }
        }
    }

    private void fixFields() {
        for (FieldNode originalField : fieldsWithFieldGranularity) {
            Class referenceClass = findReferenceClass(originalField.desc);

            //todo: should not select public automatically
            FieldNode fixedField = new FieldNode(
                    ACC_SYNTHETIC + ACC_FINAL + ACC_PUBLIC,
                    originalField.name,
                    getDescriptor(referenceClass), null, null
            );

            originalClass.fields.remove(originalField);
            originalClass.fields.add(fixedField);
        }
    }

    /**
     * Returns the reference/primitive class to store the
     */
    private static Class findReferenceClass(String desc) {
        Type type = Type.getType(desc);
        switch (type.getSort()) {
            case Type.ARRAY:
                return DefaultTransactionalReference.class;
            case Type.BOOLEAN:
                return TransactionalBoolean.class;
            case Type.BYTE:
                return TransactionalByte.class;
            case Type.CHAR:
                return TransactionalCharacter.class;
            case Type.DOUBLE:
                return TransactionalDouble.class;
            case Type.FLOAT:
                return TransactionalFloat.class;
            case Type.INT:
                return TransactionalInteger.class;
            case Type.LONG:
                return TransactionalLong.class;
            case Type.OBJECT:
                return DefaultTransactionalReference.class;
            case Type.SHORT:
                return TransactionalShort.class;
            default:
                throw new IllegalStateException("Unhandeled sort: " + type.getSort());
        }
    }
}
