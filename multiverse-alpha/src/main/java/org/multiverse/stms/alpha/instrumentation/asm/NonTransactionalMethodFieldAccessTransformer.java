package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.instrumentation.metadata.MetadataRepository;
import org.multiverse.instrumentation.metadata.MethodMetadata;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedList;
import java.util.List;

/**
 * Transforms TransactionalObjects so that access to fields in non transactional method is transformed
 */
public class NonTransactionalMethodFieldAccessTransformer implements Opcodes {

    private final ClassNode classNode;
    private final MetadataRepository metadataRepository;
    private final ClassMetadata classMetadata;
    private final ClassLoader classLoader;

    public NonTransactionalMethodFieldAccessTransformer(ClassLoader classLoader, ClassNode classNode, MetadataRepository metadataRepository) {
        if (classLoader == null || classNode == null) {
            throw new NullPointerException();
        }

        this.metadataRepository = metadataRepository;
        this.classNode = classNode;
        this.classLoader = classLoader;
        this.classMetadata = metadataRepository.getClassMetadata(classLoader, classNode.name);
    }

    public ClassNode transform() {
        if (classMetadata.isIgnoredClass()) {
            return null;
        }

        fixMethods();
        return classNode;
    }

    private void fixMethods() {
        List<MethodNode> fixedMethods = new LinkedList<MethodNode>();

        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            MethodNode fixedMethod = fixMethod(methodNode);
            fixedMethods.add(fixedMethod);
        }

        classNode.methods = fixedMethods;
    }

    private MethodNode fixMethod(MethodNode methodNode) {
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

        if (methodMetadata == null ||
                methodMetadata.isAbstract() ||
                methodMetadata.isNative() ||
                methodMetadata.isTransactional()) {
            return methodNode;
        }

        MethodNode fixedMethod = new MethodNode();
        fixedMethod.access = methodNode.access;
        fixedMethod.localVariables = new LinkedList();
        fixedMethod.name = methodNode.name;
        fixedMethod.desc = methodNode.desc;
        fixedMethod.exceptions = methodNode.exceptions;
        fixedMethod.tryCatchBlocks = new LinkedList();//originalMethod.tryCatchBlocks;

        methodNode.accept(new NonTransactionalMethodFieldAccessMethodAdapter(classLoader, fixedMethod, metadataRepository));

        return fixedMethod;
    }
}
