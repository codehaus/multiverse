package org.multiverse.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The NonTransactional annotation can be used for the following purposes:
 * <ol>
 * <li>Can be placed on a field of an {@link TransactionalObject} to exclude it from being managed by
 * the STM. So this field is for the STM completely invisible; as if it doesn't exist.</li>
 * <li>Can be placed on a instance method of a {@link TransactionalObject} to exclude it from being
 * transactional.
 * </li>
 * </ol>
 *
 * @author Peter Veentjer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface NonTransactional {
}
