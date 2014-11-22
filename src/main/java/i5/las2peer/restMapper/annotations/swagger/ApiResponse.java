package i5.las2peer.restMapper.annotations.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ApiResponse {
    /**
     * The HTTP status code of the response.
     * <p/>
     * The value should be one of the formal <a target="_blank" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">HTTP Status Code Definitions</a>.
     */
    int code();

    /**
     * Human-readable message to accompany the response.
     */
    String message();

    /**
     * Optional response class to describe the payload of the message.
     * <p/>
     * Corresponds to the `responseModel` field of the response message object.
     */
    Class<?> response() default Void.class;
}