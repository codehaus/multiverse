package org.multiverse.stms.alpha.instrumentation.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedList;
import java.util.List;

import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.isAbstract;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.isNative;

/**
 * Transforms TransactionalObjects so that access
 */
public class TransactionalObjectFieldAccessTransformer implements Opcodes {

    private final ClassNode originalClass;

    public TransactionalObjectFieldAccessTransformer(ClassNode originalClass) {
        this.originalClass = originalClass;
    }

    public ClassNode transform() {
        fixMethods();
        return originalClass;
    }

    private void fixMethods() {
        List<MethodNode> fixedMethods = new LinkedList<MethodNode>();

        for (MethodNode originalMethod : (List<MethodNode>) originalClass.methods) {
            MethodNode fixedMethod = fixMethod(originalMethod);
            fixedMethods.add(fixedMethod);
        }

        originalClass.methods = fixedMethods;
    }

    private MethodNode fixMethod(MethodNode originalMethod) {
        if (isAbstract(originalMethod) || isNative(originalMethod)) {
            return originalMethod;
        }

        MethodNode fixedMethod = new MethodNode();
        fixedMethod.access = originalMethod.access;
        fixedMethod.localVariables = new LinkedList();
        fixedMethod.name = originalMethod.name;
        fixedMethod.desc = originalMethod.desc;
        fixedMethod.exceptions = originalMethod.exceptions;
        fixedMethod.tryCatchBlocks = new LinkedList();//originalMethod.tryCatchBlocks;

        originalMethod.accept(new TransactionalObjectRemappingMethodAdapter(fixedMethod, originalClass, originalMethod));

        return fixedMethod;
    }
}
