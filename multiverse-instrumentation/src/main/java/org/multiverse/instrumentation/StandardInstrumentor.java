package org.multiverse.instrumentation;

import org.multiverse.instrumentation.asm.AsmUtils;
import org.multiverse.instrumentation.metadata.MetadataRepository;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Peter Veentjer
 */
public class StandardInstrumentor implements Instrumentor {

    private final MetadataRepository metadataRepository = new MetadataRepository();
    private final List<InstrumentationPhase> phases = new LinkedList<InstrumentationPhase>();
    private final String compilerName;
    private final String compilerVersion;
    private Filer filer;
    private boolean dumpBytecode;
    private File dumpDir = new File(System.getProperty("java.io.tmpdir"));

    private InstrumenterLogger log = new NullInstrumenterLogger();
    private final String stmName;
    private boolean optimize = false;
    private String excluded = "";
    private String included = "";

    public StandardInstrumentor(String compilerName, String compilerVersion, String stmName) {
        if (compilerName == null || compilerVersion == null || stmName == null) {
            throw new NullPointerException();
        }
        this.compilerName = compilerName;
        this.compilerVersion = compilerVersion;
        this.stmName = stmName;


        exclude("ch.qos.");
        exclude("com.jprofiler.");
        exclude("com.ibm.");
        exclude("com.intellij.");
        exclude("com.sun.");
        exclude("java.");
        exclude("org.apache.");
        exclude("org.eclipse.");
        exclude("org.gradle.");
        exclude("org.hamcrest.");
        exclude("org.jruby.");
        exclude("org.junit.");
        exclude("org.kohsuke.");
        exclude("org.junit.");
        exclude("org.mockito.");
        exclude("org.multiverse.repackaged.");
        exclude("org.objectweb.asm.");
        exclude("org.slf4j.");
        exclude("sun.");
    }

    protected final void add(InstrumentationPhase phase) {
        if (phases == null) {
            throw new NullPointerException();
        }

        phases.add(phase);
    }

    @Override
    public String getInstrumentorName() {
        return compilerName;
    }

    @Override
    public String getInstrumentorVersion() {
        return compilerVersion;
    }

    @Override
    public String getStmName() {
        return stmName;
    }

    @Override
    public String getExcluded() {
        return excluded;
    }

    @Override
    public String getIncluded() {
        return included;
    }

    @Override
    public void exclude(String pattern) {
        if (pattern == null) {
            throw new NullPointerException();
        }

        if (pattern.length() == 0) {
            return;
        }

        pattern = removeTrailingAndPrecedingSemicolons(pattern);

        if (excluded.length() == 0) {
            excluded = pattern;
        } else {
            excluded = excluded + ";" + pattern;
        }
    }

    public String removeTrailingAndPrecedingSemicolons(String s) {
        return removePreceding(removeTrailing(s));
    }

    private String removeTrailing(String s) {
        if (s.endsWith(";")) {
            return removeTrailing(s.substring(0, s.length() - 1));
        }

        return s;
    }

    private String removePreceding(String s) {
        if (s.startsWith(";")) {
            return removeTrailing(s.substring(1, s.length()));
        }

        return s;
    }

    @Override
    public void include(String pattern) {
        if (pattern == null) {
            throw new NullPointerException();
        }

        pattern = removeTrailingAndPrecedingSemicolons(pattern);

        if (pattern.length() == 0) {
            return;
        }

        if (included.length() == 0) {
            included = pattern;
        } else {
            included = included + ";" + pattern;
        }
    }

    @Override
    public void setLog(InstrumenterLogger log) {
        if (log == null) {
            this.log = new NullInstrumenterLogger();
        } else {
            this.log = log;
        }
    }

    @Override
    public void setDumpBytecode(boolean dumpBytecode) {
        this.dumpBytecode = dumpBytecode;
    }

    @Override
    public void setDumpDirectory(File dumpDirectory) {
        if (dumpDirectory == null) {
            throw new NullPointerException();
        }

        this.dumpDir = dumpDirectory;
    }

    @Override
    public File getDumpDirectory() {
        return dumpDir;
    }

    @Override
    public void setFiler(Filer filer) {
        if (filer == null) {
            throw new NullPointerException();
        }
        this.filer = filer;
    }

    @Override
    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    @Override
    public Clazz process(Clazz originalClazz) {
        if (originalClazz.getClassLoader() == null) {
            log.important("Multiverse: Ignoring %s because it is a System class", originalClazz.getName());
            return originalClazz;
        }

        if (included.length() > 0 && !contains(included, originalClazz.getInternalName())) {
            log.lessImportant("Multiverse: Ignoring %s, because it is not included", originalClazz.getName());
            return originalClazz;
        }

        if (contains(excluded, originalClazz.getInternalName())) {
            log.important("Multiverse: Ignoring %s, because it is excluded", originalClazz.getName());
            return originalClazz;
        }

        log.important("Multiverse: Instrumenting %s", originalClazz.getName());

        Environment env = new EnvironmentImpl();
        Clazz beforeClazz = originalClazz;
        for (InstrumentationPhase phase : phases) {
            Clazz afterClazz = phase.instrument(env, beforeClazz);
            if (afterClazz == null) {
                break;
            }
            dump(phase, beforeClazz, afterClazz);
            beforeClazz = afterClazz;
        }

        if (originalClazz == beforeClazz) {
            log.lessImportant("Multiverse: Finished instrumenting %s (class was not modified)", originalClazz.getName());
        } else {
            log.lessImportant("Multiverse: Finished instrumenting of class %s", originalClazz.getName());
        }

        return beforeClazz;
    }

    private void dump(InstrumentationPhase step, Clazz beforeClazz, Clazz afterClazz) {
        if (!dumpBytecode
                || afterClazz == beforeClazz) {
            return;
        }

        File begin = new File(dumpDir, beforeClazz.getName() + "_" + step.getName() + "_before.class");
        AsmUtils.writeToFile(begin, beforeClazz.getBytecode());

        File end = new File(dumpDir, beforeClazz.getName() + "_" + step.getName() + "_after.class");
        AsmUtils.writeToFile(end, afterClazz.getBytecode());
    }

    private boolean contains(String all, String name) {
        StringTokenizer tokenizer = new StringTokenizer(all, ";");

        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            if (name.startsWith(token)) {
                return true;
            }
        }

        return false;

    }

    class EnvironmentImpl implements Environment {

        @Override
        public boolean optimize() {
            return optimize;
        }

        @Override
        public boolean dumpBytecode() {
            return dumpBytecode;
        }

        @Override
        public Filer getFiler() {
            if (dumpBytecode) {
                return new DumpingFiler(filer, dumpDir);
            } else {
                return filer;
            }
        }

        @Override
        public InstrumenterLogger getLog() {
            return log;
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
        return compilerName + "-" + compilerVersion;
    }
}
