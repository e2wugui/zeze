package Zeze.Util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import Zeze.Transaction.TransactionLevel;

// 标注存储过程的事务级别（反射读取，供模块分发用）
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionLevelAnnotation {
	TransactionLevel Level() default TransactionLevel.Serializable;
}
