package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.api.Transaction;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Type.getArgumentTypes;
import static org.objectweb.asm.Type.getInternalName;
import org.objectweb.asm.tree.*;

import static java.lang.String.format;
import java.util.*;

/**
 * Transforms AtomicMethods. The first generation of atomicmethod transformers created an instance of the AtomicTemplate
 * that forwards the call. This transformer 'only' transforms the methods so that the atomictemplate donorMethod is
 * added. This prevents creating another object.
 * <p/>
 * The question is how to transform. The code needs to be placed around the original code. In principle this is no
 * problem (can be done with a try finally clause?). But the question is if this can cause problems with constructors.
 * <p/>
 * If is is placed around the constructor, the constructor will be re-executed on the same atomicobject.
 * <p/>
 * Another reason to drop the template approach is that a lot of boxing/unboxing goes on with primitive return types of
 * the atomicmethod.
 *
 * @author Peter Veentjer.
 */
public class AtomicMethodTransformer implements Opcodes {

    private final ClassNode classNode;
    private final MetadataRepository metadataService;
    private final ClassNode donorClass;
    private final MethodNode donorMethod;
    private final MethodNode donorConstructor;

    public AtomicMethodTransformer(ClassNode classNode, ClassNode donorClass) {
        this.classNode = classNode;
        this.metadataService = MetadataRepository.INSTANCE;
        this.donorClass = donorClass;
        this.donorMethod = getDonorMethod("donorMethod");
        this.donorConstructor = getDonorMethod("donorConstructor");
    }

    private MethodNode getDonorMethod(String methodName) {
        for (MethodNode m : (List<MethodNode>) donorClass.methods) {
            if (m.name.equals(methodName)) {
                return m;
            }
        }

        throw new RuntimeException(format("method '%s' not found in class '%s'", methodName, donorClass.name));
    }

    public ClassNode transform() {
        if (!metadataService.hasAtomicMethods(classNode)) {
            return null;
        }

        for (MethodNode originalAtomicMethod : metadataService.getAtomicMethods(classNode)) {
            if (originalAtomicMethod.name.equals("<clinit>")) {
                throw new RuntimeException();
            }

            MethodNode donor;
            if (isConstructor(originalAtomicMethod)) {
                donor = donorConstructor;
            } else {
                donor = donorMethod;
            }

            classNode.methods.remove(originalAtomicMethod);

            MethodNode coordinating = createCoordinatorMethod(originalAtomicMethod, donor);
            classNode.methods.add(coordinating);

            MethodNode lifting = createLiftingMethod(originalAtomicMethod);
            classNode.methods.add(lifting);

            //    AsmUtils.print(coordinating, "coordinating");
            //     AsmUtils.print(lifting, "lifting");


            //AsmUtils.print(coordinating, "coordinating");
            //String result = Debugger.debug(classNode.name, coordinating);
            //System.out.println("debugging result:\n" + result);

        }

        return classNode;
    }

    private static boolean isConstructor(MethodNode methodNode) {
        return methodNode.name.equals("<init>");
    }

