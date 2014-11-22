package i5.las2peer.restMapper.annotations.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ApiResponses {
    /**
     * A list of {@link ApiResponse}s provided by the API operation.
     */
    ApiResponse[] value();
}
