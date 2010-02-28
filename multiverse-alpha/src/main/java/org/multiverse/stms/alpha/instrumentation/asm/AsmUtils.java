package org.multiverse.stms.alpha.instrumentation.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static java.lang.String.format;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

public final class AsmUtils implements Opcodes {

    public static int firstIndexAfterSuper(String methodName, InsnList instructions, String superClass) {
        if (!methodName.equals("<init>")) {
            throw new RuntimeException();
        }

        if (instructions == null) {
            return -1;
        }

        int depth = 0;
        for (int k = 0; k < instructions.size(); k++) {
            AbstractInsnNode insn = (AbstractInsnNode) instructions.get(k);
            //System.out.println(insn+" opcode: "+insn.getOpcode());
            if (insn.getOpcode() == INVOKESPECIAL) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                //System.out.printf("method %s.%s\n",methodInsn.owner,methodInsn.name);
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


    public static int firstFreeIndex(MethodNode methodNode) {
        int firstFreeIndex = 0;

        for (LocalVariableNode localVariableNode : (List<LocalVariableNode>) methodNode.localVariables) {
            firstFreeIndex += isCategory2(localVariableNode.desc) ? 2 : 1;
        }

        return firstFreeIndex;
    }

    public static int sizeOfFormalParameters(MethodNode methodNode) {
        return sizeOfFormalParameters(methodNode.desc);
    }

    public static int sizeOfFormalParameters(String desc) {
        int size = 0;

        for (Type argType : Type.getArgumentTypes(desc)) {
            size += argType.getSize();
        }

        return size;
    }


    public static int sizeOfLocalVariables(List<LocalVariableNode> localVariables) {
        int size = 0;

        for (LocalVariableNode localVariableNode : localVariables) {
            size += isCategory2(localVariableNode.desc) ? 2 : 1;
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
     * A new constructor descriptor is created by adding the extraArgType as the first argument (so the other arguments
     * all shift one pos to the right).
     *
     * @param oldDesc      the old method description
     * @param extraArgType the internal name of the type to introduce
     * @return the new method description.
     */
    public static String createShiftedMethodDescriptor(String oldDesc, String extraArgType) {
        Type[] oldArgTypes = Type.getArgumentTypes(oldDesc);
        Type[] newArgTypes = new Type[oldArgTypes.length + 1];
        newArgTypes[0] = Type.getObjectType(extraArgType);

        System.arraycopy(oldArgTypes, 0, newArgTypes, 1, oldArgTypes.length);

        Type returnType = Type.getReturnType(oldDesc);
        return Type.getMethodDescriptor(returnType, newArgTypes);
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

    public static void verify(ClassNode classNode) {
        verify(toBytecode(classNode));
    }

    /**
     * Checks if the bytecode is valid.
     *
     * @param bytes the bytecode to check.
     */
    public static void verify(byte[] bytes) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        CheckClassAdapter.verify(new ClassReader(bytes), false, pw);
        String msg = sw.toString();
        if (msg.length() > 0) {
            throw new RuntimeException(msg);
        }
    }

    public static void printVariableTable(List<LocalVariableNode> variables) {
        System.out.println("LocalVariables size = " + variables.size());
        for (LocalVariableNode localVariableNode : variables) {
            System.out.println(
                    "\t" + localVariableNode.name + " " + localVariableNode.desc + " " + localVariableNode.index);
        }
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
     * @param classInternalForm the internal name of the Class to load.
     * @return the loaded ClassNode.
     */
    public static ClassNode loadAsClassNode(ClassLoader loader, String classInternalForm) {
        if (loader == null || classInternalForm == null) {
            throw new NullPointerException();
        }

        String fileName = classInternalForm + ".class";
        InputStream is = loader.getResourceAsStream(fileName);

        try {
            ClassNode classNode = new ClassNode();
            ClassReader reader = new ClassReader(is);
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);
            return classNode;
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(format("Could not find file '%s' for class '%s': ",
                    fileName,
                    classInternalForm));
        } catch (IOException e) {
            throw new RuntimeException("A problem ocurred while loading class: " + fileName, e);
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

    public static String internalToDesc(String internalForm) {
        return format("L%s;", internalForm);
    }


    public static Method getMethod(Class clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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

    public static void writeToFixedTmpFile(Class clazz) {
        byte[] bytecode = toBytecode(loadAsClassNode(clazz));
        writeToFixedTmpFile(bytecode);
    }

    public static void writeToFixedTmpFile(ClassNode clazz) {
        byte[] bytecode = toBytecode(clazz);
        writeToFixedTmpFile(bytecode);
    }

    public static void writeToFixedTmpFile(byte[] bytecode) {
        File file = new File(getTmpDir(), "debug.class");
        writeToFile(file, bytecode);
    }

    public static String getTmpDir() {
        return System.getProperty("java.io.tmpdir");
    }

    public static File writeToFileInTmpDirectory(String filename, byte[] bytecode) {
        File file = new File(getTmpDir(), filename);
        writeToFile(file, bytecode);
        return file;
    }

    public static void writeToTmpFile(byte[] bytecode) throws IOException {
        File file = File.createTempFile("foo", ".class");
        writeToFile(file, bytecode);
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
