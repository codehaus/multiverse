package org.multiverse.stms.alpha.instrumentation.gettersetter;

import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.instrumentation.metadata.FieldMetadata;
import org.multiverse.instrumentation.metadata.MetadataRepository;
import org.multiverse.instrumentation.metadata.MethodMetadata;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.LinkedList;
import java.util.List;

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

        if (skip(methodMetadata)) {
            return originalMethodNode;
        }

        InsnList newInstructions = new InsnList();
        for (int k = 0; k < originalMethodNode.instructions.size(); k++) {
            AbstractInsnNode originalInsn = originalMethodNode.instructions.get(k);
            if (originalInsn.getOpcode() == INVOKEVIRTUAL) {
                newInstructions.add(optimizeInvokeVirtual((MethodInsnNode) originalInsn));
            } else {
                newInstructions.add(originalInsn);
            }
        }

        originalMethodNode.instructions = newInstructions;
        return originalMethodNode;
    }

    private AbstractInsnNode optimizeInvokeVirtual(MethodInsnNode methodInsnNode) {
        ClassMetadata ownerMetadata = metadataRepository.loadClassMetadata(classLoader, methodInsnNode.owner);

        MethodMetadata calleeMetadata = ownerMetadata.getMethodMetadata(
                methodInsnNode.name, methodInsnNode.desc);

        if (!ownerMetadata.isTransactionalObject()
                || calleeMetadata == null
                || !calleeMetadata.isFinal()) {
            return methodInsnNode;
        }

        FieldMetadata fieldMetadata = calleeMetadata.getGetterSetterField();
        switch (calleeMetadata.getMethodType()) {
            case getter:
                return new FieldInsnNode(
                        GETFIELD, ownerMetadata.getName(), fieldMetadata.getName(), fieldMetadata.getDesc());
            case setter:
                return new FieldInsnNode(
                        PUTFIELD, ownerMetadata.getName(), fieldMetadata.getName(), fieldMetadata.getDesc());
            case unknown:
                return methodInsnNode;
            default:
                throw new IllegalStateException();
        }
    }

    private boolean skip(MethodMetadata methodMetadata) {
        return methodMetadata.isAbstract()
                || methodMetadata.isNative()
                || !methodMetadata.isTransactional();
    }
}
