package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.MultiverseConstants;
import org.multiverse.stms.alpha.instrumentation.asm.*;
import org.multiverse.stms.alpha.instrumentation.metadata.ClassMetadata;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;
import static org.multiverse.utils.instrumentation.ClassUtils.defineClass;

/**
 * The JavaAgent that enhances classfiles specifically for the multiverse alpha stm engine.
 *
 * @author Peter Veentjer.
 */
public class MultiverseJavaAgent {

    public final static boolean DUMP_BYTECODE = parseBoolean(
            getProperty(MultiverseJavaAgent.class.getName() + ".dumpBytecode", "false"));

    public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
        printInfo();
        registerTransformers(inst);
    }

    private static void registerTransformers(Instrumentation inst) {
        //it is very important that the order of these transformers is not
        //changed, unless you really know what you are doing.
        inst.addTransformer(new InitClassFileTransformer());
        inst.addTransformer(new JSRInlineClassFileTransformer());
        inst.addTransformer(new FieldGranularityClassFileTransformer());
        inst.addTransformer(new TranlocalClassFileTransformer());
        inst.addTransformer(new TranlocalSnapshotClassFileTransformer());
        inst.addTransformer(new NonTransactionalObjectFieldAccessClassFileTransformer());
        inst.addTransformer(new TransactionalObjectClassFileTransformer());
        inst.addTransformer(new TransactionalMethodClassFileTransformer());
        //inst.addTransformer(new VerifyingClassFileTransformer());
    }

    private static void printInfo() {
        System.out.println("Starting Multiverse JavaAgent");

        if (MultiverseConstants.___SANITY_CHECKS_ENABLED) {
            System.out.println("Sanity checks are enabled.");
        }

        if (DUMP_BYTECODE) {
            System.out.printf("Bytecode will be dumped to '%s'\n", getTmpDir());
        }
    }

    public static class VerifyingClassFileTransformer extends AbstractClassFileTransformer {

        public VerifyingClassFileTransformer() {
            super("VerifyingClassFileTransformer");
        }

        @Override
        public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws IllegalClassFormatException {

            AsmUtils.verify(classfileBuffer);
            return null;
        }
    }

    /**
     * A ClassFileTransformer that does nothing (dirty hack.. needs to be fixed in the future)
     */
    public static class InitClassFileTransformer extends AbstractClassFileTransformer {

        public InitClassFileTransformer() {
            super("InitClassFileTransformer");
        }

        @Override
        public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] bytecode)
                throws IllegalClassFormatException {
            if (DUMP_BYTECODE) {
                writeToFileInTmpDirectory(className + "__Original.class", bytecode);
            }

            return null;
        }
    }

    public static class JSRInlineClassFileTransformer extends AbstractClassFileTransformer {

        public JSRInlineClassFileTransformer() {
            super("JSRInlineTransformer");
        }

        @Override
        public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] bytecode)
                throws IllegalClassFormatException {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            JSRInlineClassAdapter inlineAdapter = new JSRInlineClassAdapter(writer);
            ClassReader reader = new ClassReader(bytecode);
            reader.accept(inlineAdapter, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        }
    }

    public static class NonTransactionalObjectFieldAccessClassFileTransformer extends AbstractClassFileTransformer {

        public NonTransactionalObjectFieldAccessClassFileTransformer() {
            super("NonTransactionalMethodFieldAccessTransformer");
        }

        @Override
        public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] bytecode)
                throws IllegalClassFormatException {

            ClassNode original = loadAsClassNode(bytecode);
            NonTransactionalMethodFieldAccessTransformer transformer = new NonTransactionalMethodFieldAccessTransformer(
                    loader, original);
            ClassNode transformed = transformer.transform();
            if (transformed == null) {
                return null;
            }

            byte[] transformedBytecode = toBytecode(transformed);
            if (DUMP_BYTECODE) {
                writeToFileInTmpDirectory(transformed.name + "__NonTransactionalFieldAccess.class", transformedBytecode);
            }

            return transformedBytecode;
        }
    }

    public static class TransactionalObjectClassFileTransformer extends AbstractClassFileTransformer {

        public TransactionalObjectClassFileTransformer() {
            super("TransactionalObjectTransformer");
        }

        @Override
        public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] bytecode)
                throws IllegalClassFormatException {

            ClassMetadata classMetadata = metadataRepository.getClassMetadata(loader, className);
            if (classMetadata.isRealTransactionalObject()) {
                ClassNode mixin = loadAsClassNode(DefaultTxObjectMixin.class);
                ClassNode original = loadAsClassNode(bytecode);
                TransactionalObjectTransformer transformer = new TransactionalObjectTransformer(loader, original, mixin);
                ClassNode result = transformer.transform();
                byte[] resultCode = toBytecode(result);
                if (DUMP_BYTECODE) {
                    writeToFileInTmpDirectory(result.name + "__TransactionalObject.class", resultCode);
                }
                return resultCode;
            }

            return null;
        }
    }

    public static class FieldGranularityClassFileTransformer extends AbstractClassFileTransformer {

        public FieldGranularityClassFileTransformer() {
            super("FieldGranularityClassFileTransformer");
        }

        @Override
        public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] bytecode)
                throws IllegalClassFormatException {

            ClassNode original = loadAsClassNode(bytecode);

            FieldGranularityTransformer transformer = new FieldGranularityTransformer(loader, original);

            ClassNode transformed = transformer.transform();

            byte[] resultCode = toBytecode(transformed);
            if (DUMP_BYTECODE) {
                writeToFileInTmpDirectory(transformed.name + "__FieldGranularity.class", resultCode);
            }
            return resultCode;
        }

    }

    public static class TranlocalSnapshotClassFileTransformer extends AbstractClassFileTransformer {

        public TranlocalSnapshotClassFileTransformer() {
            super("TranslocalSnapshotFactory");
        }

        @Override
        public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] bytecode)
                throws IllegalClassFormatException {

            ClassMetadata classMetadata = metadataRepository.getClassMetadata(loader, className);
            if (classMetadata.isRealTransactionalObject()) {
                ClassNode original = loadAsClassNode(bytecode);
                TranlocalSnapshotFactory factory = new TranlocalSnapshotFactory(loader, original);
                ClassNode result = factory.create();
                byte[] resultBytecode = toBytecode(result);
                if (DUMP_BYTECODE) {
                    writeToFileInTmpDirectory(result.name + ".class", resultBytecode);
                }
                defineClass(loader, result.name, resultBytecode);
            }

            return null;
        }
    }

    public static class TranlocalClassFileTransformer extends AbstractClassFileTransformer {

        public TranlocalClassFileTransformer() {
            super("TranslocalFactory");
        }

        @Override
        public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] bytecode)
                throws IllegalClassFormatException {

            ClassMetadata classMetadata = metadataRepository.getClassMetadata(loader, className);
            if (classMetadata.isRealTransactionalObject()) {
                ClassNode original = loadAsClassNode(bytecode);
                TranlocalFactory transformer = new TranlocalFactory(loader, original);
                ClassNode result = transformer.create();

                byte[] resultBytecode = toBytecode(result);
                if (DUMP_BYTECODE) {
                    writeToFileInTmpDirectory(result.name + ".class", resultBytecode);
                }
                defineClass(loader, result.name, resultBytecode);
            }

            return null;
        }
    }

    public static class TransactionalMethodClassFileTransformer extends AbstractClassFileTransformer {

        public TransactionalMethodClassFileTransformer() {
            super("TransactionalClassMethodTransformer");
        }

        @Override
        public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] bytecode)
                throws IllegalClassFormatException {


            ClassMetadata classMetadata = metadataRepository.getClassMetadata(loader, className);
            if (classMetadata.hasTransactionalMethods()) {
                boolean restore = InsnList.check;
                InsnList.check = true;
                try {

                    ClassNode original = loadAsClassNode(bytecode);
                    ClassNode donor = loadAsClassNode(TransactionLogicDonor.class);
                    writeToFileInTmpDirectory(donor.name + ".class", toBytecode(donor));

                    ClassNode result;
                    if (classMetadata.isInterface()) {
                        TransactionalInterfaceMethodTransformer transformer = new TransactionalInterfaceMethodTransformer(
                                loader, original);
                        result = transformer.transform();
                    } else {
                        TransactionalClassMethodTransformer transformer = new TransactionalClassMethodTransformer(
                                loader, original, donor);
                        result = transformer.transform();
                    }

                    if (result == null) {
                        return null;
                    }

                    byte[] resultBytecode = toBytecode(result);

                    if (DUMP_BYTECODE) {
                        writeToFileInTmpDirectory(result.name + "__WithTransaction.class", resultBytecode);
                    }

                    return resultBytecode;
                } finally {
                    InsnList.check = restore;
                }
            }

            return null;
        }
    }
}