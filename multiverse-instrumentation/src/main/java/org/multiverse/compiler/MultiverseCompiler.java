package org.multiverse.compiler;

import org.apache.commons.cli.*;
import org.multiverse.instrumentation.compiler.ClazzCompiler;

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

    public static void main(String[] args) throws ParseException {
        MultiverseCompiler multiverseCompiler = new MultiverseCompiler();
        multiverseCompiler.run(args);
    }

    private void run(String[] args) throws ParseException {
        System.out.println("Multiverse Compiler");

        Options options = createOptions();
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        ClazzCompiler compiler = createClazzCompiler("org.multiverse.alpha.instrumentation.AlphaClazzCompiler");
        return;
    }


    public static Options createOptions() {

        // createReference Options object
        Options options = new Options();
        //options.addOption(new Option())

        // add t option
        options.addOption("compiler", true, "display current time");
        options.addOption("verbose", false, "Outputting verbose logging information");
        options.addOption("dumpbytecode", false, "Outputting transformed class files and intermediate states for debugging purposes");

        return options;
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
