package Zeze.Util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import Zeze.Transaction.DispatchMode;

// 标注方法的异步分发模式（反射读取，供 Task/模块分发用）
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DispatchModeAnnotation {
	DispatchMode mode() default DispatchMode.Normal;
}