    /**
     * Creates a method that lifts on an already existing transaction and is going to contains the actual logic. The
     * Transaction will be passed as extra argument to the method, so the method will get a different signature. The
     * transaction will be passed as the first argument, shifting all others one to the left.
     * <p/>
     * A new methodNode will be returned, originalAtomicMethod remains untouched.
     * <p/>
     * Since a matching lifting method will always be available for each atomicmethod, this method can be called instead
     * of one knows that the method needs to run on the same transaction.
     *
     * @param originalMethod the original MethodNode that is enhanced.
     * @return the transformed MethodNode.
     */
    public MethodNode createLiftingMethod(MethodNode originalMethod) {
        CloneMap cloneMap = new CloneMap();

        MethodNode result = new MethodNode();
        result.name = originalMethod.name;
        result.access = originalMethod.access;
        result.desc = createShiftedMethodDescriptor(
                originalMethod.desc,
                getInternalName(Transaction.class));

        result.signature = originalMethod.signature;
        result.exceptions = originalMethod.exceptions;

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        //clone the local variables and introduce the transaction variable.
        result.localVariables = new LinkedList();
        //introduce the transaction variable

        LocalVariableNode transactionVariable = new LocalVariableNode(
                "transaction",
                Type.getDescriptor(Transaction.class),
                null,
                startLabelNode,
                endLabelNode,
                isStatic(originalMethod) ? 0 : 1);

        result.localVariables.add(transactionVariable);

        for (LocalVariableNode original : (List<LocalVariableNode>) originalMethod.localVariables) {
            int cloneIndex = indexForShiftedVariable(originalMethod, original.index);

            LocalVariableNode cloned = new LocalVariableNode(
                    original.name,
                    original.desc,
                    original.signature,
                    cloneMap.get(original.start),
                    cloneMap.get(original.end),
                    cloneIndex);
            result.localVariables.add(cloned);
        }

        //clone the try catch blocks
        result.tryCatchBlocks = new LinkedList();
        for (int k = 0; k < originalMethod.tryCatchBlocks.size(); k++) {
            TryCatchBlockNode original = (TryCatchBlockNode) originalMethod.tryCatchBlocks.get(k);
            TryCatchBlockNode cloned = new TryCatchBlockNode(
                    cloneMap.get(original.start),
                    cloneMap.get(original.end),
                    cloneMap.get(original.handler),
                    original.type);
            result.tryCatchBlocks.add(cloned);
        }

        //clone the instructions.
        result.instructions = new InsnList();
        result.instructions.add(startLabelNode);
        for (int k = 0; k < originalMethod.instructions.size(); k++) {
            AbstractInsnNode originalInsn = originalMethod.instructions.get(k);
            AbstractInsnNode clonedInsn = null;
            switch (originalInsn.getOpcode()) {
                case -1:
                    if (!(originalInsn instanceof FrameNode)) {
                        clonedInsn = originalInsn.clone(cloneMap);
                    }
                    break;
                case IINC: {
                    IincInsnNode originalIncNode = (IincInsnNode) originalInsn;
                    int clonedIndex = indexForShiftedVariable(originalMethod, originalIncNode.var);
                    clonedInsn = new IincInsnNode(clonedIndex, originalIncNode.incr);
                }
                break;
                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ALOAD:
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE: {
                    //the variable needs to be shifted one to the left.
                    VarInsnNode originalVarNode = (VarInsnNode) originalInsn;
                    int clonedIndex = indexForShiftedVariable(originalMethod, originalVarNode.var);
                    clonedInsn = new VarInsnNode(originalInsn.getOpcode(), clonedIndex);
                }
                break;
                default:
                    clonedInsn = originalInsn.clone(cloneMap);
                    break;
            }
            if (clonedInsn != null) {
                result.instructions.add(clonedInsn);
            }
        }

        result.instructions.add(endLabelNode);
        return result;
    }

    private static int indexForShiftedVariable(MethodNode originalMethod, int oldIndex) {
        if (isStatic(originalMethod)) {
            return oldIndex + 1;
        } else {
            if (oldIndex == 0) {
                return 0;
            } else {
                return oldIndex + 1;
            }
        }
    }

    private static LocalVariableNode findThisVariable(MethodNode methodNode) {
        if (methodNode.localVariables == null) {
            return null;
        }

        for (LocalVariableNode localVar : (List<LocalVariableNode>) methodNode.localVariables) {
            if (localVar.name.equals("this")) {
                return localVar;
            }
        }
        return null;
    }

