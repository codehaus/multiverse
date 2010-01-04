package org.multiverse.transactional.annotations;

import java.lang.annotation.*;

/**
 * Can be placed on an object to make it Transactional. See the {@link TransactionalMethod} for more information.
 * <p/>
 * All instance methods will be {@link TransactionalMethod} by default (not readonly).
 * <p/>
 * This annotation is an {@link java.lang.annotation.Inherited} annotation, so automatically all subclasses
 * will be TransactionalObjects when a @TransactionalObject annotation is on a class. no matter if the
 * subclass doesn't add the {@link java.lang.annotation.Inherited} annotation.
 *
 * @author Peter Veentjer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface TransactionalObject {

}
