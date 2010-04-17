package org.multiverse.instrumentation.asm;

import org.multiverse.instrumentation.DebugInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.*;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static java.lang.String.format;
import static org.objectweb.asm.Type.*;

public final class AsmUtils implements Opcodes {


    public static void printClassOfTopItem(InsnList instructions) {
        System.out.println("invoke printClassOfTopItem");

        instructions.add(new MethodInsnNode(
                INVOKEVIRTUAL,
                Type.getInternalName(Object.class),
                "getClass",
                "()Ljava/lang/Class;"));


        instructions.add(new MethodInsnNode(
                INVOKESTATIC,
                Type.getInternalName(AsmUtils.class),
                "printClazz",
                "(Ljava/lang/Class;)V"
        ));
    }

    public static void printClass(Class clazz) {
        System.out.println("  class on top: " + clazz.getName());
    }

    public static LocalVariableNode findThisVariable(MethodNode methodNode) {
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

    public static DebugInfo findDebugInfo(MethodNode method) {
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

    public static int getInvokeOpcode(MethodNode methodNode) {
        if (isStatic(methodNode.access)) {
            return INVOKESTATIC;
        } else if (methodNode.name.equals("<init>")) {
            return INVOKESPECIAL;
        } else {
            return INVOKEVIRTUAL;
        }
    }

    public static List<TryCatchBlockNode> cloneTryCatchBlockNodes(List<TryCatchBlockNode> originalBlocks, CloneMap cloneMap) {
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

    public static List<TryCatchBlockNode> cloneTryCatchBlocks(MethodNode originalMethod, CloneMap cloneMap) {
        //clone the try catch blocks
        List<TryCatchBlockNode> result = new LinkedList<TryCatchBlockNode>();

        for (int k = 0; k < originalMethod.tryCatchBlocks.size(); k++) {
            TryCatchBlockNode original = (TryCatchBlockNode) originalMethod.tryCatchBlocks.get(k);
            TryCatchBlockNode cloned = new TryCatchBlockNode(
                    cloneMap.get(original.start),
                    cloneMap.get(original.end),
                    cloneMap.get(original.handler),
                    original.type);
            result.add(cloned);
        }

        return result;
    }

    public static List cloneVariableTable(MethodNode methodNode, CloneMap cloneMap) {
        List<LocalVariableNode> result = new LinkedList<LocalVariableNode>();

        //copy all the rest of the local variables.
        for (LocalVariableNode originalLocalVar : (List<LocalVariableNode>) methodNode.localVariables) {
            LocalVariableNode clonedLocalVar = new LocalVariableNode(
                    originalLocalVar.name,
                    originalLocalVar.desc,
                    originalLocalVar.signature,
                    cloneMap.get(originalLocalVar.start),
                    cloneMap.get(originalLocalVar.end),
                    originalLocalVar.index);
            result.add(clonedLocalVar);
        }

        return result;
    }


    public static int firstIndexAfterSuper(String methodName, InsnList instructions, String superClass) {
        if (!methodName.equals("<init>")) {
            throw new RuntimeException();
        }

        if (instructions == null) {
            return -1;
        }

        int depth = 0;
        for (int k = 0; k < instructions.size(); k++) {
            AbstractInsnNode insn = instructions.get(k);
            if (insn.getOpcode() == INVOKESPECIAL) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (methodInsn.name.equals("<init>") && methodInsn.owner.endsWith(superClass)) {
                    if (depth == 0) {
                        return k + 1;
                    } else {
                        depth--;
                    }
                }
            } else if (insn.getOpcode() == NEW) {
                TypeInsnNode typeInsn = (TypeInsnNode) insn;
                if (typeInsn.desc.equals(superClass)) {
                    depth++;
                }
            }
        }

        return -1;
    }

    public static int firstIndexAfterSuper(MethodNode methodNode, String superClass) {
        return firstIndexAfterSuper(methodNode.name, methodNode.instructions, superClass);
    }

    public static String toString(AbstractInsnNode insnNode) {
        TraceMethodVisitor asmifier = new TraceMethodVisitor();
        insnNode.accept(asmifier);

        StringBuffer sb = new StringBuffer();
        for (String line : (List<String>) asmifier.getText()) {
            sb.append(line);
        }

        return sb.toString();
    }

    public static int sizeOfFormalParameters(String desc) {
        int size = 0;

        for (Type argType : Type.getArgumentTypes(desc)) {
            size += argType.getSize();
        }

        return size;
    }

    public static boolean isCategory2(String valueDesc) {
        return valueDesc.equals("J") || valueDesc.equals("D");
    }

    public static int upgradeToPublic(int access) {
        if (isPublic(access)) {
            return access;
        }

        if (isPrivate(access)) {
            access = access - ACC_PRIVATE;
        } else if (isProtected(access)) {
            access = access - ACC_PROTECTED;
        }

        return access + ACC_PUBLIC;
    }


    public static int upgradeToProtected(int access) {
        if (isProtected(access)) {
            return access;
        }

        if (isPublic(access)) {
            return access;
        }

        if (isPrivate(access)) {
            access = access - ACC_PRIVATE;
        }

        return access + ACC_PROTECTED;
    }


    /**
     * @param methodDesc   the original method descriptor
     * @param extraArgType internal name of extra argument to add to the right
     * @return the new method descripion
     */
    public static String createMethodDescriptorWithRightIntroducedVariable(String methodDesc, String extraArgType) {
        Type returnType = Type.getReturnType(methodDesc);
        Type[] argTypes = Type.getArgumentTypes(methodDesc);
        Type[] newArgTypes = new Type[argTypes.length + 1];
        System.arraycopy(argTypes, 0, newArgTypes, 0, argTypes.length);
        newArgTypes[argTypes.length] = Type.getObjectType(extraArgType);
        return getMethodDescriptor(returnType, newArgTypes);
    }

    public static MethodNode remap(MethodNode originalMethod, Remapper remapper) {
        String[] exceptions = getExceptions(originalMethod);

        MethodNode mappedMethod = new MethodNode(
                originalMethod.access,
                originalMethod.name,
                remapper.mapMethodDesc(originalMethod.desc),
                remapper.mapSignature(originalMethod.signature, false),
                remapper.mapTypes(exceptions));

        RemappingMethodAdapter remapVisitor = new RemappingMethodAdapter(
                mappedMethod.access,
                mappedMethod.desc,
                mappedMethod,
                remapper);
        originalMethod.accept(remapVisitor);
        return mappedMethod;
    }


    public static String[] getExceptions(MethodNode originalMethod) {
        if (originalMethod.exceptions == null) {
            return new String[]{};
        }

        String[] exceptions = new String[originalMethod.exceptions.size()];
        originalMethod.exceptions.toArray(exceptions);
        return exceptions;
    }


    /**
     * Loads a Class as ClassNode. The ClassLoader of the Class is used to retrieve a resource stream.
     *
     * @param clazz the Class to load as ClassNode.
     * @return the loaded ClassNode.
     */
    public static ClassNode loadAsClassNode(Class clazz) {
        return loadAsClassNode(clazz.getClassLoader(), getInternalName(clazz));
    }

    /**
     * Loads a Class as ClassNode.
     *
     * @param loader            the ClassLoader to getClassMetadata the resource stream of.
     * @param classInternalName the internal name of the Class to load.
     * @return the loaded ClassNode.
     */
    public static ClassNode loadAsClassNode(ClassLoader loader, String classInternalName) {
        if (loader == null || classInternalName == null) {
            throw new NullPointerException();
        }

        String fileName = classInternalName + ".class";
        InputStream is = loader.getResourceAsStream(fileName);

        try {
            ClassNode classNode = new ClassNode();
            ClassReader reader = new ClassReader(is);
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);
            return classNode;
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(format("Could not find file '%s' for class '%s': ",
                    fileName,
                    classInternalName));
        } catch (IOException e) {
            throw new RuntimeException("A problem ocurred while loading class: " + fileName, e);
        }
    }

    /**
     * Loads a Class as ClassNode.
     * <p/>
     * todo: code of this method is very nasty with closing streams.
     *
     * @return the loaded ClassNode.
     */
    public static ClassNode loadAsClassNode(File file) {

        InputStream is = null;
        try {
            is = new FileInputStream(file);
            ClassNode classNode = new ClassNode();
            ClassReader reader = new ClassReader(is);
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);
            return classNode;
        } catch (IOException e) {
            throw new RuntimeException("A problem ocurred while loading class: " + file, e);
        } finally {
            closeQuietly(is);
        }
    }

