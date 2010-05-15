package org.multiverse.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be placed on constructor. There is a lot of overlap with the {@link TransactionalMethod}
 * annotation, but the big difference is that transactions on constructors can't be retried. So everything related
 * to retrying has been removed.
 * <p/>
 * The reason why this annotation exists is that it is very hard to add the instrumentation in a constructor so that
 * it can be retried. It has to do with a bytecode verification that treats an uninitialized objects as a different
 * type than an initialized one.
 *
 * @author Peter Veentjer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR})
public @interface TransactionalConstructor {

    /**
     * If the TransactionalConstructor should be readonly or an update.
     *
     * @return
     */
    boolean readonly() default false;

    /**
     * If the transaction automatically should track reads. This behavior is not only useful to reduce
     * read conflicts, it also is needed for blocking transactions and prevention of the write skew problem.
     *
     * @return true if it should do automatic read tracking, false otherwise.
     */
    boolean trackReads() default true;

    /**
     * If the write skew problem is allowed to happen. If set to false, the readtracking also has to
     * be set to true otherwise you will get an Exception.
     *
     * @return true if the writeSkew problem is allowed.
     */
    boolean writeSkew() default true;

    LogLevel logLevel() default LogLevel.none;
}
