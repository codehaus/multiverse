package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

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
        this.classMetadata = metadataRepository.loadClassMetadata(classLoader, classNode.name);
    }

    public ClassNode transform() {
        classNode.methods = createInterfaceMethods();
        return classNode;
    }

    private List<MethodNode> createInterfaceMethods() {
        List<MethodNode> methods = new LinkedList<MethodNode>();

        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

            //if the method is transactional, a version with introduced transaction argument needs to be added.
            if (methodMetadata.isTransactional()) {
                methods.add(createInterfaceTransactionMethod(methodNode, true));
                methods.add(createInterfaceTransactionMethod(methodNode, false));
            }

            //and the original one should be added as well.
            methods.add(methodNode);
        }

        return methods;
    }

    private static MethodNode createInterfaceTransactionMethod(MethodNode methodNode, boolean readonly) {
        MethodNode transactionMethod = new MethodNode();
        transactionMethod.access = methodNode.access;//todo: should be made synthetic.
        transactionMethod.name = TransactionalMethodUtils.toTransactedMethodName(methodNode.name, readonly);
        transactionMethod.exceptions = methodNode.exceptions;
        //todo: better signature should be used here
        //transactionMethod.signature = methodNode.signature;
        transactionMethod.desc = createMethodDescriptorWithRightIntroducedVariable(
                methodNode.desc, Type.getInternalName(AlphaTransaction.class));
        return transactionMethod;
    }
}
