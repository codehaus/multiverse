package org.multiverse.stms.alpha.instrumentation;

import java.lang.reflect.Field;

/**
 * @author Peter Veentjer
 */
public class AlphaReflectionUtils {

    private AlphaReflectionUtils() {
    }

    public static boolean existsField(Class atomicObjectClass, String fieldName) {
        for (Field field : atomicObjectClass.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }

        return false;
    }

    public static Field getField(Class atomicObjectClass, String fieldName) throws NoSuchFieldException {
        for (Field field : atomicObjectClass.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }

        throw new NoSuchFieldException();
    }

    public static boolean existsTranlocalField(Class atomicObjectClass, String fieldName) {
        Class tranlocalClass = getTranlocalClass(atomicObjectClass);
        return existsField(tranlocalClass, fieldName);
    }

    public static boolean existsTranlocalSnapshotField(Class atomicObjectClass, String fieldName) {
        Class snapshotClass = getTranlocalSnapshotClass(atomicObjectClass);
        return existsField( snapshotClass, fieldName);
    }

    public static Field getTranlocalField(Class atomicObjectClass, String fieldName) {
        try {
            Class clazz = getTranlocalClass(atomicObjectClass);
            return getField(clazz, fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getTranlocalSnapshotField(Class atomicObjectClass, String fieldName) {
        try {
            Class clazz = getTranlocalSnapshotClass(atomicObjectClass);
            return getField(clazz, fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean existsTranlocalClass(Class atomicObjectClass) {
        String tranlocalClassName = atomicObjectClass.getName() + "__Tranlocal";
        try {
            atomicObjectClass.getClassLoader().loadClass(tranlocalClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean existsTranlocalSnapshotClass(Class atomicObjectClass) {
        String tranlocalClassName = atomicObjectClass.getName() + "__TranlocalSnapshot";
        try {
            atomicObjectClass.getClassLoader().loadClass(tranlocalClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Class getTranlocalClass(Class atomicObjectClass) {
        String tranlocalClassName = atomicObjectClass.getName() + "__Tranlocal";
        try {
            return atomicObjectClass.getClassLoader().loadClass(tranlocalClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class getTranlocalSnapshotClass(Class atomicObjectClass) {
        String snapshotClass = atomicObjectClass.getName() + "__TranlocalSnapshot";
        try {
            return atomicObjectClass.getClassLoader().loadClass(snapshotClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
