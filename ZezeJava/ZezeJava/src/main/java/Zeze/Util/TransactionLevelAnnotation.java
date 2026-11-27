package Zeze.Util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import Zeze.Transaction.TransactionLevel;

/** 标注协议处理方法的事务隔离级别，供框架反射读取。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionLevelAnnotation {
	TransactionLevel Level() default TransactionLevel.Serializable;
}
