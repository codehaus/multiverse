package org.multiverse.stms.alpha.instrumentation.fieldgranularity;

import org.multiverse.instrumentation.asm.CloneMap;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.instrumentation.metadata.FieldMetadata;
import org.multiverse.instrumentation.metadata.MetadataRepository;
import org.multiverse.instrumentation.metadata.MethodMetadata;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static org.multiverse.instrumentation.asm.AsmUtils.firstIndexAfterSuper;
import static org.multiverse.instrumentation.asm.AsmUtils.isCategory2;
import static org.objectweb.asm.Type.*;

public class FieldGranularityTransformer implements Opcodes {

    private final ClassNode classNode;
    private final ClassMetadata classMetadata;
    private final MetadataRepository metadataRepository;
    private final ClassLoader classLoader;

    public FieldGranularityTransformer(ClassLoader classLoader, ClassNode classNode, MetadataRepository metadataRepository) {
        if (classNode == null) {
            throw new RuntimeException();
        }

        this.metadataRepository = metadataRepository;
        this.classLoader = classLoader;
        this.classNode = classNode;
        this.classMetadata = metadataRepository.loadClassMetadata(classLoader, classNode.name);
    }

    public ClassNode transform() {
        fixFields();
        fixFieldAccessInMethods();
        addInitializationLogicToConstructors();            
        return classNode;
    }

    private void fixFieldAccessInMethods() {
        List<MethodNode> methods = new LinkedList<MethodNode>();

        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

            if (methodMetadata.isTransactional()) {
                methodNode = fixMethod(methodNode);
            }

            methods.add(methodNode);
        }

