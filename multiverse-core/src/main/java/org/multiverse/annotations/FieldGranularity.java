package org.multiverse.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be placed above a field to indicate that the stm should use a field level granularity
 * instead of object level granularity. If you have a transactional object with 2 mutable fields, 2
 * transactions could conflict even if they are writing to a different one. This reduce this 'unneeded'
 * failure, this annotation can be placed on fields.
 * <p/>
 * Under water these fields are transformed to transactional references or transactional primitives.
 * <p/>
 * <pre>
 *  &at;TransactionalObject
 *  class Person{
 *      &at;FieldGranularity
 *      private String name;
 *      &at;FieldGranularity
 *      private int age;
 *  }
 * </pre>
 * <p/>
 * This is transformed to:
 * <p/>
 * <pre>
 * &at;TransactionalObject
 * class Person{
 *     private final TransactionalReference&lt;String&gt; name = new ...
 *     private final TransactionalInteger age = new ...
 * }
 * </pre>
 *
 * @author Peter Veentjer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface FieldGranularity {

}