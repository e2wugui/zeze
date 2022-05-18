package Zeze.Util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import Zeze.Transaction.TransactionLevel;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionLevelAnnotation {
	TransactionLevel Level() default TransactionLevel.Serializable;
}
