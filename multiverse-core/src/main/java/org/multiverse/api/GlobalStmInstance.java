package org.multiverse.api;

import org.multiverse.utils.monitoring.ProfilePublisher;
import org.multiverse.utils.profiling.ProfileRepository;
import org.multiverse.utils.profiling.ProfilerAware;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;

/**
 * A singleton that can be used for easy access to the {@link org.multiverse.api.Stm} that is used globally. Once it has
 * been set, it should not be changed while running the system.
 * <p/>
 * Using the GlobalStm imposes some limitations (like 1 global stm instance that is used by everything) but makes the
 * system a lot easier to use. But if the GlobalStm should not be used, but a 'private' stm, you need to carry around
 * the stm reference yourself and just ignore this GlobalStm.
 * <p/>
 * The default implementation is the AlphaStm for now. It can be configured through setting the System property:
 * org.multiverse api GlobalStmInstance.factorymethod. This method should be a no arg static method that returns a
 * {@link Stm} instance.
 *
 * @author Peter Veentjer
 */
public final class GlobalStmInstance {

    private static final String KEY = GlobalStmInstance.class.getName() + ".factoryMethod";

    private static final String DEFAULT_FACTORY_METHOD = "org.multiverse.stms.alpha.AlphaStm.createFast";

    private static final Logger logger = Logger.getLogger(GlobalStmInstance.class.getName());

    private static volatile Stm instance;

    static {
        try {
            String factoryMethod = System.getProperty(KEY, DEFAULT_FACTORY_METHOD);
            logger.info(format("Initializing GlobalStmInstance using factoryMethod '%s'.", factoryMethod));
            try {
                Method method = getMethod(factoryMethod);
                instance = (Stm) method.invoke(null);
                logger.info(format("Successfully initialized GlobalStmInstance using factoryMethod '%s'.", factoryMethod));
            } catch (IllegalAccessException e) {
                String msg = format("Failed to initialize GlobalStmInstance through System property '%s' with value '%s'." +
                        "'%s' is not accessable (it should be public)').",
                        KEY, factoryMethod, factoryMethod);
                logger.severe(msg);
                throw new IllegalArgumentException(msg, e);
            } catch (ClassCastException e) {
                String msg = format("Failed to initialize GlobalStmInstance through System property '%s' with value '%s'." +
                        "'%s' is not accessable (it should be public)').",
                        KEY, factoryMethod, factoryMethod);
                logger.severe(msg);
                throw new IllegalArgumentException(msg, e);
            } catch (InvocationTargetException e) {
                String msg = format("Failed to initialize GlobalStmInstance through System property '%s' with value '%s'." +
                        "'%s' failed to be invoked.",
                        KEY, factoryMethod, factoryMethod);
                logger.severe(msg);
                throw new IllegalArgumentException(msg, e);
            }

            // XXX: think about a better place for this
            if (instance instanceof ProfilerAware) {
                ProfileRepository profiler = ((ProfilerAware) instance).getProfiler();
                if (profiler != null) {
                    ProfilePublisher publisher = new ProfilePublisher(profiler.getCollator());
                    String mBeanName = "uncomment following for class circularity error"; //JmxUtils.registerMBean(publisher);
                    logger.info(format("Successfully registered '%s' as an MBean under name '%s'",
                            publisher, mBeanName));
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Error e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static Method getMethod(String factoryMethod) {
        int indexOf = factoryMethod.lastIndexOf('.');
        if (indexOf == -1) {
            String msg = format(
                    "Failed to initialize GlobalStmInstance through System property '%s' with value '%s'. " +
                            "It is not a valid factory method, it should be something like 'com.SomeStm.createSomeStm').",
                    KEY, factoryMethod);
            logger.info(msg);
            throw new IllegalArgumentException();
        }

        String className = factoryMethod.substring(0, indexOf);
        Class clazz;
        try {
            clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            String msg = format("Failed to initialize GlobalStmInstance through System property '%s' with value '%s'." +
                    "'%s' is not an existing class (it can't be found using the Thread.currentThread.getContextClassLoader).",
                    KEY, className, factoryMethod);
            logger.info(msg);
            throw new IllegalArgumentException(msg, e);
        }

        String methodName = factoryMethod.substring(indexOf + 1);
        if (methodName.length() == 0) {
            String msg = format("Failed to initialize GlobalStmInstance through System property '%s' with value '%s'." +
                    "The factory method is completely missing, it should be something like %s.createSomeStm.",
                    KEY, className, factoryMethod);
            logger.info(msg);
            throw new IllegalArgumentException(msg);
        }

        Method method;
        try {
            method = clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            String msg = format("Failed to initialize GlobalStmInstance through System property '%s' with value '%s'." +
                    "The factory method is not found. Remember that it should not have any arguments.",
                    KEY, factoryMethod);
            logger.info(msg);
            throw new IllegalArgumentException(msg, e);
        }

        if (!isStatic(method.getModifiers())) {
            String msg = format("Failed to initialize GlobalStmInstance through System property '%s' with value '%s'." +
                    "The factory method is not static.",
                    KEY, factoryMethod);
            logger.info(msg);
            throw new IllegalArgumentException(msg);
        }

        return method;
    }

    /**
     * Gets the global {@link Stm} instance. The returned value will never be null.
     *
     * @return the global STM instance.
     */
    public static Stm getGlobalStmInstance() {
        return instance;
    }

    /**
     * Sets the global Stm instance.
     *
     * @param newInstance the instance to set.
     * @throws NullPointerException if newInstance is null. No need to allow for an illegal reference.
     */
    public static void setGlobalStmInstance(Stm newInstance) {
        if (newInstance == null) {
            throw new NullPointerException();
        }

        doSomeLogging();
        instance = newInstance;
    }

    private static void doSomeLogging() {
        Stm oldInstance = instance;
        if (oldInstance != null && oldInstance.getVersion() > 0) {
            logger.warning("Replacing a used global STM instance. The old STM instance already has commits " +
                    "and this could lead to strange concurrency bugs. Normally this situation should be prevented. " +
                    "The safest thing to do is to drop all transactional objects that have been created while " +
                    "using that STM.");
        } else {
            logger.info("Replacing unused GlobalStmInstance");
        }
    }

    //we don't want instances.

    private GlobalStmInstance() {
    }
}
