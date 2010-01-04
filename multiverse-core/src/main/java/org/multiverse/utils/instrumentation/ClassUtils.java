package org.multiverse.utils.instrumentation;

import static java.lang.String.format;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A utility class for {@link Class}
 *
 * @author Peter Veentjer.
 */
public class ClassUtils {

    //we don't want instances.
    private ClassUtils() {
    }

    private final static Method defineClassMethod;

    static{
        try {
            defineClassMethod = ClassLoader.class.getDeclaredMethod(
                    "defineClass",
                    String.class,
                    byte[].class,
                    int.class,
                    int.class);
            defineClassMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class defineClass(ClassLoader classLoader, String className, byte[] bytecode) {
        //System.out.println("definingClass: "+className);
        try {
            return (Class) defineClassMethod.invoke(
                    classLoader,
                    className.replace("/", "."),
                    bytecode,
                    0,
                    bytecode.length);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(format("A problem occurred while defining class '%s'", className),e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(format("A problem occurred while defining class '%s'", className),e);
        }
    }

    public static void printClassLoaderInfo(Class clazz) {
        System.out.println(getClassLoaderInfo(clazz));
    }

    public static String getClassLoaderInfo(Class clazz) {
        StringBuffer sb = new StringBuffer();
        sb.append(format("Classloader info for class: %s\n", clazz.getName()));
        ClassLoader classLoader = clazz.getClassLoader();

        while (classLoader != null) {
            sb.append(format("\t%s extends %s\n",
                    classLoader.getClass().getName(),
                    classLoader.getClass().getSuperclass().getName()));
            classLoader = classLoader.getParent();
        }

        sb.append("\tBootstrap ClassLoader\n");

        return sb.toString();
    }

    public static void printClassLoaderInfo(ClassLoader classLoader) {
        System.out.println(getClassLoaderInfo(classLoader));
    }

    public static String getClassLoaderInfo(ClassLoader classLoader) {
        StringBuffer sb = new StringBuffer();
        sb.append(format("ClassLoader info for classLoader: %s\n", classLoader));
        while (classLoader != null) {
            sb.append(format("\t%s extends %s\n",
                    classLoader.getClass().getName(),
                    classLoader.getClass().getSuperclass().getName()));
            classLoader = classLoader.getParent();
        }

        sb.append("\tBootstrap ClassLoader\n");

        return sb.toString();
    }

    public static void printClassInfo(Class clazz) {
        System.out.println(getClassInfo(clazz));
    }

    public static String getClassInfo(Class clazz) {
        StringBuffer sb = new StringBuffer();
        if (clazz.isInterface()) {
            sb.append(format("Info for interface: %s\n", clazz.getName()));
        } else {
            sb.append(format("Info for class: %s\n", clazz.getName()));
        }
        sb.append(format("superclass: %s\n", clazz.getSuperclass()));
        sb.append("interfaces:\n");
        for (Class interfaze : clazz.getInterfaces()) {
            sb.append(format("\t%s\n", interfaze.getName()));
        }

        sb.append("fields:\n");
        for (Field field : clazz.getDeclaredFields()) {
            sb.append(format("\t%s\n", field));
        }

        sb.append("methods");
        for (Method method : clazz.getMethods()) {
            sb.append(format("\t%s\n", method));
        }
        sb.append(getClassLoaderInfo(clazz));
        return sb.toString();
    }
}
