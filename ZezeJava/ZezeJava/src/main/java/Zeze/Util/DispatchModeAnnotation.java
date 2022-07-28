package Zeze.Util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import Zeze.Transaction.DispatchMode;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DispatchModeAnnotation {
	DispatchMode Mode() default DispatchMode.Normal;
}
