package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.multiverse.api.GlobalStmInstance;
import org.multiverse.api.Stm;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.instrumentation.CompileException;
import org.multiverse.instrumentation.DebugInfo;
import org.multiverse.instrumentation.InstrumenterLogger;
import org.multiverse.instrumentation.asm.AsmUtils;
import org.multiverse.instrumentation.asm.CloneMap;
import org.multiverse.instrumentation.metadata.*;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;

import static java.lang.String.format;
import static org.multiverse.instrumentation.asm.AsmUtils.*;
import static org.multiverse.stms.alpha.instrumentation.transactionalmethod.TransactionalMethodUtils.toTransactedMethodName;
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
public final class TransactionalClassMethodTransformer implements Opcodes {

    private static final String ALPHA_TRANSACTION_INTERNAL_NAME = Type.getInternalName(AlphaTransaction.class);

    private final ClassNode classNode;
    private final MetadataRepository metadataRepository;
    private final ClassNode donorClassNode;
    private final MethodNode donorMethodNode;
    private final MethodNode donorConstructorNode;
    private final String tranlocalName;
    private final ClassMetadata classMetadata;
    private final ClassLoader classLoader;
    private final Map<MethodNode, FieldNode> transactionFactoryFields = new HashMap<MethodNode, FieldNode>();
    private final boolean optimize;
    private final InstrumenterLogger logger;

    public TransactionalClassMethodTransformer(
            ClassLoader classLoader, ClassNode classNode, ClassNode donorClassNode,
            MetadataRepository metadataRepository, boolean optimize, InstrumenterLogger logger) {
        this.classLoader = classLoader;
        this.metadataRepository = metadataRepository;
        this.classNode = classNode;
        this.classMetadata = metadataRepository.loadClassMetadata(classLoader, classNode.name);
        this.tranlocalName = classMetadata.getTranlocalName();
        this.donorClassNode = donorClassNode;
        this.donorMethodNode = getDonorMethod("donorMethod");
        this.donorConstructorNode = getDonorMethod("donorConstructor");
        this.optimize = optimize;
        this.logger = logger;
    }

    public ClassNode transform() {
        addTransactionFactoryInitializationToStaticInitializer();
        classNode.methods = fixMethods();
        return classNode;
    }

    // ===========================================================================================
    //                      static initializer stuff (for transaction factories)
    // ===========================================================================================