    /**
     * Creates  the coordinating method; a method that is responsible for starting/committing aborting and retrying. The
     * real logic is forwarded to the lifting method.
     * <p/>
     * The lifting method has received an extra (first) argument; the transaction that is managed by
     *
     * @param originalMethod the original MethodNode.
     * @return the coordinating method.
     */
    public MethodNode createCoordinatorMethod(MethodNode originalMethod, MethodNode donorMethod) {
        MethodNode result = new MethodNode(
                originalMethod.access,
                originalMethod.name,
                originalMethod.desc,
                originalMethod.signature,
                getExceptions(originalMethod));
        //todo: clone
        result.visibleAnnotations = originalMethod.visibleAnnotations;
        //todo: clone
        result.invisibleAnnotations = originalMethod.invisibleAnnotations;
        //todo: clone
        result.invisibleParameterAnnotations = originalMethod.invisibleParameterAnnotations;
        //todo: clone
        result.annotationDefault = originalMethod.annotationDefault;

        CloneMap cloneMap = new CloneMap();

        //======================================================
        //      placement of the local variables
        //======================================================

        LocalVariableNode transactionVar = null;
        Map<Integer, Integer> varMapping = new HashMap<Integer, Integer>();


        //at the moment the start end endscope are harvested from the original instructions.
        //LabelNode startScope = cloneMap.get(originalMethod.instructions.getFirst());
        //LabelNode endScope = cloneMap.get(originalMethod.instructions.getLast());
        LabelNode startScope = new LabelNode();
        LabelNode endScope = new LabelNode();

        int var = 0;

        //create local variable for the 'this' if needed.
        if (!isStatic(originalMethod)) {
            LocalVariableNode originalThis = findThisVariable(originalMethod);

            LocalVariableNode clonedThis = new LocalVariableNode(
                    originalThis.name,
                    originalThis.desc,
                    originalThis.signature,
                    startScope,
                    endScope,
                    originalThis.index);
            result.localVariables.add(clonedThis);
            var++;
        }

        //create local variables for all the method arguments.
        for (Type argType : getArgumentTypes(originalMethod.desc)) {
            LocalVariableNode clonedVar = new LocalVariableNode(
                    "arg" + result.localVariables.size(),
                    argType.getDescriptor(),
                    null,
                    startScope,
                    endScope,
                    var);
            var += argType.getSize();
            result.localVariables.add(clonedVar);
        }

        //create local variables based on the local variables of the donor method.
        for (LocalVariableNode donorVar : (List<LocalVariableNode>) donorMethod.localVariables) {
            LocalVariableNode clonedVar = new LocalVariableNode(
                    donorVar.name,
                    donorVar.desc,
                    donorVar.signature,
                    cloneMap.get(donorVar.start),
                    cloneMap.get(donorVar.end),
                    var);
            varMapping.put(donorVar.index, clonedVar.index);

            if (donorVar.name.equals("t")) {
                transactionVar = clonedVar;
            }

            result.localVariables.add(clonedVar);
            var += Type.getType(clonedVar.desc).getSize();
        }

        //create the variable containing the result
        LocalVariableNode resultVariable = null;
        Type returnType = Type.getReturnType(originalMethod.desc);
        if (!returnType.equals(Type.VOID_TYPE)) {
            resultVariable = new LocalVariableNode(
                    "result",
                    returnType.getDescriptor(),
                    null,
                    startScope,
                    endScope,
                    var);
            result.localVariables.add(resultVariable);
            var += returnType.getSize();
        }

        if (transactionVar == null) {
            throw new RuntimeException(format("No transaction variable with name 't' is found in donor method '%s.%s'",
                                              donorClass.name, donorMethod.name));
        }

        //======================================================
        //      placement of the try catch blocks
        //======================================================

        result.tryCatchBlocks = new LinkedList();
        result.tryCatchBlocks.addAll(clone(donorMethod.tryCatchBlocks, cloneMap));

        //======================================================
        //      placement of the instructions
        //======================================================
        result.instructions.add(startScope);

        AtomicMethodParams params = metadataService.getAtomicMethodParams(classNode, originalMethod);
        for (ListIterator<AbstractInsnNode> it = (ListIterator<AbstractInsnNode>) donorMethod.instructions
                .iterator(); it.hasNext();) {
            AbstractInsnNode donorInsn = it.next();
            switch (donorInsn.getOpcode()) {
                case -1:
                    //all the linenumber & frame nodes can be discarded.
                    if (!(donorInsn instanceof LineNumberNode) && !(donorInsn instanceof FrameNode)) {
                        AbstractInsnNode cloned = donorInsn.clone(cloneMap);
                        result.instructions.add(cloned);
                    }
                    break;
                case INVOKESTATIC:
                    MethodInsnNode donorMethodInsn = (MethodInsnNode) donorInsn;
                    if (isReplacementMethod(donorMethodInsn)) {
                        int loadIndex = 0;
                        if (isStatic(originalMethod)) {
                            //push the transaction on the stack
                            result.instructions.add(new VarInsnNode(ALOAD, transactionVar.index));
                        } else {
                            //push the this in the stack
                            result.instructions.add(new VarInsnNode(ALOAD, loadIndex));
                            loadIndex = 1;
                            //push the transaction var on the stack
                            result.instructions.add(new VarInsnNode(ALOAD, transactionVar.index));
                        }

                        //place the rest of the arguments on the stack
                        for (Type argType : getArgumentTypes(originalMethod.desc)) {
                            VarInsnNode loadInsn = new VarInsnNode(argType.getOpcode(ILOAD), loadIndex);
                            result.instructions.add(loadInsn);
                            loadIndex += argType.getSize();
                        }

                        //do the invoke
                        String desc = createShiftedMethodDescriptor(
                                originalMethod.desc,
                                getInternalName(Transaction.class));

                        MethodInsnNode invokeInsn = new MethodInsnNode(
                                getInvokeOpcode(originalMethod),
                                classNode.name,
                                originalMethod.name,
                                desc);
                        result.instructions.add(invokeInsn);

                        if (!returnType.equals(Type.VOID_TYPE)) {
                            result.instructions.add(
                                    new VarInsnNode(returnType.getOpcode(ISTORE), resultVariable.index));
                        }
                    } else {
                        AbstractInsnNode cloned = donorInsn.clone(cloneMap);
                        result.instructions.add(cloned);
                    }
                    break;
                case GETSTATIC: {
                    FieldInsnNode donorFieldInsnNode = (FieldInsnNode) donorInsn;
                    boolean donorIsOwner = donorFieldInsnNode.owner.equals(donorClass.name);

                    if (donorIsOwner && donorFieldInsnNode.name.equals("readOnly")) {
                        if (params.readOnly) {
                            result.instructions.add(new InsnNode(ICONST_1));
                        } else {
                            result.instructions.add(new InsnNode(ICONST_0));
                        }
                    } else if (donorIsOwner && donorFieldInsnNode.name.equals("retryCount")) {
                        Integer retryCount = params.retryCount;
                        result.instructions.add(new LdcInsnNode(retryCount));
                    } else if (donorIsOwner && donorFieldInsnNode.name.equals("familyName")) {
                        result.instructions.add(new LdcInsnNode(params.familyName));
                    } else {
                        result.instructions.add(donorInsn.clone(cloneMap));
                    }
                }
                break;
                case IINC: {
                    IincInsnNode donorIncInsn = (IincInsnNode) donorInsn;
                    int clonedIndex = varMapping.get(donorIncInsn.var);
                    result.instructions.add(new IincInsnNode(clonedIndex, donorIncInsn.incr));
                }
                break;
                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ALOAD:
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE: {
                    VarInsnNode donorVarInsn = (VarInsnNode) donorInsn;
                    Integer foundVar = varMapping.get(donorVarInsn.var);
                    int index;
                    if (foundVar == null) {
                        //it could be that a variable is found that is not in the localvariable table.
                        //these variables are the secret throwable storage location needed to compile
                        //finally clauses. So for these variables, we just create a new 
                        index = var;
                        varMapping.put(donorVarInsn.var, var);
                        var++;
                    } else {
                        index = foundVar;
                    }
                    VarInsnNode cloned = new VarInsnNode(donorVarInsn.getOpcode(), index);
                    result.instructions.add(cloned);
                }
                break;
                case RETURN:
                    if (returnType.equals(Type.VOID_TYPE)) {
                        result.instructions.add(new InsnNode(RETURN));
                    } else {
                        //the returns need to be replaced by returns of the correct returntype of the
                        //originalMethod.
                        result.instructions.add(new VarInsnNode(returnType.getOpcode(ILOAD), resultVariable.index));
                        result.instructions.add(new InsnNode(returnType.getOpcode(IRETURN)));
                    }
                    break;
                default:
                    AbstractInsnNode cloned = donorInsn.clone(cloneMap);
                    result.instructions.add(cloned);
                    break;
            }
        }

        result.instructions.add(endScope);
        return result;
    }

    public static int getInvokeOpcode(MethodNode methodNode) {
        if (isStatic(methodNode.access)) {
            return INVOKESTATIC;
        } else if (isConstructor(methodNode)) {
            return INVOKESPECIAL;
        } else {
            return INVOKEVIRTUAL;
        }
    }

    public static List<TryCatchBlockNode> clone(List<TryCatchBlockNode> originalBlocks, CloneMap cloneMap) {
        List<TryCatchBlockNode> result = new LinkedList<TryCatchBlockNode>();
        for (TryCatchBlockNode originalBlock : originalBlocks) {
            TryCatchBlockNode clonedBlock = new TryCatchBlockNode(
                    cloneMap.get(originalBlock.start),
                    cloneMap.get(originalBlock.end),
                    cloneMap.get(originalBlock.handler),
                    originalBlock.type
            );
            result.add(clonedBlock);
        }
        return result;
    }

    private static boolean isReplacementMethod(MethodInsnNode donorMethodInsnNode) {
        if (!donorMethodInsnNode.name.equals("execute")) {
            return false;
        }

        if (!donorMethodInsnNode.owner.equals(getInternalName(AtomicLogicDonor.class))) {
            return false;
        }

        return true;
    }
}
