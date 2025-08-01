package Zeze.Arch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedirectHash {
	boolean oneByOne() default true;

	String ConcurrentLevelSource() default "";

	int timeout() default 30_000;

	int version() default 0;
}