    private static void closeQuietly(InputStream is) {
        if (is == null) {
            return;
        }

        try {
            is.close();
        } catch (IOException ignore) {
        }
    }

    public static byte[] loadAsBytecode(File file) {
        try {
            InputStream is = new FileInputStream(file);
            ClassNode classNode = new ClassNode();
            ClassReader reader = new ClassReader(is);
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);
            return toBytecode(classNode);
        } catch (IOException e) {
            throw new RuntimeException("A problem ocurred while loading class: " + file, e);
        }
    }


    /**
     * Checks if a ClassNode has the specified visible annotation.
     *
     * @param memberNode      the ClassNode to check
     * @param annotationClass the Annotation class that is checked for.
     * @return true if classNode has the specified annotation, false otherwise.
     */
    public static boolean hasVisibleAnnotation(MemberNode memberNode, Class annotationClass) {
        return getVisibleAnnotation(memberNode, annotationClass) != null;
    }

    public static AnnotationNode getVisibleAnnotation(MemberNode memberNode, Class annotationClass) {
        if (memberNode == null || annotationClass == null) {
            throw new NullPointerException();
        }

        if (memberNode.visibleAnnotations == null) {
            return null;
        }

        String annotationClassDescriptor = getDescriptor(annotationClass);

        for (AnnotationNode node : (List<AnnotationNode>) memberNode.visibleAnnotations) {
            if (annotationClassDescriptor.equals(node.desc)) {
                return node;
            }
        }

        return null;
    }

    public static Object getAnnotationValue(AnnotationNode annotationNode, String valueName) {
        List values = annotationNode.values;

        if (values == null) {
            return null;
        }

        for (int k = 0; k < values.size(); k += 2) {
            if (values.get(k).equals(valueName)) {
                return values.get(k + 1);
            }
        }

        return null;
    }

    public static String internalToDesc(String internalForm) {
        return format("L%s;", internalForm);
    }

    public static Field getField(Class clazz, String fieldName) {
        try {
            return clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isAbstract(MethodNode methodNode) {
        return isAbstract(methodNode.access);
    }

    public static boolean isInterface(ClassNode classNode) {
        return (classNode.access & Opcodes.ACC_INTERFACE) != 0;
    }

    public static boolean isNative(MethodNode methodNode) {
        return isNative(methodNode.access);
    }

    public static boolean isFinal(FieldNode fieldNode) {
        return isFinal(fieldNode.access);
    }


    public static boolean isStatic(FieldNode fieldNode) {
        return isStatic(fieldNode.access);
    }

    public static boolean isStatic(MethodNode methodNode) {
        return isStatic(methodNode.access);
    }

    public static boolean isPrivate(MethodNode methodNode) {
        return isPrivate(methodNode.access);
    }

    public static boolean isPrivate(int access) {
        return (access & Opcodes.ACC_PRIVATE) != 0;
    }

    public static boolean isPublic(int access) {
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    public static boolean isProtected(int access) {
        return (access & Opcodes.ACC_PROTECTED) != 0;
    }

    public static boolean isFinal(int access) {
        return (access & Opcodes.ACC_FINAL) != 0;
    }

    public static boolean isSynthetic(int access) {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public static boolean isNative(int access) {
        return (access & Opcodes.ACC_NATIVE) != 0;
    }

    public static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    public static boolean isAbstract(int access) {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public static ClassNode loadAsClassNode(byte[] bytecode) {
        if (bytecode == null) {
            throw new NullPointerException();
        }

        ClassNode classNode = new ClassNode();
        ClassReader cr = new ClassReader(bytecode);
        cr.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    public static byte[] toBytecode(ClassNode classNode) {
        if (classNode == null) {
            throw new NullPointerException();
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }

    public static String getTmpDir() {
        return System.getProperty("java.io.tmpdir");
    }

    public static void writeToFile(File file, byte[] bytecode) {
        if (file == null || bytecode == null) {
            throw new NullPointerException();
        }

        try {
            ensureExistingParent(file);

            OutputStream writer = new FileOutputStream(file);
            try {
                writer.write(bytecode);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void ensureExistingParent(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent.isDirectory()) {
            return;
        }

        if (!parent.mkdirs()) {
            throw new IOException("Failed to make parent directories for file " + file);
        }
    }

    private AsmUtils() {
    }
}
