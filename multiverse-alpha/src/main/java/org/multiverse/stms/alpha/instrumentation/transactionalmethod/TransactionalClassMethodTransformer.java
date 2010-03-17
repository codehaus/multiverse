package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.multiverse.api.GlobalStmInstance;
import org.multiverse.api.Stm;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.instrumentation.DebugInfo;
import org.multiverse.instrumentation.asm.AsmUtils;
import org.multiverse.instrumentation.asm.CloneMap;
import org.multiverse.instrumentation.metadata.*;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.multiverse.instrumentation.asm.AsmUtils.*;
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
public class TransactionalClassMethodTransformer implements Opcodes {

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

    public TransactionalClassMethodTransformer(ClassLoader classLoader, ClassNode classNode, ClassNode donorClassNode, MetadataRepository metadataRepository) {
        this.classLoader = classLoader;
        this.metadataRepository = metadataRepository;
        this.classNode = classNode;
        this.classMetadata = metadataRepository.getClassMetadata(classLoader, classNode.name);
        this.tranlocalName = classMetadata.getTranlocalName();
        this.donorClassNode = donorClassNode;
        this.donorMethodNode = getDonorMethod("donorMethod");
        this.donorConstructorNode = getDonorMethod("donorConstructor");
    }

    public ClassNode transform() {
        if (classMetadata.isIgnoredClass() || !classMetadata.hasTransactionalMethods()) {
            return null;
        }

        addTransactionFactoryInitializationToStaticInitializer();
        classNode.methods = fixClassMethods();
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
                //txFactoryField is used to create the transaction fot this methodNode
                FieldNode txFactoryField = createTransactionFactoryField();
                classNode.fields.add(txFactoryField);

                transactionFactoryFields.put(methodNode, txFactoryField);

                //the static initializer for the transactionFactories.
                if (staticInitializerNode == null) {
                    MethodNode existingStaticInitializerNode = findStaticInitializer();
                    if (existingStaticInitializerNode == null) {
                        //lets create a static initializer since it doesn't exist.
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

        //timeout
        insnList.add(new LdcInsnNode(transactionMetadata.timeout));
        insnList.add(new FieldInsnNode(
                GETSTATIC,
                Type.getInternalName(TimeUnit.class),
                transactionMetadata.timeoutTimeUnit.name(),
                Type.getDescriptor(TimeUnit.class)));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setTimeout",
                "(JLjava/util/concurrent/TimeUnit;)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //familyName
        insnList.add(new LdcInsnNode(transactionMetadata.familyName));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setFamilyName",
                "(Ljava/lang/String;)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //readonly
        insnList.add(new InsnNode(transactionMetadata.readOnly ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setReadonly",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //trackreads
        insnList.add(new InsnNode(transactionMetadata.automaticReadTracking ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setAutomaticReadTracking",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //interruptible.
        insnList.add(new InsnNode(transactionMetadata.interruptible ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setInterruptible",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //allowWriteSkewProblem
        insnList.add(new InsnNode(transactionMetadata.allowWriteSkewProblem ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setAllowWriteSkewProblem",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //smartTxLength.
        insnList.add(new InsnNode(transactionMetadata.smartTxLengthSelector ? ICONST_1 : ICONST_0));
        insnList.add(new MethodInsnNode(
                INVOKEINTERFACE,
                Type.getInternalName(TransactionFactoryBuilder.class),
                "setSmartTxLengthSelector",
                "(Z)" + Type.getDescriptor(TransactionFactoryBuilder.class)));

        //familyName
        insnList.add(new LdcInsnNode(transactionMetadata.maxRetryCount));
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

    // ===========================================================================================
    // ========================== fixing of transactional classes.
    // ===========================================================================================

    private List<MethodNode> fixClassMethods() {
        List<MethodNode> methods = new LinkedList<MethodNode>();


        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

            if (methodMetadata == null || !methodMetadata.isTransactional()) {
                methods.add(methodNode);
            } else if (methodMetadata.isAbstract()) {
                methods.add(methodNode);
                methods.add(createAbstractTransactionMethod(methodNode));
            } else if (methodMetadata.isStatic()) {
                methods.add(createMasterMethod(methodNode));
                methods.add(createLogicMethod(methodNode));
            } else if (methodMetadata.isConstructor()) {
                methods.add(createMasterMethod(methodNode));
                methods.add(createLogicConstructor(methodNode));
            } else {
                methods.add(createMasterMethod(methodNode));

                if (methodMetadata.getClassMetadata().isRealTransactionalObject()) {
                    methods.add(createTranlocalRetrieveMethod(methodNode));
                    methods.add(createTranlocalLogicMethod(methodNode));
                } else {
                    methods.add(createLogicMethod(methodNode));
                }
            }
        }

        return methods;
    }

    private MethodNode createLogicConstructor(MethodNode methodNode) {
        CloneMap cloneMap = new CloneMap();
        DebugInfo debugInfo = findDebugInfo(methodNode);

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        MethodNode result = new MethodNode();
        result.name = "<init>";
        result.access = methodNode.access;//todo: synthetic needs to be added
        result.desc = createTransactionMethodDesc(methodNode.desc);
        result.signature = methodNode.signature;
        result.exceptions = methodNode.exceptions;
        result.localVariables = cloneVariableTableForLogicMethod(methodNode, cloneMap, startLabelNode, endLabelNode);
        result.tryCatchBlocks = cloneTryCatchBlocks(methodNode, cloneMap);
        result.instructions = transformOriginalLogic(methodNode, cloneMap, debugInfo, startLabelNode, endLabelNode);

        int transactionVar = indexOfTransactionVariable(methodNode.name, methodNode.desc);

        //if it a init of a transactional object, we need to make sure that the openForWrite is called
        //this is done by adding it directly after calling the super call.
        if (classMetadata.isFirstGenerationRealTransactionalObject()) {
            int indexOfFirst = firstIndexAfterSuper(methodNode.name, result.instructions, classNode.superName);

            if (indexOfFirst >= 0) {
                InsnList initTranlocal = new InsnList();

                initTranlocal.add(new VarInsnNode(ALOAD, transactionVar));

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

                result.instructions.insertBefore(result.instructions.get(indexOfFirst), initTranlocal);
            }
        }

        return result;
    }

    /**
     * Creates an abstract version of the method.
     *
     * @param methodNode the MethodNode for the method
     * @return the created abstract version of the method.
     */
    private MethodNode createAbstractTransactionMethod(MethodNode methodNode) {
        MethodNode transactionMethod = new MethodNode();
        transactionMethod.access = methodNode.access;//todo: should be made synthetic.
        transactionMethod.name = methodNode.name;
        transactionMethod.exceptions = methodNode.exceptions;
        transactionMethod.desc = createTransactionMethodDesc(methodNode.desc);
        return transactionMethod;
    }

    //forwards the logic to the tranlocal method.

    private MethodNode createTranlocalRetrieveMethod(MethodNode methodNode) {
        CloneMap cloneMap = new CloneMap();

        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        MethodNode result = new MethodNode();
        result.name = methodNode.name;
        result.access = methodNode.access;//todo: synthetic needs to be added
        result.desc = createTransactionMethodDesc(methodNode.desc);
        result.signature = methodNode.signature;
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
                methodNode.name,
                createTranlocalMethodDesc(methodNode.name, methodNode.desc));
        result.instructions.add(invokeInsn);

        //dealing with the return value.
        int returnOpCode = Type.getReturnType(methodNode.desc).getOpcode(IRETURN);
        result.instructions.add(new InsnNode(returnOpCode));

        //and finish it up and return the created MethodNode
        result.instructions.add(endLabelNode);
        return result;
    }

    //does the actual logic

    //does the actual logic

    private MethodNode createLogicMethod(MethodNode methodNode) {
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata(methodNode.name, methodNode.desc);
        CloneMap cloneMap = new CloneMap();
        DebugInfo debugInfo = findDebugInfo(methodNode);

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        MethodNode result = new MethodNode();
        result.name = methodNode.name;
        result.access = methodNode.access;//todo: synthetic needs to be added
        result.desc = createTransactionMethodDesc(methodNode.desc);
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

        result.instructions = transformOriginalLogic(methodNode, cloneMap, debugInfo, startLabelNode, endLabelNode);
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

    private MethodNode findStaticInitializer() {
        for (MethodNode method : (List<MethodNode>) classNode.methods) {
            if (method.name.equals("<clinit>")) {
                return method;
            }
        }

        return null;
    }

    private MethodNode getDonor(MethodNode originalTxMethod) {
        MethodNode donor;
        if (originalTxMethod.name.equals("<init>")) {
            donor = donorConstructorNode;
        } else {
            donor = donorMethodNode;
        }
        return donor;
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
    public MethodNode createTranlocalLogicMethod(MethodNode methodNode) {
        CloneMap cloneMap = new CloneMap();
        DebugInfo debugInfo = findDebugInfo(methodNode);

        LabelNode startLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        MethodNode result = new MethodNode();
        result.name = methodNode.name;
        result.access = methodNode.access;//todo: synthetic needs to be added
        result.desc = createTranlocalMethodDesc(methodNode.name, methodNode.desc);
        result.signature = methodNode.signature;
        result.exceptions = methodNode.exceptions;
        result.localVariables = cloneVariableTableForLogicMethod(methodNode, cloneMap, startLabelNode, endLabelNode);
        result.tryCatchBlocks = cloneTryCatchBlocks(methodNode, cloneMap);
        result.instructions = transformOriginalLogic(methodNode, cloneMap, debugInfo, startLabelNode, endLabelNode);
        return result;
    }

    private InsnList transformOriginalLogic(MethodNode methodNode, CloneMap cloneMap,
                                            DebugInfo debugInfo, LabelNode startLabelNode, LabelNode endLabelNode) {

        int transactionVar = indexOfTransactionVariable(methodNode.name, methodNode.desc);

        InsnList instructions = new InsnList();
        instructions.add(startLabelNode);
        instructions.add(new LineNumberNode(debugInfo.beginLine, startLabelNode));

        for (int k = 0; k < methodNode.instructions.size(); k++) {
            AbstractInsnNode originalInsn = methodNode.instructions.get(k);
            AbstractInsnNode clonedInsn = null;
            switch (originalInsn.getOpcode()) {
                case -1:
                    if (!(originalInsn instanceof FrameNode)) {
                        clonedInsn = originalInsn.clone(cloneMap);
                    }
                    break;
                case PUTFIELD: {
                    FieldInsnNode fieldInsn = (FieldInsnNode) originalInsn;
                    ClassMetadata ownerMetadata = metadataRepository.getClassMetadata(classLoader, fieldInsn.owner);
                    FieldMetadata fieldMetadata = ownerMetadata.getFieldMetadata(fieldInsn.name);

                    if (fieldMetadata.isManagedField()) {
                        AbstractInsnNode previous = methodNode.instructions.get(k - 1);

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

                        instructions.add(new VarInsnNode(ALOAD, transactionVar));

                        instructions.add(new InsnNode(SWAP));

                        instructions.add(new MethodInsnNode(
                                INVOKEINTERFACE,
                                getInternalName(AlphaTransaction.class),
                                "openForWrite",
                                format("(%s)%s", getDescriptor(AlphaTransactionalObject.class), getDescriptor(AlphaTranlocal.class))));

                        instructions.add(new TypeInsnNode(CHECKCAST, ownerMetadata.getTranlocalName()));

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


                        clonedInsn = new FieldInsnNode(PUTFIELD, ownerMetadata.getTranlocalName(), fieldInsn.name, fieldInsn.desc);
                    } else {
                        clonedInsn = originalInsn.clone(cloneMap);
                    }
                }
                break;
                case GETFIELD: {
                    FieldInsnNode fieldInsn = (FieldInsnNode) originalInsn;
                    ClassMetadata ownerMetadata = metadataRepository.getClassMetadata(classLoader, fieldInsn.owner);
                    FieldMetadata fieldMetadata = ownerMetadata.getFieldMetadata(fieldInsn.name);

                    if (fieldMetadata.isManagedField()) {
                        instructions.add(new VarInsnNode(ALOAD, transactionVar));
                        instructions.add(new InsnNode(SWAP));
                        instructions.add(new MethodInsnNode(
                                INVOKEINTERFACE,
                                getInternalName(AlphaTransaction.class),
                                "openForRead",
                                format("(%s)%s", getDescriptor(AlphaTransactionalObject.class), getDescriptor(AlphaTranlocal.class))));
                        instructions.add(new TypeInsnNode(CHECKCAST, ownerMetadata.getTranlocalName()));
                        instructions.add(new FieldInsnNode(GETFIELD, ownerMetadata.getTranlocalName(), fieldInsn.name, fieldInsn.desc));
                    } else {
                        clonedInsn = originalInsn.clone(cloneMap);
                    }
                }
                break;
                case IINC: {
                    IincInsnNode originalIncInsn = (IincInsnNode) originalInsn;
                    int newPos = newIndexOfLocalVariable(methodNode.name, methodNode.desc, originalIncInsn.var);
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
                    int newPos = newIndexOfLocalVariable(methodNode.name, methodNode.desc, originalVarNode.var);
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

        instructions.add(endLabelNode);
        return instructions;
    }

    private List cloneVariableTableForLogicMethod(MethodNode methodNode, CloneMap cloneMap, LabelNode startLabelNode, LabelNode endLabelNode) {
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
                    null,
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
     * real logic is forwarded to the lifting method.
     * <p/>
     * The lifting method has received an extra (first) argument; the transaction that is managed by
     *
     * @param originalMethod the original MethodNode.
     * @return the coordinating method.
     */
    public MethodNode createMasterMethod(MethodNode originalMethod) {
        MethodNode donorMethodNode = getDonor(originalMethod);

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

        if (isAbstract(originalMethod)) {
            return result;
        }

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

                        //do the method call
                        MethodInsnNode invokeInsn = new MethodInsnNode(
                                getInvokeOpcode(originalMethod),
                                classNode.name,
                                originalMethod.name,
                                createTransactionMethodDesc(originalMethod.desc));
                        result.instructions.add(invokeInsn);

                        //deal with the return values
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

    public static boolean isReplacementMethod(MethodInsnNode donorMethodInsnNode) {
        if (!donorMethodInsnNode.name.equals("execute")) {
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
        if (methodMetadata.isStatic()) {
            return -1;
        }

        if (!methodMetadata.getClassMetadata().isTransactionalObject()) {
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

    private String createTransactionMethodDesc(String methodDesc) {
        return createMethodDescriptorWithRightIntroducedVariable(
                methodDesc, ALPHA_TRANSACTION_INTERNAL_NAME);
    }

    private String createTranlocalMethodDesc(String methodName, String methodDesc) {
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
