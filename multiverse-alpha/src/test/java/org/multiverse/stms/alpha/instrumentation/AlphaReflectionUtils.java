package org.multiverse.stms.alpha.instrumentation;

import java.lang.reflect.Field;

/**
 * @author Peter Veentjer
 */
public class AlphaReflectionUtils {

    private AlphaReflectionUtils() {
    }

    public static boolean existsField(Class txObjectClass, String fieldName) {
        for (Field field : txObjectClass.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }

        return false;
    }

    public static Field getField(Class txObjectClass, String fieldName) throws NoSuchFieldException {
        for (Field field : txObjectClass.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }

        throw new NoSuchFieldException();
    }

    public static boolean existsTranlocalField(Class txObjectClass, String fieldName) {
        Class tranlocalClass = getTranlocalClass(txObjectClass);
        return existsField(tranlocalClass, fieldName);
    }

    public static boolean existsTranlocalSnapshotField(Class txObjectClass, String fieldName) {
        Class snapshotClass = getTranlocalSnapshotClass(txObjectClass);
        return existsField( snapshotClass, fieldName);
    }

    public static Field getTranlocalField(Class txObjectClass, String fieldName) {
        try {
            Class clazz = getTranlocalClass(txObjectClass);
            return getField(clazz, fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getTranlocalSnapshotField(Class txObjectClass, String fieldName) {
        try {
            Class clazz = getTranlocalSnapshotClass(txObjectClass);
            return getField(clazz, fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean existsTranlocalClass(Class txObjectClass) {
        String tranlocalClassName = txObjectClass.getName() + "__Tranlocal";
        try {
            txObjectClass.getClassLoader().loadClass(tranlocalClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean existsTranlocalSnapshotClass(Class txObjectClass) {
        String tranlocalClassName = txObjectClass.getName() + "__TranlocalSnapshot";
        try {
            txObjectClass.getClassLoader().loadClass(tranlocalClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Class getTranlocalClass(Class txObjectClass) {
        String tranlocalClassName = txObjectClass.getName() + "__Tranlocal";
        try {
            return txObjectClass.getClassLoader().loadClass(tranlocalClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class getTranlocalSnapshotClass(Class txObjectClass) {
        String snapshotClass = txObjectClass.getName() + "__TranlocalSnapshot";
        try {
            return txObjectClass.getClassLoader().loadClass(snapshotClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