    /**
     * Adds the creating of TransactionFactories for all the transactional methods to the
     * static initializer if needed. If one already is available, the initialization constructions
     * will be added to it. It also creates the static fields for storing the TransactionFactories.
     */
    private void addTransactionFactoryInitializationToStaticInitializer() {
        MethodNode staticInitializerNode = null;

        List<MethodNode> extraMethods = new LinkedList<MethodNode>();

        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

            if (methodMetadata != null && methodMetadata.isTransactional() && !methodMetadata.isAbstract()) {
                //txFactoryField is used to createReference the transaction fot this methodNode
                FieldNode txFactoryField = createTransactionFactoryField();
                classNode.fields.add(txFactoryField);

                transactionFactoryFields.put(methodNode, txFactoryField);

                //the static initializer for the transactionFactories.
                if (staticInitializerNode == null) {
                    MethodNode existingStaticInitializerNode = findStaticInitializerMethodNode();
                    if (existingStaticInitializerNode == null) {
                        //lets createReference a static initializer since it doesn't exist.
                        staticInitializerNode = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, new String[]{});
                        staticInitializerNode.instructions.add(new InsnNode(RETURN));
                        extraMethods.add(staticInitializerNode);
                    } else {
                        //we have found a static initalizer, lets use that one.
                        staticInitializerNode = existingStaticInitializerNode;
                    }
                }

                //add the transactionFactory initialization code to the front of the static initializer.
                TransactionMetadata transactionMetadata = methodMetadata.getTransactionalMetadata();
                InsnList insnList = transactionFactoryInitialization(transactionMetadata, txFactoryField);
                staticInitializerNode.instructions.insert(insnList);
            }
        }

        classNode.methods.addAll(extraMethods);
    }

    /**
     * Creates code for the initialization of a {@link TransactionFactory} for some method.
     *
     * @param transactionMetadata the TransactionMetadata for the method.
     * @param txFactoryField      the field where the created TransactionFactory is stored.
     * @return the created TransactionFactory.
     */
    private InsnList transactionFactoryInitialization(TransactionMetadata transactionMetadata, FieldNode txFactoryField) {
        InsnList insnList = new InsnList();

        //lets getClassMetadata the stm instance from the GlobalStmInstance
        insnList.add(new MethodInsnNode(
                INVOKESTATIC,
                Type.getInternalName(GlobalStmInstance.class),
                "getGlobalStmInstance",
                "()" + Type.getDescriptor(Stm.class)));

        //lets getClassMetadata the transactionFactoryBuilder from the stm.
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(Stm.class),
                "getTransactionFactoryBuilder",
                "()" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        boolean speculative = transactionMetadata.speculativeConfigurationEnabled;

        //speculativeConfigurationEnabled
        insnList.add(new InsnNode(speculative ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setSpeculativeConfigurationEnabled",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //readonly
        if (transactionMetadata.readOnly != null) {
            insnList.add(new InsnNode(transactionMetadata.readOnly ? ICONST_1 : ICONST_0));
            insnList.add(new MethodInsnNode(
                    INVOKEINTERFACE,
                    Type.getInternalName(TransactionFactoryBuilder.class),
                    "setReadonly",
                    "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));
        }

        //trackreads
        if (transactionMetadata.automaticReadTrackingEnabled != null) {
            insnList.add(new InsnNode(transactionMetadata.automaticReadTrackingEnabled ? ICONST_1 : ICONST_0));
            insnList.add(new MethodInsnNode(
                    INVOKEINTERFACE,
                    Type.getInternalName(TransactionFactoryBuilder.class),
                    "setAutomaticReadTrackingEnabled",
                    "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));
        }

        //familyName
        insnList.add(new LdcInsnNode(transactionMetadata.familyName));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setFamilyName",
                "(Ljava/lang/String;)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //interruptible.
        insnList.add(new InsnNode(transactionMetadata.interruptible ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setInterruptible",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //isWriteSkewProblemAllowed
        insnList.add(new InsnNode(transactionMetadata.writeSkewProblemAllowed ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setWriteSkewProblemAllowed",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //maxRetryCount
        insnList.add(new LdcInsnNode(transactionMetadata.maxRetryCount));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setMaxRetryCount",
                "(I)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //timeout
        insnList.add(new LdcInsnNode(transactionMetadata.timeoutNs));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setTimeoutNs",
                "(J)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

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

    // ===========================================================================================
    // ========================== fixing of transactional classes.
    // ===========================================================================================

    private List<MethodNode> fixMethods() {
        List<MethodNode> fixedMethods = new LinkedList<MethodNode>();

        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

            if (methodMetadata == null || !methodMetadata.isTransactional()) {
                fixedMethods.add(methodNode);
            } else if (methodMetadata.isAbstract()) {
                fixedMethods.add(methodNode);
                fixedMethods.add(createAbstractTransactedMethod(methodNode, true));
                fixedMethods.add(createAbstractTransactedMethod(methodNode, false));
            } else if (methodMetadata.isStatic()) {
                fixedMethods.add(createTransactionalMethod(methodNode));
                fixedMethods.add(createTransactedMethod(methodNode, true));
                fixedMethods.add(createTransactedMethod(methodNode, false));
            } else if (methodMetadata.isConstructor()) {
                fixedMethods.add(createTransactionalMethod(methodNode));
                fixedMethods.add(createTransactedConstructor(methodNode));
            } else {
                fixedMethods.add(createTransactionalMethod(methodNode));

                if (methodMetadata.getClassMetadata().isRealTransactionalObject()) {
                    fixedMethods.add(createRealTransactionalObjectTransactedMethod(methodNode, true));
                    fixedMethods.add(createRealTransactionalObjectTransactedMethod(methodNode, false));
                    fixedMethods.add(createRealTransactionalObjectTransactedMethodWithTranlocal(methodNode, true));
                    fixedMethods.add(createRealTransactionalObjectTransactedMethodWithTranlocal(methodNode, false));
                } else {
                    fixedMethods.add(createTransactedMethod(methodNode, true));
                    fixedMethods.add(createTransactedMethod(methodNode, false));
                }
            }
        }

        return fixedMethods;
    }

    private MethodNode createTransactedConstructor(MethodNode methodNode) {
        CloneMap cloneMap = new CloneMap();
        DebugInfo debugInfo = findDebugInfo(methodNode);

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        MethodNode result = new MethodNode();
        result.name = "<init>";
        result.access = methodNode.access;//todo: synthetic needs to be added
        result.desc = createTransactedMethodDesc(methodNode.desc);
        //result.signature = methodNode.signature;
        result.exceptions = methodNode.exceptions;
        result.localVariables = createNewVariableTableForMethodWithLogic(methodNode, cloneMap, startLabelNode, endLabelNode);
        result.tryCatchBlocks = cloneTryCatchBlocks(methodNode, cloneMap);
        result.instructions = transformOriginalLogic(methodNode, cloneMap, debugInfo, startLabelNode, endLabelNode, false);

        int transactionVar = indexOfTransactionVariable(methodNode.name, methodNode.desc);

        //if it a init of a transactional object, we need to make sure that the openForConstruction is called
        //this is done by adding it directly after calling the super call.
        if (classMetadata.isFirstGenerationRealTransactionalObject()) {
            int indexOfFirst = firstIndexAfterSuper(methodNode.name, result.instructions, classNode.superName);

            if (indexOfFirst >= 0) {
                InsnList initTranlocal = new InsnList();

                initTranlocal.add(new VarInsnNode(ALOAD, transactionVar));

                initTranlocal.add(new VarInsnNode(ALOAD, 0));

                String openForConstructionDesc = format("(%s)%s",
                        getDescriptor(AlphaTransactionalObject.class),
                        getDescriptor(AlphaTranlocal.class));

                initTranlocal.add(new MethodInsnNode(
                        INVOKEINTERFACE,
                        getInternalName(AlphaTransaction.class),
                        "openForConstruction",
                        openForConstructionDesc));

                //int tranlocalIndex =

                //todo: instead of popping it, place it in the tranlocal local variable.
                //this method doesn't contain a formal argument with the tranlocal, but needs a local
                //variable
                initTranlocal.add(new InsnNode(POP));

                result.instructions.insertBefore(result.instructions.get(indexOfFirst), initTranlocal);
            }
        }

        return result;
    }

    /**
     * Creates an abstract Transacted version of the method.
     *
     * @param methodNode the MethodNode for the method
     * @return the created abstract version of the method.
     */
    private MethodNode createAbstractTransactedMethod(MethodNode methodNode, boolean readonly) {
        MethodNode transactionMethod = new MethodNode();
        transactionMethod.access = methodNode.access;//todo: should be made synthetic.
        transactionMethod.name = toTransactedMethodName(methodNode.name, readonly);
        transactionMethod.exceptions = methodNode.exceptions;
        //todo: correct signature should be used here
        //transactionMethod.signature = methodNode.signature;
        transactionMethod.desc = createTransactedMethodDesc(methodNode.desc);
        return transactionMethod;
    }

    //forwards the logic to the tranlocal method.

    private MethodNode createRealTransactionalObjectTransactedMethod(MethodNode methodNode, boolean readonly) {
        CloneMap cloneMap = new CloneMap();

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        MethodNode result = new MethodNode();
        result.name = toTransactedMethodName(methodNode.name, readonly);
        result.access = methodNode.access;//todo: synthetic needs to be added
        result.desc = createTransactedMethodDesc(methodNode.desc);
        //result.signature = methodNode.signature;
        result.exceptions = methodNode.exceptions;

        //todo: no clone
        //result.localVariables = cloneVariableTableForLogicMethod(methodNode, cloneMap, startLabelNode, endLabelNode);

        result.tryCatchBlocks = cloneTryCatchBlocks(methodNode, cloneMap);
        result.instructions = new InsnList();

        result.instructions.add(startLabelNode);

        //push the this in the stack
        result.instructions.add(new VarInsnNode(ALOAD, 0));

        int loadIndex = 1;
        //place the rest of the arguments on the stack
        for (Type argType : getArgumentTypes(methodNode.desc)) {
            VarInsnNode loadInsn = new VarInsnNode(argType.getOpcode(ILOAD), loadIndex);
            result.instructions.add(loadInsn);
            loadIndex += argType.getSize();
        }

        int transactionVarIndex = indexOfTransactionVariable(methodNode.name, methodNode.desc);

        //push the transaction on the stack
        result.instructions.add(new VarInsnNode(ALOAD, transactionVarIndex));

        //push the tranlocal on the stack.
        result.instructions.add(new VarInsnNode(ALOAD, transactionVarIndex));
        result.instructions.add(new VarInsnNode(ALOAD, 0));
        String openForReadDesc = format("(%s)%s",
                getDescriptor(AlphaTransactionalObject.class),
                getDescriptor(AlphaTranlocal.class));
        //doing the method call that returns the tranlocal
        result.instructions.add(new MethodInsnNode(
                INVOKEINTERFACE,
                getInternalName(AlphaTransaction.class),
                "openForRead",
                openForReadDesc));
        result.instructions.add(new TypeInsnNode(
                CHECKCAST,
                tranlocalName));

        //call the tranlocal method (transaction and tranlocal are already pushed on the stack)
        MethodInsnNode invokeInsn = new MethodInsnNode(
                getInvokeOpcode(methodNode),
                classNode.name,
                toTransactedMethodName(methodNode.name, readonly),
                createTranlocalMethodDesc(methodNode.name, methodNode.desc));
        result.instructions.add(invokeInsn);

        //dealing with the return value.
        int returnOpCode = Type.getReturnType(methodNode.desc).getOpcode(IRETURN);
        result.instructions.add(new InsnNode(returnOpCode));

        //and finish it up and return the created MethodNode
        result.instructions.add(endLabelNode);
        return result;
    }

    /**
     * Creates the method that does the real logic. This method contains the original logic
     * (somehow a littlebit changed) of the method.
     *
     * @param methodNode
     * @return
     */
    private MethodNode createTransactedMethod(MethodNode methodNode, boolean readonly) {
        CloneMap cloneMap = new CloneMap();
        DebugInfo debugInfo = findDebugInfo(methodNode);

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        MethodNode result = new MethodNode();
        result.name = toTransactedMethodName(methodNode.name, readonly);
        result.access = methodNode.access;//todo: synthetic needs to be added
        result.desc = createTransactedMethodDesc(methodNode.desc);
        result.signature = methodNode.signature;
        result.exceptions = methodNode.exceptions;
        result.localVariables = cloneVariableTable(methodNode, cloneMap);

        //introduce the transaction.
        LocalVariableNode transactionVar = new LocalVariableNode(
                "transaction",
                Type.getDescriptor(AlphaTransaction.class),
                null,
                startLabelNode,
                endLabelNode,
                indexOfTransactionVariable(methodNode.name, methodNode.desc));

        result.localVariables.add(transactionVar);

        result.tryCatchBlocks = cloneTryCatchBlocks(methodNode, cloneMap);

        result.instructions = transformOriginalLogic(methodNode, cloneMap, debugInfo, startLabelNode, endLabelNode, readonly);
        return result;
    }

    private MethodNode getDonorMethod(String methodName) {
        for (MethodNode m : (List<MethodNode>) donorClassNode.methods) {
            if (m.name.equals(methodName)) {
                return m;
            }
        }

        throw new RuntimeException(format("method '%s' not found in class '%s'", methodName, donorClassNode.name));
    }

    private MethodNode findStaticInitializerMethodNode() {
        for (MethodNode method : (List<MethodNode>) classNode.methods) {
            if (method.name.equals("<clinit>")) {
                return method;
            }
        }

        return null;
    }

    private MethodNode getDonorMethodNode(MethodNode originalTxMethod) {
        if (originalTxMethod.name.equals("<init>")) {
            return donorConstructorNode;
        } else {
            return donorMethodNode;
        }
    }

    /**
     * Creates the static field that contains the txFactory for a transactional method.
     *
     * @return the created FieldNode.
     */
    private FieldNode createTransactionFactoryField() {
        int access = ACC_FINAL + ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC;
        String name = "___transactionFactory_" + System.nanoTime(); //todo: improve, use better name
        String desc = Type.getDescriptor(TransactionFactory.class);
        String sig = null;
        Object value = null;
        return new FieldNode(access, name, desc, sig, value);
    }


    /**
     * Creates a method that lifts on an already existing transaction and is going to contains the actual logic. The
     * Tranlocal and the Transaction will be passed as extra arguments to the method.
     * <p/>
     * A new methodNode will be returned, originalMethod remains untouched.
     *
     * @param methodNode the original MethodNode where the variable table is cloned from.
     * @return the transformed MethodNode.
     */
    public MethodNode createRealTransactionalObjectTransactedMethodWithTranlocal(MethodNode methodNode, boolean readonly) {
        CloneMap cloneMap = new CloneMap();
        DebugInfo debugInfo = findDebugInfo(methodNode);

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        MethodNode result = new MethodNode();
        result.name = toTransactedMethodName(methodNode.name, readonly);
        result.access = methodNode.access;//todo: synthetic needs to be added
        result.desc = createTranlocalMethodDesc(methodNode.name, methodNode.desc);
        //result.signature = methodNode.signature;
        result.exceptions = methodNode.exceptions;
        result.localVariables = createNewVariableTableForMethodWithLogic(methodNode, cloneMap, startLabelNode, endLabelNode);
        result.tryCatchBlocks = cloneTryCatchBlocks(methodNode, cloneMap);
        result.instructions = transformOriginalLogic(methodNode, cloneMap, debugInfo, startLabelNode, endLabelNode, readonly);
        return result;
    }


    private InsnList transformOriginalLogic(MethodNode methodNode, CloneMap cloneMap,
                                            DebugInfo debugInfo, LabelNode startLabelNode, LabelNode endLabelNode, boolean readonly) {

        MethodMetadata metadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

        logger.lessImportant("transactify %s.%s", classMetadata.getName(), methodNode.name);

        Analyzer a = new Analyzer(new SourceInterpreter());
        Frame[] frames;
        try {
            frames = a.analyze(classNode.name, methodNode);
        } catch (AnalyzerException e) {
            throw new CompileException("failed to create frames for " + classMetadata.getName() + "." + methodNode.name);
        }

        int transactionVar = indexOfTransactionVariable(methodNode.name, methodNode.desc);
        int tranlocalVar = indexOfTranlocalVariable(methodNode.name, methodNode.desc);

        InsnList newInstructions = new InsnList();
        newInstructions.add(startLabelNode);
        newInstructions.add(new LineNumberNode(debugInfo.beginLine, startLabelNode));

        for (int k = 0; k < methodNode.instructions.size(); k++) {
            AbstractInsnNode originalInsn = methodNode.instructions.get(k);
            AbstractInsnNode newInsn = null;
            switch (originalInsn.getOpcode()) {
                case -1:
                    if (!(originalInsn instanceof FrameNode)) {
                        newInsn = originalInsn.clone(cloneMap);
                    }
                    break;
                case PUTFIELD: {
                    //TODO: Should be improved by using frames instead of this
                    //complexity

                    FieldInsnNode originalFieldInsnNode = (FieldInsnNode) originalInsn;
                    ClassMetadata ownerMetadata = metadataRepository.loadClassMetadata(classLoader, originalFieldInsnNode.owner);
                    FieldMetadata fieldMetadata = ownerMetadata.getFieldMetadata(originalFieldInsnNode.name);

                    if (!fieldMetadata.isManagedField()) {
                        newInsn = originalInsn.clone(cloneMap);
                    } else {
                        Frame methodFrame = frames[methodNode.instructions.indexOf(originalFieldInsnNode)];
                        int size = 1 + (AsmUtils.isCategory2(originalFieldInsnNode.desc) ? 2 : 1);
                        int stackSlot = methodFrame.getStackSize() - size;

                        SourceValue stackValue = (SourceValue) methodFrame.getStack(stackSlot);
                        boolean aload0 = false;

                        if (stackValue.insns.size() > 0) {
                            if (stackValue.insns.size() > 1) {
                                throw new RuntimeException();
                            }

                            AbstractInsnNode node = (AbstractInsnNode) stackValue.insns.iterator().next();
                            if (node.getOpcode() == ALOAD) {
                                VarInsnNode varNode = (VarInsnNode) node;
                                if (varNode.var == 0) {
                                    aload0 = true;
                                    logger.lessImportant("   aload 0 found for candidate put optimization %s.%s",
                                            originalFieldInsnNode.owner, originalFieldInsnNode.name);
                                }
                            }
                        }

                        if (isCategory2(originalFieldInsnNode.desc)) {
                            //value(category2), owner(txobject),..

                            newInstructions.add(new InsnNode(DUP2_X1));
                            //[value(category2), owner(txobject), value(category2),...]

                            newInstructions.add(new InsnNode(POP2));
                            //[owner(txobject), value(category2), ...]
                        } else {
                            //[value(category1), owner(txobject),..
                            newInstructions.add(new InsnNode(SWAP));
                            //[owner(txobject), value(category1),..
                        }

                        LabelNode finishedLoadingLabel = new LabelNode();
                        LabelNode startFullLoadLabel = new LabelNode();
                        boolean fullMonty = aload0 && tranlocalVar > -1 && !methodNode.name.equals("<init>");

                        if (fullMonty) {
                            logger.lessImportant("   candidate for put optimization %s.%s",
                                    originalFieldInsnNode.owner, originalFieldInsnNode.name);

                            newInstructions.add(new VarInsnNode(ALOAD, tranlocalVar));

                            newInstructions.add(new MethodInsnNode(
                                    INVOKEVIRTUAL,
                                    Type.getType(AlphaTranlocal.class).getInternalName(),
                                    "isCommitted",
                                    "()Z"
                            ));

                            //if it already is committed, we need to do a full openForWrite.
                            newInstructions.add(new JumpInsnNode(IFNE, startFullLoadLabel));

                            //it was not committed, so we are allowed to do the shortcut access to the tranlocal
                            //remove the txObject and replace it by the tranlocal stored in the tranlocal var
                            newInstructions.add(new InsnNode(POP));
                            newInstructions.add(new VarInsnNode(ALOAD, tranlocalVar));
                            newInstructions.add(new JumpInsnNode(GOTO, finishedLoadingLabel));
                        }

                        newInstructions.add(startFullLoadLabel);

                        newInstructions.add(new VarInsnNode(ALOAD, transactionVar));

                        newInstructions.add(new InsnNode(SWAP));

                        newInstructions.add(new MethodInsnNode(
                                INVOKEINTERFACE,
                                getInternalName(AlphaTransaction.class),
                                "openForWrite",
                                format("(%s)%s", getDescriptor(AlphaTransactionalObject.class), getDescriptor(AlphaTranlocal.class))));

                        newInstructions.add(new TypeInsnNode(CHECKCAST, ownerMetadata.getTranlocalName()));

                        //store the loaded
                        if (fullMonty) {
                            newInstructions.add(new InsnNode(DUP));
                            newInstructions.add(new VarInsnNode(ASTORE, tranlocalVar));
                        }

                        newInstructions.add(finishedLoadingLabel);

                        if (isCategory2(originalFieldInsnNode.desc)) {
                            //[owner(tranlocal), value(category2),..

                            newInstructions.add(new InsnNode(DUP_X2));
                            //[owner(tranlocal), value(category2), owner(tranlocal)

                            newInstructions.add(new InsnNode(POP));
                            //[value(category2), owner(tranlocal),..
                        } else {
                            //[value(category1), owner(txobject),..
                            newInstructions.add(new InsnNode(SWAP));
                            //[owner(txobject), value(category1),..
                        }

                        newInsn = new FieldInsnNode(PUTFIELD,
                                ownerMetadata.getTranlocalName(),
                                originalFieldInsnNode.name,
                                originalFieldInsnNode.desc);
                    }
                }
                break;
                case GETFIELD: {
                    FieldInsnNode originalFieldInsnNode = (FieldInsnNode) originalInsn;
                    ClassMetadata ownerMetadata = metadataRepository.loadClassMetadata(classLoader, originalFieldInsnNode.owner);
                    FieldMetadata fieldMetadata = ownerMetadata.getFieldMetadata(originalFieldInsnNode.name);

                    if (!fieldMetadata.isManagedField()) {
                        newInsn = originalInsn.clone(cloneMap);
                    } else {
                        Frame methodFrame = frames[methodNode.instructions.indexOf(originalFieldInsnNode)];
                        int stackSlot = methodFrame.getStackSize() - 1;

                        SourceValue stackValue = (SourceValue) methodFrame.getStack(stackSlot);
                        boolean aload0 = false;

                        if (stackValue.insns.size() > 0) {
                            if (stackValue.insns.size() > 1) {
                                throw new RuntimeException();
                            }

                            AbstractInsnNode node = (AbstractInsnNode) stackValue.insns.iterator().next();
                            if (node.getOpcode() == ALOAD) {
                                VarInsnNode varNode = (VarInsnNode) node;
                                if (varNode.var == 0) {
                                    aload0 = true;
                                    logger.lessImportant("   aload 0 found for candidate get optimization %s.%s",
                                            originalFieldInsnNode.owner, originalFieldInsnNode.name);
                                }
                            }
                        }


                        LabelNode startFullLoadLabel = new LabelNode();
                        LabelNode continueGetFieldLabel = new LabelNode();

                        boolean fullMonty = aload0 && tranlocalVar > -1 && !methodNode.name.equals("<init>");

                        if (fullMonty) {
                            if (readonly) {
                                newInstructions.add(new InsnNode(POP));
                                newInstructions.add(new VarInsnNode(ALOAD, tranlocalVar));
                                newInstructions.add(new JumpInsnNode(GOTO, continueGetFieldLabel));
                            } else {
                                newInstructions.add(new VarInsnNode(ALOAD, tranlocalVar));
                                newInstructions.add(new MethodInsnNode(
                                        INVOKEVIRTUAL,
                                        getInternalName(AlphaTranlocal.class),
                                        "isCommitted",
                                        "()Z"
                                ));

                                //if it already is committed, we need to do a full openForWrite.
                                newInstructions.add(new JumpInsnNode(IFNE, startFullLoadLabel));

                                //it was not committed, so we are allowed to do the shortcut access to the tranlocal
                                //remove the txObject and replace it by the tranlocal stored in the tranlocal var
                                newInstructions.add(new InsnNode(POP));
                                newInstructions.add(new VarInsnNode(ALOAD, tranlocalVar));
                                newInstructions.add(new JumpInsnNode(GOTO, continueGetFieldLabel));
                                newInstructions.add(new JumpInsnNode(GOTO, startFullLoadLabel));
                            }
                        }

                        newInstructions.add(startFullLoadLabel);
                        newInstructions.add(new VarInsnNode(ALOAD, transactionVar));
                        newInstructions.add(new InsnNode(SWAP));
                        newInstructions.add(new MethodInsnNode(
                                INVOKEINTERFACE,
                                getInternalName(AlphaTransaction.class),
                                "openForRead",
                                format("(%s)%s", getDescriptor(AlphaTransactionalObject.class), getDescriptor(AlphaTranlocal.class))));
                        newInstructions.add(new TypeInsnNode(CHECKCAST, ownerMetadata.getTranlocalName()));

                        //store the loaded
                        if (fullMonty) {
                            newInstructions.add(new InsnNode(DUP));
                            newInstructions.add(new VarInsnNode(ASTORE, tranlocalVar));
                        }

                        newInstructions.add(continueGetFieldLabel);

                        newInstructions.add(new FieldInsnNode(GETFIELD, ownerMetadata.getTranlocalName(), originalFieldInsnNode.name, originalFieldInsnNode.desc));
                    }
                }
                break;
                case IINC: {
                    //take care of the additional introduced variables
                    IincInsnNode originalIncInsn = (IincInsnNode) originalInsn;
                    int newPos = newIndexOfLocalVariable(methodNode.name, methodNode.desc, originalIncInsn.var);
                    newInsn = new IincInsnNode(newPos, originalIncInsn.incr);
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
                    //take care of the additional introduced variables
                    VarInsnNode originalVarNode = (VarInsnNode) originalInsn;
                    int newPos = newIndexOfLocalVariable(methodNode.name, methodNode.desc, originalVarNode.var);
                    newInsn = new VarInsnNode(originalInsn.getOpcode(), newPos);
                }
                break;
                case INVOKESPECIAL:
                case INVOKEINTERFACE:
                case INVOKEVIRTUAL:
                    MethodInsnNode originalMethodInsnNode = (MethodInsnNode) originalInsn;

                    ClassMetadata ownerMetadata = metadataRepository.loadClassMetadata(
                            classLoader, originalMethodInsnNode.owner);
                    MethodMetadata ownerMethodMetadata = ownerMetadata.getMethodMetadata(
                            originalMethodInsnNode.name, originalMethodInsnNode.desc);

                    boolean optimizeTransactionalMethodCall = optimize
                            && ownerMethodMetadata != null
                            && ownerMethodMetadata.isTransactional();

                    logger.lessImportant("  executing call %s transactional %s   optimize allowed: %s",
                            originalMethodInsnNode.owner + "." + originalMethodInsnNode.name,
                            ownerMethodMetadata != null && ownerMethodMetadata.isTransactional(), optimize);

                    if (optimizeTransactionalMethodCall) {
                        Frame methodFrame = frames[methodNode.instructions.indexOf(originalMethodInsnNode)];
                        int stackSlot = methodFrame.getStackSize();

                        for (Type type : Type.getArgumentTypes(originalMethodInsnNode.desc)) {
                            stackSlot -= type.getSize();
                        }
                        stackSlot--;

                        SourceValue stackValue = (SourceValue) methodFrame.getStack(stackSlot);
                        boolean aload0 = false;
                        if (stackValue.insns.size() > 0) {
                            if (stackValue.insns.size() > 1) {
                                throw new RuntimeException();
                            }

                            AbstractInsnNode node = (AbstractInsnNode) stackValue.insns.iterator().next();
                            if (node.getOpcode() == ALOAD) {
                                VarInsnNode v = (VarInsnNode) node;
                                VarInsnNode varNode = (VarInsnNode) node;
                                if (varNode.var == 0) {
                                    aload0 = true;
                                }
                            }

                            //if (node.getOpcode() == INVOKEVIRTUAL || node.getOpcode() == INVOKEINTERFACE) {
                            //    MethodInsnNode m = (MethodInsnNode) node;
                            //    //    System.out.println("  startinsn methodinsn:" + m.owner + "." + m.name + "" + m.desc);
                            //} else if (node.getOpcode() == GETFIELD || node.getOpcode() == PUTFIELD) {
                            //    FieldInsnNode m = (FieldInsnNode) node;
                            //    //    System.out.println("  startinsn fieldinsn:" + m.owner + "." + m.name + "" + m.desc);
                            //}
                        }

                        boolean fullMonty = classMetadata.isRealTransactionalObject()
                                && aload0
                                && !methodNode.name.equals("<init>");

                        if (fullMonty) {
                            newInstructions.add(new VarInsnNode(ALOAD, transactionVar));
                            //tranlocal needs to be used here.
                            newInstructions.add(new VarInsnNode(ALOAD, tranlocalVar));
                            newInstructions.add(new MethodInsnNode(
                                    originalMethodInsnNode.getOpcode(),
                                    originalMethodInsnNode.owner,
                                    toTransactedMethodName(originalMethodInsnNode.name, readonly),
                                    createTransactedWithTranlocalMethodDesc(ownerMetadata, originalMethodInsnNode.name, originalMethodInsnNode.desc)
                            ));
                            logger.lessImportant("   ---full monty tranlocal method optimization %s.%s",
                                    originalMethodInsnNode.owner, originalMethodInsnNode.name);
                        } else {
                            newInstructions.add(new VarInsnNode(ALOAD, transactionVar));
                            newInstructions.add(new MethodInsnNode(
                                    originalMethodInsnNode.getOpcode(),
                                    originalMethodInsnNode.owner,
                                    toTransactedMethodName(originalMethodInsnNode.name, readonly),
                                    createTransactedMethodDesc(originalMethodInsnNode.desc)
                            ));
                            logger.lessImportant("   ---transactional method optimization %s.%s",
                                    originalMethodInsnNode.owner, originalMethodInsnNode.name);
                        }
                    } else {
                        newInsn = originalInsn.clone(cloneMap);
                    }
                    break;
                default:
                    newInsn = originalInsn.clone(cloneMap);
                    break;
            }

            if (newInsn != null) {
                newInstructions.add(newInsn);
            }
        }

        newInstructions.add(endLabelNode);
        return newInstructions;
    }

    private boolean startsWithALOAD0(Frame[] frames, AbstractInsnNode node) {
        while (node != null) {
            if (node.getPrevious() == null) {
                return false;
            }

            AbstractInsnNode prev = node.getPrevious();

            if (prev.getOpcode() == -1) {
                node = prev.getPrevious();
            } else if (prev.getOpcode() != ALOAD) {
                return false;
            } else {
                VarInsnNode varInsnNode = (VarInsnNode) prev;
                boolean result = varInsnNode.var == 0;
                if (result) {
                    //    System.out.println("--------------------------------");
                    //    System.out.println("ALOAD 0 found");
                    //    System.out.println("--------------------------------");
                }
                return result;
            }
        }

        return false;
    }

    private List createNewVariableTableForMethodWithLogic(MethodNode methodNode, CloneMap cloneMap, LabelNode startLabelNode, LabelNode endLabelNode) {
        List<LocalVariableNode> result = new LinkedList<LocalVariableNode>();

        //introduce the transaction.
        LocalVariableNode transactionVar = new LocalVariableNode(
                "transaction",
                Type.getDescriptor(AlphaTransaction.class),
                null,
                startLabelNode,
                endLabelNode,
                indexOfTransactionVariable(methodNode.name, methodNode.desc));
        result.add(transactionVar);

        //introduce the tranlocal if needed
        int tranlocalVarIndex = indexOfTranlocalVariable(methodNode.name, methodNode.desc);
        if (tranlocalVarIndex >= 0) {
            LocalVariableNode tranlocalVar = new LocalVariableNode(
                    "tranlocalThis",
                    internalToDesc(tranlocalName),
                    null,     //todo: signature
                    startLabelNode,
                    endLabelNode,
                    tranlocalVarIndex);
            result.add(tranlocalVar);
        }

        Set<Integer> encountered = new HashSet<Integer>();
        //copy all the rest of the local variables.
        for (LocalVariableNode originalLocalVar : (List<LocalVariableNode>) methodNode.localVariables) {
            int originalVarIndex = originalLocalVar.index;
            int clonedVarIndex = newIndexOfLocalVariable(methodNode.name, methodNode.desc, originalVarIndex);

            //it can happen that multiple entries are found for the same variable.
            //we need to detect that and skip duplicates (else we will get a classformaterror)
            if (!encountered.contains(originalVarIndex)) {
                encountered.add(originalVarIndex);
                LocalVariableNode clonedLocalVar = new LocalVariableNode(
                        originalLocalVar.name,
                        originalLocalVar.desc,
                        originalLocalVar.signature,
                        cloneMap.get(originalLocalVar.start),
                        cloneMap.get(originalLocalVar.end),
                        clonedVarIndex);

                result.add(clonedLocalVar);
            }
        }

        return result;
    }

    /**
     * Creates  the coordinating method; a method that is responsible for starting/committing aborting and retrying. The
     * real logic is forwarded to the lifting method. So this method doesn't contain the original logic anymore.
     * <p/>
     * The lifting method has received an extra (first) argument; the transaction that is managed by
     *
     * @param originalMethod the original MethodNode.
     * @return the coordinating method.
     */
    public MethodNode createTransactionalMethod(MethodNode originalMethod) {
        MethodNode donorMethodNode = getDonorMethodNode(originalMethod);

        FieldNode txFactoryFieldNode = transactionFactoryFields.get(originalMethod);

        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(originalMethod.name, originalMethod.desc);
        TransactionMetadata transactionMetadata = methodMetadata.getTransactionalMetadata();

        if (transactionMetadata.interruptible) {
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

        boolean isConstructor = "<init>".equals(originalMethod.name);

        CloneMap cloneMap = new CloneMap();

        //======================================================
        //      placement of the local variables
        //======================================================

        LocalVariableNode transactionVar = null;
        Map<Integer, Integer> varMapping = new HashMap<Integer, Integer>();


        //at the moment the start end endscope are harvested from the original instructions.
        //LabelNode startScope = cloneMap.getClassMetadata(originalMethod.instructions.getFirst());
        //LabelNode endScope = cloneMap.getClassMetadata(originalMethod.instructions.getLast());
        LabelNode startScope = new LabelNode();
        LabelNode endScope = new LabelNode();

        int var = 0;

        //createReference local variable for the 'this' if needed.
        if (!isStatic(originalMethod)) {
            LocalVariableNode clonedThis = new LocalVariableNode(
                    "this",
                    AsmUtils.internalToDesc(classNode.name),
                    null, //todo: signature
                    startScope,
                    endScope,
                    0);
            result.localVariables.add(clonedThis);
            var++;
        }

        //createReference local variables for all the method arguments.
        for (Type argType : getArgumentTypes(originalMethod.desc)) {
            LocalVariableNode clonedVar = new LocalVariableNode(
                    "arg" + result.localVariables.size(),
                    argType.getDescriptor(),
                    null, //todo: signature
                    startScope,
                    endScope,
                    var);
            var += argType.getSize();
            result.localVariables.add(clonedVar);
        }

        //createReference local variables based on the local variables of the donor method.
        for (LocalVariableNode donorVar : (List<LocalVariableNode>) donorMethodNode.localVariables) {
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

        //createReference the variable containing the result
        LocalVariableNode resultVariable = null;
        Type returnType = Type.getReturnType(originalMethod.desc);
        if (!returnType.equals(Type.VOID_TYPE)) {
            resultVariable = new LocalVariableNode(
                    "result",
                    returnType.getDescriptor(),
                    null, //todo: signature
                    startScope,
                    endScope,
                    var);
            result.localVariables.add(resultVariable);
            var += returnType.getSize();
        }

        if (transactionVar == null) {
            throw new RuntimeException(format("No transaction variable with name 'tx' is found in donor method '%s.%s'",
                    donorClassNode.name, donorMethodNode.name));
        }

        //======================================================
        //      placement of the try catch blocks
        //======================================================

        result.tryCatchBlocks = new LinkedList();
        result.tryCatchBlocks.addAll(cloneTryCatchBlockNodes(donorMethodNode.tryCatchBlocks, cloneMap));

        //======================================================
        //      placement of the instructions
        //======================================================
        result.instructions.add(startScope);

        for (ListIterator<AbstractInsnNode> it = (ListIterator<AbstractInsnNode>) donorMethodNode.instructions
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

                        if (isConstructor) {
                            result.instructions.add(new MethodInsnNode(
                                    INVOKESPECIAL,
                                    classNode.name,
                                    "<init>",
                                    createTransactedMethodDesc(originalMethod.desc)));
                        } else {
                            String transactedMethodName = toTransactedMethodName(
                                    originalMethod.name,
                                    donorMethodInsn.name.endsWith("___ro"));
                            result.instructions.add(new MethodInsnNode(
                                    getInvokeOpcode(originalMethod),
                                    classNode.name,
                                    transactedMethodName,
                                    createTransactedMethodDesc(originalMethod.desc)));

                            //deal with the return values
                            if (!returnType.equals(Type.VOID_TYPE)) {
                                result.instructions.add(
                                        new VarInsnNode(returnType.getOpcode(ISTORE), resultVariable.index));
                            }
                        }
                    } else {
                        AbstractInsnNode cloned = donorInsn.clone(cloneMap);
                        result.instructions.add(cloned);
                    }
                    break;
                case GETSTATIC: {
                    FieldInsnNode donorFieldInsnNode = (FieldInsnNode) donorInsn;
                    boolean donorIsOwner = donorFieldInsnNode.owner.equals(donorClassNode.name);

                    if (donorIsOwner && donorFieldInsnNode.name.equals("transactionFactory")) {
                        result.instructions.add(new FieldInsnNode(
                                GETSTATIC, classNode.name, txFactoryFieldNode.name, donorFieldInsnNode.desc));
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
                        //finally clauses. So for these variables, we just createReference a new
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

    public static boolean isReplacementMethod(MethodInsnNode donorMethodInsnNode) {
        if (!donorMethodInsnNode.name.equals("execute___ro") &&
                !donorMethodInsnNode.name.equals("execute___up")) {
            return false;
        }

        if (!donorMethodInsnNode.owner.equals(getInternalName(TransactionLogicDonor.class))) {
            return false;
        }

        return true;
    }

    // ==============================================

    public int newIndexOfLocalVariable(String methodName, String methodDesc, int originalVar) {
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodName, methodDesc);

        int firstPrivateLocalVar = sizeOfFormalParameters(methodDesc);

        if (!methodMetadata.isStatic()) {
            firstPrivateLocalVar++;
        }

        if (originalVar < firstPrivateLocalVar) {
            return originalVar;
        } else if (indexOfTranlocalVariable(methodName, methodDesc) >= 0) {
            //the transaction and tranlocal are introduced
            return originalVar + 2;
        } else {
            //only the transaction is introduced
            return originalVar + 1;
        }
    }

    public int indexOfTranlocalVariable(String methodName, String methodDesc) {
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodName, methodDesc);

        if (methodMetadata.isStatic() ||
                !methodMetadata.getClassMetadata().isRealTransactionalObject()) {
            return -1;
        }

        return sizeOfFormalParameters(methodDesc) + 2;
    }

    public int indexOfTransactionVariable(String methodName, String methodDesc) {
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodName, methodDesc);

        if (!methodMetadata.isTransactional()) {
            return -1;
        }

        int sizeOfFormalParameters = sizeOfFormalParameters(methodDesc);
        return methodMetadata.isStatic() ? sizeOfFormalParameters : sizeOfFormalParameters + 1;
    }

    private String createTransactedMethodDesc(String methodDesc) {
        return createMethodDescriptorWithRightIntroducedVariable(
                methodDesc, ALPHA_TRANSACTION_INTERNAL_NAME);
    }

    /**
     * Adds the transaction and then the tranlocal as extra parameters.
     *
     * @param methodName
     * @param methodDesc
     * @return
     */
    private String createTranlocalMethodDesc(String methodName, String methodDesc) {
        return createTransactedWithTranlocalMethodDesc(classMetadata, methodName, methodDesc);
    }


    private String createTransactedWithTranlocalMethodDesc(ClassMetadata classMetadata, String methodName, String methodDesc) {
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodName, methodDesc);

        if (methodMetadata.isStatic() || !methodMetadata.isTransactional()) {
            throw new IllegalStateException();
        }

        String result = AsmUtils.createMethodDescriptorWithRightIntroducedVariable(
                methodDesc, ALPHA_TRANSACTION_INTERNAL_NAME);

        result = AsmUtils.createMethodDescriptorWithRightIntroducedVariable(
                result, methodMetadata.getClassMetadata().getTranlocalName());

        return result;
    }

}
