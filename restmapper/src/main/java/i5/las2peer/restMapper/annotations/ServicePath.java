package i5.las2peer.restMapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The base path where the service is deployed.
 * 
 * This is the base path for the service. All service methods will be available under /path/to/my/service/*.
 * 
 * Please notice that paths of different services have to be prefix free.
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServicePath {
	String value();
}