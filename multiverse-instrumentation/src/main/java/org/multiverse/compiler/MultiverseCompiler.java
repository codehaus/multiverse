package org.multiverse.compiler;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.multiverse.instrumentation.asm.AsmUtils;
import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.ClazzCompiler;
import org.multiverse.instrumentation.compiler.FileSystemFiler;
import org.multiverse.instrumentation.compiler.SystemOutLog;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.lang.String.format;

/**
 * The MultiverseCompiler is responsible for transforming class files. It is a general purpose
 * compiler since it doesn't do any compiling itself, but forwards everything a
 * {@link ClazzCompiler}  The advantage is that not yet another MultiverseCompiler needs to
 * be written; so shared plumbing.
 * <p/>
 * Another advange is that the same ClazzCompiler can be used for the MultiverseJavaAgent
 * and the MultiverseCompiler.
 *
 * @author Peter Veentjer
 */
public final class MultiverseCompiler {

    public static void main(String[] args) {
        MultiverseCompiler multiverseCompiler = new MultiverseCompiler();
        multiverseCompiler.run(args);
    }

    private ClassLoader compilerClassLoader;

    private void run(String[] args) {
        System.out.println("Initializing Multiverse Compiler");

        OptionParser parser = new OptionParser();
        parser.accepts("compiler")
                .withRequiredArg()
                .ofType(String.class)
                .describedAs("The full class name of the ClazzCompiler to use");

        parser.accepts("targetDirectory")
                .withRequiredArg()
                .ofType(String.class)
                .describedAs("The path of the directory of classes to transform recursivly");

        OptionSet options = parser.parse(args);

        String clazzCompilerName = (String) options.valueOf("compiler");

        ClazzCompiler compiler = createClazzCompiler(clazzCompilerName);
        compiler.setLog(new SystemOutLog());

        System.out.println("Using ClazzCompiler:" + compiler.getCompilerName() + " " + compiler.getCompilerVersion());

        File targetDirectory = new File((String) options.valueOf("targetDirectory"));
        compilerClassLoader = new MyClassLoader(targetDirectory, MultiverseCompiler.class.getClassLoader());

        if (!targetDirectory.isDirectory()) {
            String msg = format("Target directory '%s' is not a directory", targetDirectory);
            throw new RuntimeException(msg);
        }

        compiler.setFiler(new FileSystemFiler(targetDirectory));

        System.out.printf("Transforming classes in targetDirectory %s\n", targetDirectory);

        recursiveApply(targetDirectory, compiler);
    }

    public void recursiveApply(File directory, ClazzCompiler clazzCompiler) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                recursiveApply(file, clazzCompiler);
            } else if (file.getName().endsWith(".class")) {
                transform(file, clazzCompiler);
            }
        }
    }

    private void transform(File file, ClazzCompiler clazzCompiler) {
        Clazz clazz = load(file);
        Clazz result = clazzCompiler.process(clazz);
        write(file, result);
    }

    private Clazz load(File file) {
        ClassNode node = AsmUtils.loadAsClassNode(file);

        Clazz clazz = new Clazz(node.name);
        clazz.setBytecode(AsmUtils.toBytecode(node));
        clazz.setClassLoader(compilerClassLoader);
        return clazz;
    }

    class MyClassLoader extends ClassLoader {
        private final File rootDirectory;

        protected MyClassLoader(File rootDirectory, ClassLoader parent) {
            super(parent);
            this.rootDirectory = rootDirectory;
        }

        public Class findClass(String className) throws ClassNotFoundException {
            String filename = className.replace('.', '/') + ".class";
            File file = new File(rootDirectory, filename);

            if (!file.exists()) {
                return super.findClass(className);
            }

            ClassNode node = AsmUtils.loadAsClassNode(file);
            byte[] bytecode = AsmUtils.toBytecode(node);
            return defineClass(className, bytecode, 0, bytecode.length, null);
        }

        public InputStream getResourceAsStream(String resource) {
            int indexOfLastDot = resource.lastIndexOf(".");
            String filename;
            if (indexOfLastDot == -1) {
                filename = resource;
            } else {
                String begin = resource.substring(0, indexOfLastDot).replace('.', '/');
                String end = resource.substring(indexOfLastDot);
                filename = begin + end;
            }
            File file = new File(rootDirectory, filename);

            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            return super.getResourceAsStream(resource);
        }
    }

    public void write(File file, Clazz clazz) {
        System.out.println("Writing file: " + file);

        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(clazz.getBytecode());
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ClazzCompiler createClazzCompiler(String compilerClassName) {
        System.out.println(format("Multiverse Compiler: using ClazzCompiler '%s'", compilerClassName));

        Constructor constructor = getMethod(compilerClassName);
        try {
            return (ClazzCompiler) constructor.newInstance();
        } catch (IllegalAccessException e) {
            String msg = format("Failed to initialize ClazzCompiler '%s'." +
                    "The constructor is not accessable.",
                    compilerClassName);
            throw new IllegalArgumentException(msg, e);
        } catch (InvocationTargetException e) {
            String msg = format("Failed to initialize ClazzCompiler '%s'." +
                    "The constructor threw an exception.",
                    compilerClassName);
            System.out.println(msg);
            throw new IllegalArgumentException(msg, e);
        } catch (InstantiationException e) {
            String msg = format("Failed to initialize ClazzCompiler '%s'." +
                    "The class could not be instantiated.",
                    compilerClassName);
            System.out.println(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    private static Constructor getMethod(String className) {
        if (className == null) {
            throw new NullPointerException();
        }
        Class compilerClazz;
        try {
            compilerClazz = MultiverseCompiler.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            String msg = format("Failed to initialize ClazzCompiler '%s'." +
                    "Is not an existing class (it can't be found using the Thread.currentThread.getContextClassLoader).",
                    className);
            System.out.println(msg);
            throw new IllegalArgumentException(msg, e);
        }

        if (!ClazzCompiler.class.isAssignableFrom(compilerClazz)) {
            String msg = format("Failed to initialize ClazzCompiler '%s'." +
                    "Is not an subclass of org.multiverse.compiler.ClazzCompiler).",
                    className);
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        }

        Constructor method;
        try {
            method = compilerClazz.getConstructor();
        } catch (NoSuchMethodException e) {
            String msg = format("Failed to initialize ClazzCompiler '%s'." +
                    "A no arg constructor is not found.",
                    compilerClazz);
            System.out.println(msg);
            throw new IllegalArgumentException(msg, e);
        }

        return method;
    }
}
