package org.multiverse.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

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

    boolean readonly() default false;

    /**
     * If the transaction automatically should track reads. This behavior is not only useful to reduce
     * read conflicts, it also is needed for blocking transactions and prevention of the write skew problem.
     *
     * @return true if it should do automatic read tracking, false otherwise.
     */
    boolean automaticReadTracking() default true;

    /**
     * If the write skew problem is allowed to happen. If set to false, the automaticReadTracking also has to
     * be set to true otherwise you will get an Exception.
     *
     * @return true if the writeSkew problem is allowed.
     */
    boolean allowWriteSkewProblem() default true;

    /**
     * The timeout that is used to limit the time a transaction blocks.
     * <p/>
     * Value smaller than zero indicates that there is no limit on the timeout.
     *
     * @return the timeout.
     */
    long timeout() default -1;

    /**
     * The TimeUnit for the timeout argument.
     *
     * @return the TimeUnit for the timeout argument.
     */
    TimeUnit timeoutTimeUnit() default TimeUnit.SECONDS;
}
