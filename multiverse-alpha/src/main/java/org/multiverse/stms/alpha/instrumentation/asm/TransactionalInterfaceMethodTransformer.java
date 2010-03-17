package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.instrumentation.metadata.MetadataRepository;
import org.multiverse.instrumentation.metadata.MethodMetadata;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedList;
import java.util.List;

import static org.multiverse.instrumentation.asm.AsmUtils.createMethodDescriptorWithRightIntroducedVariable;

/**
 * Responsible for transforming Transactional interfaces.
 * <p/>
 * The transformation is simple, for every method methodname(arg1..argn) an additional method
 * is created: metodname(arg1..argn,AlphaTransaction) is created (also abstract ofcourse).
 * <p/>
 * This last method can be used if a transaction already is available, and instead of going
 * through the original method that does the transaction management, go to the method that
 * contains the logic.
 *
 * @author Peter Veentjer.
 */
public class TransactionalInterfaceMethodTransformer {

    private final ClassNode classNode;
    private final MetadataRepository metadataRepository;
    private final ClassMetadata classMetadata;
    private final ClassLoader classLoader;

    public TransactionalInterfaceMethodTransformer(ClassLoader classLoader, ClassNode classNode, MetadataRepository metadataRepository) {
        if (classLoader == null || classNode == null) {
            throw new NullPointerException();
        }

        this.classLoader = classLoader;
        this.metadataRepository = metadataRepository;
        this.classNode = classNode;
        this.classMetadata = metadataRepository.getClassMetadata(classLoader, classNode.name);
    }

    public ClassNode transform() {


        if (ignore()) {
            return null;
        }

        classNode.methods = createInterfaceMethods();
        return classNode;
    }

    private boolean ignore() {
        if (classMetadata.isIgnoredClass()) {
            return true;
        }

        return false;
    }

    private List<MethodNode> createInterfaceMethods() {
        List<MethodNode> methods = new LinkedList<MethodNode>();

        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

            //if the method is transactional, a version with introduced transaction argument needs to be added.
            if (methodMetadata.isTransactional()) {
                MethodNode transactionMethodNode = createInterfaceTransactionMethod(methodNode);
                methods.add(transactionMethodNode);
            }

            //and the original one should be added as well.
            methods.add(methodNode);
        }

        return methods;
    }

    private static MethodNode createInterfaceTransactionMethod(MethodNode methodNode) {
        MethodNode transactionMethod = new MethodNode();
        transactionMethod.access = methodNode.access;//todo: should be made synthetic.
        transactionMethod.name = methodNode.name;
        transactionMethod.exceptions = methodNode.exceptions;
        transactionMethod.desc = createMethodDescriptorWithRightIntroducedVariable(
                methodNode.desc, Type.getInternalName(AlphaTransaction.class));
        return transactionMethod;
    }
}
