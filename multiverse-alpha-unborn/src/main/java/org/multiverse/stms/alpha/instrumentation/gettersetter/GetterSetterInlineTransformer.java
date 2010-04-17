package org.multiverse.stms.alpha.instrumentation.gettersetter;

import org.multiverse.instrumentation.asm.CloneMap;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.instrumentation.metadata.FieldMetadata;
import org.multiverse.instrumentation.metadata.MetadataRepository;
import org.multiverse.instrumentation.metadata.MethodMetadata;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.LinkedList;
import java.util.List;

import static org.multiverse.instrumentation.asm.AsmUtils.cloneMethodWithoutInstructions;

/**
 * @author Peter Veentjer
 */
public final class GetterSetterInlineTransformer implements Opcodes {
    private final ClassNode originalClassNode;
    private final ClassMetadata classMetadata;
    private final MetadataRepository metadataRepository;
    private final ClassLoader classLoader;

    public GetterSetterInlineTransformer(
            ClassNode originalClassNode, ClassMetadata classMetadata,
            MetadataRepository metadataRepository, ClassLoader classLoader) {

        this.classLoader = classLoader;
        this.originalClassNode = originalClassNode;
        this.classMetadata = classMetadata;
        this.metadataRepository = metadataRepository;
    }

    public ClassNode transform() {
        List<MethodNode> newMethods = new LinkedList<MethodNode>();
        for (MethodNode originalMethodNode : (List<MethodNode>) originalClassNode.methods) {
            MethodNode transformed = transform(originalMethodNode);
            newMethods.add(transformed);
        }
        originalClassNode.methods = newMethods;
        return originalClassNode;
    }

    private MethodNode transform(MethodNode originalMethodNode) {
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(
                originalMethodNode.name, originalMethodNode.desc);

        if (skipMethod(methodMetadata)) {
            return originalMethodNode;
        }

        CloneMap cloneMap = new CloneMap();
        MethodNode result = cloneMethodWithoutInstructions(originalMethodNode, cloneMap);

        InsnList newInstructions = new InsnList();
        for (int k = 0; k < originalMethodNode.instructions.size(); k++) {
            AbstractInsnNode originalInsn = originalMethodNode.instructions.get(k);
            switch (originalInsn.getOpcode()) {
                case INVOKEVIRTUAL:
                case INVOKESPECIAL:
                    AbstractInsnNode fixedInsn = optimizeInvoke((MethodInsnNode) originalInsn, cloneMap);
                    newInstructions.add(fixedInsn);
                    break;
                default:
                    newInstructions.add(originalInsn.clone(cloneMap));
                    break;
            }
        }

        result.instructions = newInstructions;
        return result;
    }

    private AbstractInsnNode optimizeInvoke(MethodInsnNode methodInsnNode, CloneMap cloneMap) {
        ClassMetadata ownerMetadata = metadataRepository.loadClassMetadata(classLoader, methodInsnNode.owner);

        MethodMetadata calleeMetadata = ownerMetadata.getMethodMetadata(
                methodInsnNode.name, methodInsnNode.desc);

        boolean implementationKnown = classMetadata.isFinal() || methodInsnNode.getOpcode() == INVOKESPECIAL;

        FieldMetadata fieldMetadata = calleeMetadata.getGetterSetterField();

        if (!ownerMetadata.isTransactionalObject()
                || calleeMetadata == null
                || !implementationKnown
                || !fieldMetadata.isManagedField()) {
            return methodInsnNode.clone(cloneMap);
        }


        switch (calleeMetadata.getMethodType()) {
            case getter:
                return new FieldInsnNode(
                        GETFIELD,
                        ownerMetadata.getName(),
                        fieldMetadata.getName(),
                        fieldMetadata.getDesc());
            case setter:
                return new FieldInsnNode(
                        PUTFIELD,
                        ownerMetadata.getName(),
                        fieldMetadata.getName(),
                        fieldMetadata.getDesc());
            case unknown:
                return methodInsnNode;
            default:
                throw new IllegalStateException();
        }
    }

    private boolean skipMethod(MethodMetadata methodMetadata) {
        return methodMetadata.isAbstract()
                || methodMetadata.isNative()
                || !methodMetadata.isTransactional();
    }
}
