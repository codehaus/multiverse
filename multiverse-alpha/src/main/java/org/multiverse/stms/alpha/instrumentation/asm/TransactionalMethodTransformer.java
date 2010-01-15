package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.api.GlobalStmInstance;
import org.multiverse.api.Stm;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;
import static org.objectweb.asm.Type.*;

/**
 * Transforms transactionalmethod.
 * <p/>
 * The first generation of transactionalmethod transformers created an instance of the TransactionTemplate that forwards
 * the call but this transformer actually merges the code of the TransactionLogicDonor.
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

    private final ClassNode originalClass;
    private final MetadataRepository metadataRepository;
    private final ClassNode donorClass;
    private final MethodNode donorMethod;
    private final MethodNode donorConstructor;
    private final boolean isRealTransactionalObject;
    private final String tranlocalName;

    public TransactionalMethodTransformer(ClassNode classNode, ClassNode donorClass) {
        this.originalClass = classNode;
        this.metadataRepository = MetadataRepository.INSTANCE;
        this.donorClass = donorClass;
        this.tranlocalName = metadataRepository.getTranlocalName(classNode);
        this.donorMethod = getDonorMethod("donorMethod");
        this.donorConstructor = getDonorMethod("donorConstructor");
        this.isRealTransactionalObject = metadataRepository.isRealTransactionalObject(classNode.name);
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
        for (MethodNode method : (List<MethodNode>) originalClass.methods) {
            if (method.name.equals("<clinit>")) {
                return method;
            }
        }

        return null;
    }

    public ClassNode transform() {
        if (!metadataRepository.hasTransactionalMethods(originalClass)) {
            return null;
        }

        MethodNode staticInitializer = findStaticInitializer();
        if (staticInitializer == null) {
            staticInitializer = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, new String[]{});
            staticInitializer.instructions.add(new InsnNode(RETURN));
            originalClass.methods.add(staticInitializer);
        }

        for (MethodNode originalMethod : metadataRepository.getTransactionalMethods(originalClass)) {
            if (originalMethod.name.equals("<clinit>")) {
                //todo: improve exception
                throw new RuntimeException();
            }

            MethodNode donor = getDonor(originalMethod);

            originalClass.methods.remove(originalMethod);

            TransactionalMethodParams params = metadataRepository.getTransactionalMethodParams(originalClass,
                    originalMethod);

            //txFactoryField is used to create the transaction fot this originalMethod
            FieldNode txFactoryField = createTxFactoryField(originalMethod);
            originalClass.fields.add(txFactoryField);

            //add the txFactory initialization code to the front of the static initializer.
            InsnList insnList = codeForTxFactoryInitialization(params, txFactoryField);
            staticInitializer.instructions.insert(insnList);

            //create the coordinating method (the method that does the tx management)
            MethodNode coordinatingMethod = createCoordinatorMethod(originalMethod, donor, txFactoryField, params);
            originalClass.methods.add(coordinatingMethod);

            //creat the lift method (method that contains the 'original' logic).
            MethodNode liftMethod = createLiftMethod(originalMethod);
            originalClass.methods.add(liftMethod);

        }

        return originalClass;
    }

    private MethodNode getDonor(MethodNode originalTxMethod) {
        MethodNode donor;
        if (originalTxMethod.name.equals("<init>")) {
            donor = donorConstructor;
        } else {
            donor = donorMethod;
        }
        return donor;
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

    private InsnList codeForTxFactoryInitialization(TransactionalMethodParams params, FieldNode txFactoryField) {
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

        //preventWriteSkew
        insnList.add(new InsnNode(params.preventWriteSkew ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setPreventWriteSkew",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //smartTxLength.
        insnList.add(new InsnNode(params.smartTxLengthSelector ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setSmartTxLengthSelector",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //familyName
        insnList.add(new LdcInsnNode(params.maxRetryCount));
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
                originalClass.name,
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
    public MethodNode createLiftMethod(MethodNode originalMethod) {
        CloneMap cloneMap = new CloneMap();

        DebugInfo debugInfo = findDebugInfo(originalMethod);

        MethodNode result = new MethodNode();
        result.name = originalMethod.name;
        result.access = originalMethod.access;
        //todo: synthetic needs to be added

        //introduce the transaction arg
        result.desc = getLiftMethodDesc(originalClass.name, originalMethod);

        result.signature = originalMethod.signature;
        result.exceptions = originalMethod.exceptions;

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        result.localVariables = createListMethodLocalVariableTable(
                originalMethod, cloneMap, startLabelNode, endLabelNode);

        result.tryCatchBlocks = createLiftMethodTryCatchBlocks(originalMethod, cloneMap);

        result.instructions = createLiftMethodInstructions(
                originalMethod, cloneMap, debugInfo, startLabelNode, endLabelNode);

        return result;
    }

    private InsnList createLiftMethodInstructions(MethodNode originalMethod, CloneMap cloneMap,
                                                  DebugInfo debugInfo, LabelNode startLabelNode, LabelNode endLabelNode) {

        InsnList instructions = new InsnList();
        instructions.add(startLabelNode);
        instructions.add(new LineNumberNode(debugInfo.beginLine, startLabelNode));
        for (int k = 0; k < originalMethod.instructions.size(); k++) {
            AbstractInsnNode originalInsn = originalMethod.instructions.get(k);
            AbstractInsnNode clonedInsn = null;
            switch (originalInsn.getOpcode()) {
                case -1:
                    if (!(originalInsn instanceof FrameNode)) {
                        clonedInsn = originalInsn.clone(cloneMap);
                    }
                    break;
                case INVOKEINTERFACE:
                    //fall through
                case INVOKEVIRTUAL:
                    //fall through
                case INVOKESTATIC: {
                    MethodInsnNode methodInsn = (MethodInsnNode) originalInsn;
                    boolean transactionalMethod = metadataRepository.isTransactionalMethod(
                            methodInsn.owner, methodInsn.name, methodInsn.desc);

                    if (transactionalMethod) {
                        //if it is a transactional method, instead of calling the coordinating version,
                        //jump directly to the lifting version. This can be done by selecting the
                        //lifting method signature and add the transaction + optional tranlocal on the stack.

                        boolean isStaticMethod = originalInsn.getOpcode() == INVOKESTATIC;

                        //add the transaction
                        int transactionVar = getLiftMethodTransactionVar(originalMethod);
                        instructions.add(new VarInsnNode(ALOAD, transactionVar));

                        int tranlocalVarIndex = getLiftMethodTranlocalVar(
                                isStaticMethod, methodInsn.owner, methodInsn.name, methodInsn.desc);

                        //add the tranlocal if needed
                        if (tranlocalVarIndex >= 0) {
                            instructions.add(new InsnNode(ACONST_NULL));
                        }

                        //call the method.
                        clonedInsn = new MethodInsnNode(
                                methodInsn.getOpcode(),
                                methodInsn.owner,
                                methodInsn.name,
                                getLiftMethodDesc(isStaticMethod, methodInsn.owner, methodInsn.name, methodInsn.desc));
                    } else {
                        clonedInsn = originalInsn.clone(cloneMap);
                    }
                }
                break;
                case PUTFIELD: {
                    FieldInsnNode fieldInsn = (FieldInsnNode) originalInsn;

                    if (metadataRepository.isManagedInstanceField(fieldInsn.owner, fieldInsn.name)) {
                        AbstractInsnNode previous = (AbstractInsnNode) originalMethod.instructions.get(k - 1);

                        if (isCategory2(fieldInsn.desc)) {
                            //value(category2), owner(txobject),..

                            instructions.add(new InsnNode(DUP2_X1));
                            //[value(category2), owner(txobject), value(category2),...]

                            instructions.add(new InsnNode(POP2));
                            //[owner(txobject), value(category2), ...]
                        } else {
                            //[value(category1), owner(txobject),..
                            instructions.add(new InsnNode(SWAP));
                            //[owner(txobject), value(category1),..
                        }

                        if (isTranlocalAvailable(originalMethod, previous) && false) {
                            int tranlocalVar = getLiftMethodTranlocalVar(originalClass.name, originalMethod);

                            instructions.add(new VarInsnNode(ALOAD, tranlocalVar));

                            instructions.add(new InsnNode(DUP));

                            //instructions.add(new MethodInsnNode(
                            //        INVOKEVIRTUAL,
                            //));                                                        
                        } else {
                            instructions.add(new VarInsnNode(ALOAD, getLiftMethodTransactionVar(originalMethod)));

                            instructions.add(new InsnNode(SWAP));

                            instructions.add(new MethodInsnNode(
                                    INVOKEINTERFACE,
                                    getInternalName(AlphaTransaction.class),
                                    "openForWrite",
                                    format("(%s)%s", getDescriptor(AlphaTransactionalObject.class), getDescriptor(AlphaTranlocal.class))));

                            String tranlocalName = metadataRepository.getTranlocalName(fieldInsn.owner);

                            instructions.add(new TypeInsnNode(CHECKCAST, tranlocalName));

                            //if(isTranlocalAvailable())
                        }

                        if (isCategory2(fieldInsn.desc)) {
                            //[owner(tranlocal), value(category2),..

                            instructions.add(new InsnNode(DUP_X2));
                            //[owner(tranlocal), value(category2), owner(tranlocal)

                            instructions.add(new InsnNode(POP));
                            //[value(category2), owner(tranlocal),..
                        } else {
                            //[value(category1), owner(txobject),..
                            instructions.add(new InsnNode(SWAP));
                            //[owner(txobject), value(category1),..
                        }

                        String tranlocalName = metadataRepository.getTranlocalName(fieldInsn.owner);
                        clonedInsn = new FieldInsnNode(PUTFIELD, tranlocalName, fieldInsn.name, fieldInsn.desc);
                    } else {
                        clonedInsn = originalInsn.clone(cloneMap);
                    }
                }
                break;
                case GETFIELD: {
                    FieldInsnNode fieldInsn = (FieldInsnNode) originalInsn;

                    if (metadataRepository.isManagedInstanceField(fieldInsn.owner, fieldInsn.name)) {
                        AbstractInsnNode previous = (AbstractInsnNode) originalMethod.instructions.get(k - 1);

                        if (isTranlocalAvailable(originalMethod, previous) && false) {
                            //the original target can be popped from the stack.
                            instructions.add(new InsnNode(POP));

                            //load the tranlocal
                            int tranlocalIndex = getLiftMethodTranlocalVar(originalClass.name, originalMethod);
                            instructions.add(new VarInsnNode(ALOAD, tranlocalIndex));
                        } else {
                            instructions.add(new VarInsnNode(ALOAD, getLiftMethodTransactionVar(originalMethod)));

                            instructions.add(new InsnNode(SWAP));

                            instructions.add(new MethodInsnNode(
                                    INVOKEINTERFACE,
                                    getInternalName(AlphaTransaction.class),
                                    "openForRead",
                                    format("(%s)%s", getDescriptor(AlphaTransactionalObject.class), getDescriptor(AlphaTranlocal.class))));

                            String tranlocalName = metadataRepository.getTranlocalName(fieldInsn.owner);
                            instructions.add(new TypeInsnNode(CHECKCAST, tranlocalName));
                        }

                        String tranlocalName = metadataRepository.getTranlocalName(fieldInsn.owner);
                        instructions.add(new FieldInsnNode(GETFIELD, tranlocalName, fieldInsn.name, fieldInsn.desc));
                    } else {
                        clonedInsn = originalInsn.clone(cloneMap);
                    }
                }
                break;
                case IINC: {
                    IincInsnNode originalIncInsn = (IincInsnNode) originalInsn;
                    int newPos = getLiftMethodLocalVarIndex(originalClass.name, originalMethod, originalIncInsn.var);
                    clonedInsn = new IincInsnNode(newPos, originalIncInsn.incr);
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
                    VarInsnNode originalVarNode = (VarInsnNode) originalInsn;
                    int newPos = getLiftMethodLocalVarIndex(originalClass.name, originalMethod, originalVarNode.var);
                    clonedInsn = new VarInsnNode(originalInsn.getOpcode(), newPos);
                }
                break;
                default:
                    clonedInsn = originalInsn.clone(cloneMap);
                    break;
            }

            if (clonedInsn != null) {
                instructions.add(clonedInsn);
            }
        }

        //if it a init of a transactional object, we need to make sure that the openForWrite is called
        if (originalMethod.name.equals("<init>") && isRealTransactionalObject) {
            int indexOfFirst = firstIndexAfterSuper(originalMethod.name, instructions, originalClass.superName);

            if (indexOfFirst >= 0) {
                InsnList initTranlocal = new InsnList();

                initTranlocal.add(new VarInsnNode(ALOAD, getLiftMethodTransactionVar(originalMethod)));

                initTranlocal.add(new VarInsnNode(ALOAD, 0));

                String openForWriteDesc = format("(%s)%s",
                        getDescriptor(AlphaTransactionalObject.class),
                        getDescriptor(AlphaTranlocal.class));

                initTranlocal.add(new MethodInsnNode(
                        INVOKEINTERFACE,
                        getInternalName(AlphaTransaction.class),
                        "openForWrite",
                        openForWriteDesc));

                //int tranlocalIndex =

                //todo: instead of popping it, place it in the tranlocal local variable.
                initTranlocal.add(new InsnNode(POP));

                instructions.insertBefore(instructions.get(indexOfFirst), initTranlocal);
            }
        }

        instructions.add(endLabelNode);
        return instructions;
    }

    private boolean isTranlocalAvailable(MethodNode originalMethod, AbstractInsnNode previous) {
        if (getLiftMethodTranlocalVar(originalClass.name, originalMethod) >= 0) {
            if (previous instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode) previous;
                if (varInsn.getOpcode() == ALOAD && varInsn.var == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private List createLiftMethodTryCatchBlocks(MethodNode originalMethod, CloneMap cloneMap) {
        //clone the try catch blocks
        List tryCatchBlocks = new LinkedList();
        for (int k = 0; k < originalMethod.tryCatchBlocks.size(); k++) {
            TryCatchBlockNode original = (TryCatchBlockNode) originalMethod.tryCatchBlocks.get(k);
            TryCatchBlockNode cloned = new TryCatchBlockNode(
                    cloneMap.get(original.start),
                    cloneMap.get(original.end),
                    cloneMap.get(original.handler),
                    original.type);
            tryCatchBlocks.add(cloned);
        }

        return tryCatchBlocks;
    }

    private List createListMethodLocalVariableTable(MethodNode originalMethod, CloneMap cloneMap,
                                                    LabelNode startLabelNode,
                                                    LabelNode endLabelNode) {
        List<LocalVariableNode> result = new LinkedList<LocalVariableNode>();

        //introduce the transaction.
        LocalVariableNode transactionVar = new LocalVariableNode(
                "transaction",
                Type.getDescriptor(AlphaTransaction.class),
                null,
                startLabelNode,
                endLabelNode,
                getLiftMethodTransactionVar(originalMethod));
        result.add(transactionVar);

        //introduce the tranlocal if needed
        int tranlocalVarIndex = getLiftMethodTranlocalVar(originalClass.name, originalMethod);
        if (tranlocalVarIndex >= 0) {
            LocalVariableNode tranlocalVar = new LocalVariableNode(
                    "tranlocalThis",
                    internalToDesc(tranlocalName),
                    null,
                    startLabelNode,
                    endLabelNode,
                    tranlocalVarIndex);
            result.add(tranlocalVar);
        }

        //copy all the rest of the local variables.
        for (LocalVariableNode originalLocalVar : (List<LocalVariableNode>) originalMethod.localVariables) {
            int clonedVarIndex = getLiftMethodLocalVarIndex(originalClass.name, originalMethod, originalLocalVar.index);
            LocalVariableNode clonedLocalVar = new LocalVariableNode(
                    originalLocalVar.name,
                    originalLocalVar.desc,
                    originalLocalVar.signature,
                    cloneMap.get(originalLocalVar.start),
                    cloneMap.get(originalLocalVar.end),
                    clonedVarIndex);
            result.add(clonedLocalVar);
        }

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

                        if (!isStatic(originalMethod)) {
                            //push the this in the stack
                            result.instructions.add(new VarInsnNode(ALOAD, 0));
                        }

                        int loadIndex = isStatic(originalMethod) ? 0 : 1;
                        //place the rest of the arguments on the stack
                        for (Type argType : getArgumentTypes(originalMethod.desc)) {
                            VarInsnNode loadInsn = new VarInsnNode(argType.getOpcode(ILOAD), loadIndex);
                            result.instructions.add(loadInsn);
                            loadIndex += argType.getSize();
                        }

                        //push the transaction on the stack
                        result.instructions.add(new VarInsnNode(ALOAD, transactionVar.index));

                        int tranlocalIndex = getLiftMethodTranlocalVar(originalClass.name, originalMethod);

                        //push the tranlocal on the stack if needed
                        if (tranlocalIndex >= 0) {
                            result.instructions.add(new VarInsnNode(ALOAD, transactionVar.index));

                            result.instructions.add(new VarInsnNode(ALOAD, 0));

                            String openForReadDesc = format("(%s)%s",
                                    getDescriptor(AlphaTransactionalObject.class),
                                    getDescriptor(AlphaTranlocal.class));

                            result.instructions.add(new MethodInsnNode(
                                    INVOKEINTERFACE,
                                    getInternalName(AlphaTransaction.class),
                                    "openForRead",
                                    openForReadDesc));
                            //
                            result.instructions.add(new TypeInsnNode(
                                    CHECKCAST,
                                    tranlocalName));
                        }

                        MethodInsnNode invokeInsn = new MethodInsnNode(
                                getInvokeOpcode(originalMethod),
                                originalClass.name,
                                originalMethod.name,
                                getLiftMethodDesc(originalClass.name, originalMethod));
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
                                GETSTATIC, originalClass.name, txFactoryField.name, donorFieldInsnNode.desc));
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
                + "InterruptedException or Exception ", originalClass.name.replace("/", "."), originalMethod.name);
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

    public static int getLiftMethodTransactionVar(boolean isStaticMethod, String methodDesc) {
        if (isStaticMethod) {
            return sizeOfFormalParameters(methodDesc);
        }

        //transaction will always be added to the end.
        return 1 + sizeOfFormalParameters(methodDesc);
    }


    public static int getLiftMethodTransactionVar(MethodNode originalMethod) {
        return getLiftMethodTransactionVar(isStatic(originalMethod), originalMethod.desc);
    }

    public static int getLiftMethodTranlocalVar(boolean isStaticMethod, String owner, String methodName, String methodDesc) {
        if (isStaticMethod) {
            return -1;
        }

        boolean isRealTransactionalObject = MetadataRepository.INSTANCE.isRealTransactionalObject(owner);

        if (isRealTransactionalObject && !methodName.equals("<init>")) {
            return getLiftMethodTransactionVar(isStaticMethod, methodDesc) + 1;
        } else {
            return -1;
        }
    }

    public static int getLiftMethodTranlocalVar(String owner, MethodNode originalMethod) {
        return getLiftMethodTranlocalVar(isStatic(originalMethod), owner, originalMethod.name, originalMethod.desc);
    }

    public static int getLiftMethodLocalVarIndex(String owner, MethodNode originalMethod, int var) {
        int firstLocalVar = sizeOfFormalParameters(originalMethod);
        if (!isStatic(originalMethod)) {
            firstLocalVar++;
        }

        if (var < firstLocalVar) {
            return var;
        } else if (getLiftMethodTranlocalVar(owner, originalMethod) >= 0) {
            //the tranlocal and the transaction are introduced
            return var + 2;
        } else {
            //only the tranlocal is introduced
            return var + 1;
        }
    }

    private static String getLiftMethodDesc(String owner, MethodNode originalMethod) {
        return getLiftMethodDesc(isStatic(originalMethod), owner, originalMethod.name, originalMethod.desc);
    }

    private static String getLiftMethodDesc(boolean isStaticMethod, String owner, String methodName, String methodDesc) {
        Type returnType = Type.getReturnType(methodDesc);
        Type[] argTypes = Type.getArgumentTypes(methodDesc);
        Type[] newArgTypes;
        if (getLiftMethodTranlocalVar(isStaticMethod, owner, methodName, methodDesc) >= 0) {
            newArgTypes = new Type[argTypes.length + 2];
            String tranlocalName = MetadataRepository.INSTANCE.getTranlocalName(owner);
            newArgTypes[newArgTypes.length - 1] = getType(internalToDesc(tranlocalName));
        } else {
            newArgTypes = new Type[argTypes.length + 1];
        }
        newArgTypes[argTypes.length] = Type.getType(AlphaTransaction.class);
        System.arraycopy(argTypes, 0, newArgTypes, 0, argTypes.length);
        return getMethodDescriptor(returnType, newArgTypes);
    }
}
