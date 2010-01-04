package org.multiverse.transactional.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be placed on constructor. There is a lot of overlap with the
 * {@link TransactionalMethod} annotation, but the big difference is that transactions
 * on constructors can't be retried. So everything related to retrying has been removed.
 * <p/>
 * The reason why this annotation exists is that it is very hard to add the instrumentation
 * in a constructor so that it can be retried. It has to do with a bytecode verification that
 * treats an uninitialized objects as a different type than an initialized one.
 *
 * @author Peter Veentjer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR})
public @interface TransactionalConstructor {

    boolean readonly() default false;

    /**
     * The familyname of the transaction. If not set, a familyname will be inferred based on the classname en
     * method signature and in most cases this is the best solution (not providing a value). The family name
     * is not only useful for logging purposes, but als for optimizations like selecting the optimal transaction
     * implementation for that specific set of transactions.
     *
     * @return the familyname of the transaction
     */
    String familyName() default "";

    /**
     * If the transaction automatically should track reads. This behavior is not only useful to reduce
     * read conflicts, it also is needed for blocking transactions and prevention of the write skew problem.
     *
     * @return true if it should do automatic readtracking, false otherwise.
     */
    boolean automaticReadTracking() default true;

    /**
     * If the writeskew problem should be prevented. If set to true, the automaticReadTracking also has to
     * be set to true.
     *
     * @return true if the writeskew problem should be prevented.
     */
    boolean detectWriteSkew() default true;

    boolean loggingEnabled() default false;
}
