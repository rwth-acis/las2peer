package i5.las2peer.restMapper.annotations.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiInfo {
	String title();
	String description();
	String contact();
	String license();
	String licenseUrl();
	String termsOfServiceUrl();
}