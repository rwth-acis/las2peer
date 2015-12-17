package i5.las2peer.restMapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds all name value pairs of a HTTP header to a resource method parameter. The type T of the annotated parameter
 * must be {@link java.lang.String}.
 *
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpHeaders {
}
