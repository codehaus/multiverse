package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.api.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;
import static org.objectweb.asm.Type.getArgumentTypes;
import static org.objectweb.asm.Type.getInternalName;

/**
 * Transforms transactionalmethod.
 * <p/>
 * The first generation of transactionalmethod transformers created an instance of the
 * TransactionTemplate that forwards the call but this transformer actually merges the code of the
 * TransactionLogicDonor.
 * <p/>
 * <p/>
 * The question is how to transform. The code needs to be placed around the original code. In principle this is no
 * problem (can be done with a try finally clause?). But the question is if this can cause problems with constructors.
 * <p/>
 * If is is placed around the constructor, the constructor will be re-executed on the same atomicobject.
 * <p/>
 * Another reason to drop the template approach is that a lot of boxing/unboxing goes on with primitive return types of
 * the transactionalobject.
 *
 * @author Peter Veentjer.
 */
public class TransactionalMethodTransformer implements Opcodes {

    private final ClassNode classNode;
    private final MetadataRepository metadataService;
    private final ClassNode donorClass;
    private final MethodNode donorMethod;
    private final MethodNode donorConstructor;

    public TransactionalMethodTransformer(ClassNode classNode, ClassNode donorClass) {
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

    private MethodNode findStaticInitializer() {
        for (MethodNode method : (List<MethodNode>) classNode.methods) {
            if (method.name.equals("<clinit>")) {
                return method;
            }
        }

        return null;
    }

    public ClassNode transform() {
        if (!metadataService.hasTransactionalMethods(classNode)) {
            return null;
        }

        MethodNode staticInitializer = findStaticInitializer();
        if (staticInitializer == null) {
            staticInitializer = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, new String[]{});
            staticInitializer.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(staticInitializer);
        }

        for (MethodNode originalTxMethod : metadataService.getTransactionalMethods(classNode)) {
            if (originalTxMethod.name.equals("<clinit>")) {
                //todo: improve exception
                throw new RuntimeException();
            }

            MethodNode donor;
            if (originalTxMethod.name.equals("<init>")) {
                donor = donorConstructor;
            } else {
                donor = donorMethod;
            }

            classNode.methods.remove(originalTxMethod);

            TransactionalMethodParams params = metadataService.getTransactionalMethodParams(classNode, originalTxMethod);

            FieldNode txFactoryField = createTxFactoryField(originalTxMethod);
            classNode.fields.add(txFactoryField);

            InsnList insnList = codeForTransactionFactory(params, txFactoryField);
            staticInitializer.instructions.insert(insnList);

            MethodNode coordinating = createCoordinatorMethod(originalTxMethod, donor, txFactoryField, params);
            classNode.methods.add(coordinating);

            MethodNode lifting = createLiftingMethod(originalTxMethod);
            classNode.methods.add(lifting);

            //    AsmUtils.print(coordinating, "coordinating");
            //     AsmUtils.print(lifting, "lifting");


            //AsmUtils.print(coordinating, "coordinating");
            //String result = Debugger.debug(classNode.name, coordinating);
            //System.out.println("debugging result:\n" + result);

        }

        return classNode;
    }

    /**
     * Creates the static field that contains the txFactory for a transactional method.
     *
     * @param originalMethod the MethodNode
     * @return the created FieldNode.
     */
    private FieldNode createTxFactoryField(MethodNode originalMethod) {
        int access = ACC_FINAL + ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC;
        String name = "___transactionFactory_" + System.nanoTime(); //todo: improve
        String desc = Type.getDescriptor(TransactionFactory.class);
        String sig = null;
        Object value = null;
        return new FieldNode(access, name, desc, sig, value);
    }