        classNode.methods = methods;
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
        for (LocalVariableNode localVariableNode : (List<LocalVariableNode>) originalMethod.localVariables) {

            LocalVariableNode cloned = new LocalVariableNode(
                    localVariableNode.name,
                    localVariableNode.desc,
                    localVariableNode.signature,
                    cloneMap.get(localVariableNode.start),
                    cloneMap.get(localVariableNode.end),
                    localVariableNode.index
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
        for (TryCatchBlockNode tryCatchBlockNode : (List<TryCatchBlockNode>) originalMethod.tryCatchBlocks) {

            TryCatchBlockNode cloned = new TryCatchBlockNode(
                    cloneMap.get(tryCatchBlockNode.start),
                    cloneMap.get(tryCatchBlockNode.end),
                    cloneMap.get(tryCatchBlockNode.handler),
                    tryCatchBlockNode.type
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
                //the put on the field granular field is transformed to a fieldref.set
                case PUTFIELD: {
                    FieldInsnNode fieldInsn = (FieldInsnNode) originalInsn;
                    ClassMetadata ownerMetadata = metadataRepository.loadClassMetadata(classLoader, fieldInsn.owner);
                    FieldMetadata fieldMetadata = ownerMetadata.getFieldMetadata(fieldInsn.name);
                    Type originalFieldType = Type.getType(fieldMetadata.getDesc());
                                                              
                    if (fieldMetadata.hasFieldGranularity()) {

                        boolean fieldIsCategory2 = isCategory2(fieldMetadata.getDesc());

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

                        String referenceDesc = findReferenceDesc(fieldMetadata.getDesc());
                        String referenceName = Type.getType(referenceDesc).getInternalName();

                        instructions.add(new FieldInsnNode(
                                GETFIELD,
                                fieldInsn.owner,
                                fieldInsn.name,
                                referenceDesc
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

                        //call the set
                        if (originalFieldType.getSort() == Type.ARRAY || originalFieldType.getSort() == Type.OBJECT) {
                            String objectDesc = Type.getDescriptor(Object.class);
                            MethodInsnNode methodInsn = new MethodInsnNode(
                                    INVOKEVIRTUAL,
                                    referenceName,
                                    "set",
                                    format("(%s)%s", objectDesc, objectDesc));
                            instructions.add(methodInsn);
                        } else {
                            MethodInsnNode methodInsn = new MethodInsnNode(
                                    INVOKEVIRTUAL,
                                    referenceName,
                                    "set",
                                    format("(%s)%s", fieldMetadata.getDesc(), fieldMetadata.getDesc()));
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
                //the get on the field granular field is transformed to a fieldref.get
                case GETFIELD: {
                    FieldInsnNode fieldInsn = (FieldInsnNode) originalInsn;
                    FieldMetadata fieldMetadata = metadataRepository.loadClassMetadata(classLoader, fieldInsn.owner)
                            .getFieldMetadata(fieldInsn.name);
                    if (!fieldMetadata.hasFieldGranularity()) {
                        //if it is not getter on a field granular field
                        instructions.add(originalInsn.clone(cloneMap));
                    } else {
                        //it is a getter on a field granular field.
                        String referenceDesc = findReferenceDesc(fieldMetadata.getDesc());
                        String referenceName = Type.getType(referenceDesc).getInternalName();

                        //place the fieldref on the stack.
                        instructions.add(new FieldInsnNode(
                                GETFIELD,
                                fieldInsn.owner,
                                fieldInsn.name,
                                referenceDesc
                        ));

                        Type originalFieldType = Type.getType(fieldMetadata.getDesc());
                        if (originalFieldType.getSort() == Type.ARRAY || originalFieldType.getSort() == Type.OBJECT) {
                            instructions.add(new MethodInsnNode(
                                    INVOKEVIRTUAL,
                                    referenceName,
                                    "get",
                                    format("()%s", getDescriptor(Object.class))
                            ));

                            if (!originalFieldType.equals(Type.getType(Object.class))) {
                                instructions.add(new TypeInsnNode(CHECKCAST, originalFieldType.getInternalName()));
                            }
                        } else {
                            instructions.add(new MethodInsnNode(
                                    INVOKEVIRTUAL,
                                    referenceName,
                                    "get",
                                    format("()%s", fieldMetadata.getDesc())
                            ));
                        }
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
        if (!classMetadata.hasFieldsWithFieldGranularity()) {
            return;
        }

        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

            if (methodMetadata.isTransactional() && methodMetadata.isConstructor()) {
                int firstAfterSuper = firstIndexAfterSuper(methodNode, classNode.superName);

                if (firstAfterSuper >= 0) {
                    InsnList extraInstructions = new InsnList();
                    for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
                        FieldMetadata fieldMetadata = classMetadata.getFieldMetadata(fieldNode.name);

                        if (fieldMetadata.hasFieldGranularity()) {
                            extraInstructions.add(new VarInsnNode(ALOAD, 0));

                            String referenceDesc = findReferenceDesc(fieldMetadata.getDesc());
                            String referenceName = Type.getType(referenceDesc).getInternalName();

                            extraInstructions.add(new TypeInsnNode(NEW, referenceName));
                            extraInstructions.add(new InsnNode(DUP));

                            extraInstructions.add(
                                    new MethodInsnNode(INVOKESPECIAL, referenceName, "<init>", "()V"));


                            extraInstructions.add(new FieldInsnNode(
                                    PUTFIELD,
                                    classNode.name,
                                    fieldNode.name,
                                    referenceDesc));
                        }

                        AbstractInsnNode first = methodNode.instructions.get(firstAfterSuper);
                        methodNode.instructions.insert(first, extraInstructions);
                    }
                }
            }
        }
    }

    private void fixFields() {
        List<FieldNode> fields = new LinkedList<FieldNode>();
        for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
            FieldMetadata fieldMetadata = classMetadata.getFieldMetadata(fieldNode.name);
            if (fieldMetadata.hasFieldGranularity()) {
                String referenceDesc = findReferenceDesc(fieldNode.desc);

                //todo: should not select public automatically
                FieldNode fixedFieldNode = new FieldNode(
                        ACC_SYNTHETIC + ACC_FINAL + ACC_PUBLIC,
                        fieldNode.name,
                        referenceDesc, null, null
                );

                fields.add(fixedFieldNode);
            } else {
                fields.add(fieldNode);
            }
        }

        classNode.fields = fields;
    }

    /**
     * Returns the reference/primitive class to store the
     */
    private static String findReferenceDesc(String desc) {
        Type type = Type.getType(desc);

        switch (type.getSort()) {
            case Type.ARRAY:
                return "Lorg/multiverse/transactional/DefaultTransactionalReference;";
            case Type.BOOLEAN:
                return "Lorg/multiverse/transactional/primitives/TransactionalBoolean;";
            case Type.BYTE:
                return "Lorg/multiverse/transactional/primitives/TransactionalByte;";
            case Type.CHAR:
                return "Lorg/multiverse/transactional/primitives/TransactionalCharacter;";
            case Type.DOUBLE:
                return "Lorg/multiverse/transactional/primitives/TransactionalDouble;";
            case Type.FLOAT:
                return "Lorg/multiverse/transactional/primitives/TransactionalFloat;";
            case Type.INT:
                return "Lorg/multiverse/transactional/primitives/TransactionalInteger;";
            case Type.LONG:
                return "Lorg/multiverse/transactional/primitives/TransactionalLong;";
            case Type.SHORT:
                return "Lorg/multiverse/transactional/primitives/TransactionalShort;";
            case Type.OBJECT:
                return "Lorg/multiverse/transactional/DefaultTransactionalReference;";
            default:
                throw new IllegalStateException("Unhandeled sort: " + type.getSort());
        }
    }
}