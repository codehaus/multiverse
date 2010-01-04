package org.multiverse.transactional.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be added to a method and constructors to make them Transactional. A transactional method
 * supports the following properties:
 * <ol>
 * <li>A: failure Atomicity. All changes get in, or no changes get in</li>
 * <li>C: Consistent. A TransactionalMethod can expect to enter memory in a valid state, and is expected when
 * it leaves the space is consistent again.</li>
 * </li>I: Isolated. A transaction will not observe changes made others transactions running in parallel.
 * But it is going to see the changes made by transaction that completed earlier. If a transaction doesn't see
 * this, the system could start to suffer from the lost update problem</li>
 * </ol>
 * <p/>
 * Unfortunately @Inherited doesn't work for methods, only for classes. So if you have an interface containing some
 * methods that need to be atomic, it needs to be added to the implementation and not to the interface. In the future
 * Multiverse may try to this inference this.
 * <p/>
 * When the readonly property is set to true, the transaction is not be able to do any updates and will throw
 * a {@link org.multiverse.api.exceptions.ReadonlyException}. 
 * <p/>
 * With the familyName groups of transactions can be identified that share similar paths of execution. Based on the
 * familyName the stm could do all kinds of optimizations. Luckily using instrumentation this family name can be set
 * (and it should be) based on the class/method-name/method-signature. So no need for users to initialize it explicitly
 * unless they want to.
 * <p/>
 * With the retryCount the number of retries of the transaction can be controlled. For all kinds of reasons a
 * transaction can fail, and these transactions can be retried because the next time they could succeed. An example of
 * such a cause is optimistic locking the stm might use. The default number of retries is 1000.
 *
 * @author Peter Veentjer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TransactionalMethod {

    boolean automaticReadTracking() default false;

    boolean detectWriteSkew() default true;

    String familyName() default "";

    boolean interruptible() default false;

    boolean loggingEnabled() default false;

    boolean readonly() default false;

    int retryCount() default 1000;

    //PropagationLevel propagationLevel() default PropagationLevel.requires;

    //retry policy

    //CommitLockPolicy commitLockPolicy() default GenericCommitLockPolicy.FAIL_FAST;
        
    //long timeout() default -1;

    //TimeUnit timeoutUnit() default TimeUnit.MILLISECONDS;
}
