package org.multiverse.compiler;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.multiverse.instrumentation.Clazz;
import org.multiverse.instrumentation.FileSystemFiler;
import org.multiverse.instrumentation.Instrumentor;
import org.multiverse.instrumentation.SystemOutLog;
import org.multiverse.instrumentation.asm.AsmUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.lang.String.format;

/**
 * The MultiverseCompiler is responsible for transforming class files. It is a general purpose
 * compiler since it doesn't do any compiling itself, but forwards everything a
 * {@link org.multiverse.instrumentation.Instrumentor}  The advantage is that not yet another MultiverseCompiler needs to
 * be written; so shared plumbing.
 * <p/>
 * Another advange is that the same Instrumentor can be used for the MultiverseJavaAgent
 * and the MultiverseCompiler.
 *
 * @author Peter Veentjer
 */
public final class MultiverseCompiler {

    public static void main(String[] args) {
        MultiverseCompilerArguments cli = createCli(args);
        MultiverseCompiler multiverseCompiler = new MultiverseCompiler();
        multiverseCompiler.run(cli);
    }

    private ClassLoader compilerClassLoader;

    private void run(MultiverseCompilerArguments cli) {
        System.out.println("Initializing Multiverse Compiler");

        Instrumentor instrumentor = createInstrumentor(cli.instrumentorName);
        File targetDirectory = new File(cli.targetDirectory);
        compilerClassLoader = new MyClassLoader(targetDirectory, MultiverseCompiler.class.getClassLoader());

        if (!targetDirectory.isDirectory()) {
            System.out.printf("Target directory '%s' is not found, skipping instrumentation\n", targetDirectory);
            return;
        }

        if (cli.dumpBytecode) {
            System.out.println("Bytecode is dumped for debugging purposes");
            instrumentor.setDumpBytecode(true);
        }

        if (cli.verbose) {
            instrumentor.setLog(new SystemOutLog());
        }

        System.out.printf("Using org.multiverse.instrumentation.Instrumentor %s-%s\n",
                instrumentor.getInstrumentorName(),
                instrumentor.getInstrumentorVersion());

        instrumentor.setFiler(new FileSystemFiler(targetDirectory));

        System.out.printf("Transforming classes in targetDirectory %s\n", targetDirectory);

        applyRecursive(targetDirectory, instrumentor);
    }

    private static MultiverseCompilerArguments createCli(String[] args) {
        CmdLineParser parser = null;
        try {
            MultiverseCompilerArguments cli = new MultiverseCompilerArguments();
            parser = new CmdLineParser(cli);
            parser.parseArgument(args);
            return cli;
        } catch (CmdLineException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.err.println("java -jar myprogram.jar [options...] compilername target");
            if (parser != null) {
                parser.printUsage(System.out);
            }
            System.exit(-1);
            return null;
        }
    }

    public void applyRecursive(File directory, Instrumentor clazzCompiler) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                applyRecursive(file, clazzCompiler);
            } else if (file.getName().endsWith(".class")) {
                transform(file, clazzCompiler);
            }
        }
    }

    private void transform(File file, Instrumentor clazzCompiler) {
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
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(clazz.getBytecode());
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Instrumentor createInstrumentor(String compilerClassName) {
        System.out.println(format("Multiverse Compiler: using Instrumentor '%s'", compilerClassName));

        Constructor constructor = getMethod(compilerClassName);
        try {
            return (Instrumentor) constructor.newInstance();
        } catch (IllegalAccessException e) {
            String msg = format("Failed to initialize Instrumentor '%s'." +
                    "The constructor is not accessable.",
                    compilerClassName);
            throw new IllegalArgumentException(msg, e);
        } catch (InvocationTargetException e) {
            String msg = format("Failed to initialize Instrumentor '%s'." +
                    "The constructor threw an exception.",
                    compilerClassName);
            System.out.println(msg);
            throw new IllegalArgumentException(msg, e);
        } catch (InstantiationException e) {
            String msg = format("Failed to initialize Instrumentor '%s'." +
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
            String msg = format("Failed to initialize Instrumentor '%s'." +
                    "Is not an existing class (it can't be found using the Thread.currentThread.getContextClassLoader).",
                    className);
            System.out.println(msg);
            throw new IllegalArgumentException(msg, e);
        }

        if (!Instrumentor.class.isAssignableFrom(compilerClazz)) {
            String msg = format("Failed to initialize Instrumentor '%s'." +
                    "Is not an subclass of org.multiverse.compiler.Instrumentor).",
                    className);
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        }

        Constructor method;
        try {
            method = compilerClazz.getConstructor();
        } catch (NoSuchMethodException e) {
            String msg = format("Failed to initialize Instrumentor '%s'." +
                    "A no arg constructor is not found.",
                    compilerClazz);
            System.out.println(msg);
            throw new IllegalArgumentException(msg, e);
        }

        return method;
    }
}
