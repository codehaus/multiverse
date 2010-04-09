package org.multiverse.instrumentation;

import org.multiverse.instrumentation.asm.AsmUtils;

import java.io.File;

/**
 * A Filer that decorates another filer with the ability to dump the classfile
 * to some directory.
 *
 * @author Peter Veentjer
 */
public class DumpingFiler implements Filer {

    private final Filer filer;
    private final File dumpDirectory;

    public DumpingFiler(Filer filer, File dumpDirectory) {
        if (filer == null || dumpDirectory == null) {
            throw new NullPointerException();
        }

        this.dumpDirectory = dumpDirectory;
        this.filer = filer;
    }

    @Override
    public void createClassFile(Clazz clazz) {
        File file = new File(dumpDirectory, clazz.getName() + ".class");
        AsmUtils.writeToFile(file, clazz.getBytecode());

        filer.createClassFile(clazz);
    }
}
