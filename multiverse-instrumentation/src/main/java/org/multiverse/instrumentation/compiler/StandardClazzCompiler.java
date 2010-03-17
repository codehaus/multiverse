package org.multiverse.instrumentation.compiler;

import org.multiverse.instrumentation.Filer;
import org.multiverse.instrumentation.Resolver;
import org.multiverse.instrumentation.metadata.MetadataRepository;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * @author Peter Veentjer
 */
public class StandardClazzCompiler implements ClazzCompiler {

    private final static Logger logger = Logger.getLogger(StandardClazzCompiler.class.getName());

    private final MetadataRepository metadataRepository = new MetadataRepository();
    private final List<CompilePhase> compileSteps = new LinkedList<CompilePhase>();
    private boolean dumpClassFiles;
    private Resolver resolver;
    private Filer filer;
    private boolean dumpBytecode;
    private File dumpDir;
    private String name;

    public StandardClazzCompiler(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
    }

    protected final void add(CompilePhase phase) {
        if (compileSteps == null) {
            throw new NullPointerException();
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Adding CompilerPhase: " + phase.getName());
        }
        compileSteps.add(phase);
    }

    @Override
    public void setDumpBytecode(boolean dumpBytecode) {
        this.dumpBytecode = dumpBytecode;
    }

    @Override
    public void setDumpDirectory(File dumpDirectory) {
        this.dumpDir = dumpDirectory;
    }

    @Override
    public void setFiler(Filer filer) {
        if (filer == null) {
            throw new NullPointerException();
        }
        this.filer = filer;
    }

    @Override
    public void setResolver(Resolver resolver) {
        if (resolver == null) {
            throw new NullPointerException();
        }
        this.resolver = resolver;
    }

    @Override
    public Clazz process(Clazz clazz) {
        if (clazz.getClassLoader() == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.info(format("Ignoring compilation of class '%s' because it is a system class", clazz.getName()));
            }
            return clazz;
        }

        if (isExcluded(clazz.getName())) {
            if (logger.isLoggable(Level.FINE)) {
                logger.info(format("Ignoring compilation of class '%s' because it is excluded", clazz.getName()));
            }
            return clazz;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.info(format("Starting compilation of class '%s'", clazz.getName()));
        }

        Environment env = new EnvironmentImpl();
        Clazz newClazz = clazz;
        for (CompilePhase step : compileSteps) {
            newClazz = step.compile(env, newClazz);
        }

        if (logger.isLoggable(Level.FINE)) {
            if (clazz == newClazz) {
                logger.info(format("Finished compilation of class '%s' (class was not modified)", clazz.getName()));
            } else {
                logger.info(format("Finished compilation of class '%s'", clazz.getName()));
            }
        }

        return newClazz;
    }

    //todo: ugly hack.

    private static boolean isExcluded(String className) {
        return className.startsWith("java/") ||
                className.startsWith("javax/") ||
                className.startsWith("org/mockito") ||
                className.startsWith("com/jprofiler/") ||
                className.startsWith("org/junit") ||
                className.startsWith("sun/") ||
                className.startsWith("com/sun") ||
                className.startsWith("org/apache/") ||
                className.startsWith("org/hamcrest/") ||
                className.startsWith("com/intellij") ||
                className.startsWith("org/eclipse") ||
                className.startsWith("junit/");
    }

    class EnvironmentImpl implements Environment {
        @Override
        public boolean dumpClassFiles() {
            return dumpClassFiles;
        }

        @Override
        public Filer getFiler() {
            return filer;
        }

        @Override
        public boolean isVerbose() {
            return true;
        }

        @Override
        public File getDumpDirectory() {
            return dumpDir;
        }

        @Override
        public MetadataRepository getMetadataRepository() {
            return metadataRepository;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
