package org.multiverse.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be placed on an object to make it Atomic. See the {@link AtomicMethod} for more information.
 * All instance methods will be {@link AtomicMethod} by default (not readonly).
 *
 * @author Peter Veentjer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface AtomicObject {

}