    private InsnList codeForTransactionFactory(TransactionalMethodParams params, FieldNode txFactoryField) {
        InsnList insnList = new InsnList();

        //lets get the stm instance from the GlobalStmInstance
        insnList.add(new MethodInsnNode(
                INVOKESTATIC,
                Type.getInternalName(GlobalStmInstance.class),
                "getGlobalStmInstance",
                "()" + Type.getDescriptor(Stm.class)));

        //lets get the transactionFactoryBuilder from the stm.
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(Stm.class),
                "getTransactionFactoryBuilder",
                "()" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //familyName
        insnList.add(new LdcInsnNode(params.familyName));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setFamilyName",
                "(Ljava/lang/String;)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //readonly
        insnList.add(new InsnNode(params.readOnly ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setReadonly",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //trackreads
        insnList.add(new InsnNode(params.automaticReadTracking ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setAutomaticReadTracking",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //interruptible.
        insnList.add(new InsnNode(params.interruptible ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setInterruptible",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //detectWriteSkew
        insnList.add(new InsnNode(params.detectWriteSkew ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setDetectWriteSkew",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //smartTxLength.
        insnList.add(new InsnNode(params.smartTxLengthSelector ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setSmartTxLengthSelector",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //familyName
        insnList.add(new LdcInsnNode(params.retryCount));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setMaxRetryCount",
                "(I)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //now lets build the TransactionFactory
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "build",
                "()" + Type.getDescriptor(TransactionFactory.class)));

        //and store it in the txFactoryField
        insnList.add(new FieldInsnNode(
                PUTSTATIC,
                classNode.name,
                txFactoryField.name,
                Type.getDescriptor(TransactionFactory.class)));

        return insnList;
    }


    /**
     * Creates a method that lifts on an already existing transaction and is going to contains the actual logic. The
     * Transaction will be passed as extra argument to the method, so the method will get a different signature. The
     * transaction will be passed as the first argument, shifting all others one to the left.
     * <p/>
     * A new methodNode will be returned, originalMethod remains untouched.
     * <p/>
     * Since a matching lifting method will always be available for each atomicmethod, this method can be called instead
     * of one knows that the method needs to run on the same transaction.
     *
     * @param originalMethod the original MethodNode that is enhanced.
     * @return the transformed MethodNode.
     */
    public MethodNode createLiftingMethod(MethodNode originalMethod) {
        CloneMap cloneMap = new CloneMap();

        DebugInfo debugInfo = findDebugInfo(originalMethod);

        MethodNode result = new MethodNode();
        result.name = originalMethod.name;
        result.access = originalMethod.access;
        //todo: synthetic needs to be added
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
        //System.out.println("beginline: "+debugInfo.beginLine);
        result.instructions.add(new LineNumberNode(debugInfo.beginLine, startLabelNode));
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

    public DebugInfo findDebugInfo(MethodNode method) {
        DebugInfo info = new DebugInfo();

        for (ListIterator<AbstractInsnNode> iterator = method.instructions.iterator(); iterator.hasNext();) {
            AbstractInsnNode node = iterator.next();
            if (node instanceof LineNumberNode) {
                LineNumberNode lineNumberNode = (LineNumberNode) node;
                if (lineNumberNode.line > info.endLine) {
                    info.endLine = lineNumberNode.line;
                }

                if (info.beginLine == -1) {
                    info.beginLine = lineNumberNode.line;
                } else if (lineNumberNode.line < info.beginLine) {
                    info.beginLine = lineNumberNode.line;
                }
            }
        }

        return info;
    }

    static class DebugInfo {

        int beginLine = -1;
        int endLine = -1;
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
    public MethodNode createCoordinatorMethod(MethodNode originalMethod, MethodNode donorMethod,
                                              FieldNode txFactoryField, TransactionalMethodParams params) {

        if (params.interruptible) {
            ensureInterruptibleExceptionCanBeThrown(originalMethod);
        }

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

            if (donorVar.name.equals("tx")) {
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
            throw new RuntimeException(format("No transaction variable with name 'tx' is found in donor method '%s.%s'",
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

                    if (donorIsOwner && donorFieldInsnNode.name.equals("transactionFactory")) {
                        result.instructions.add(new FieldInsnNode(
                                GETSTATIC, classNode.name, txFactoryField.name, donorFieldInsnNode.desc));
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

    private void ensureInterruptibleExceptionCanBeThrown(MethodNode originalMethod) {
        for (String exception : (List<String>) originalMethod.exceptions) {
            if (exception.equals(Type.getInternalName(InterruptedException.class)) ||
                    exception.equals(Type.getInternalName(Throwable.class)) ||
                    exception.equals(Type.getInternalName(Exception.class))) {
                return;
            }
        }

        String msg = format("Transaction on Method '%s.%s' can't be made interruptible since it doesn't throw "
                + "InterruptedException or Exception ", classNode.name.replace("/", "."), originalMethod.name);
        throw new RuntimeException(msg);
    }

    public static int getInvokeOpcode(MethodNode methodNode) {
        if (isStatic(methodNode.access)) {
            return INVOKESTATIC;
        } else if (methodNode.name.equals("<init>")) {
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

        if (!donorMethodInsnNode.owner.equals(getInternalName(TransactionLogicDonor.class))) {
            return false;
        }

        return true;
    }
}
