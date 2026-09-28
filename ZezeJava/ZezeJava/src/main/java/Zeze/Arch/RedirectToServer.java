package Zeze.Arch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注按 serverId 路由的 redirect 生成方法的注解。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedirectToServer {
	boolean oneByOne() default true;

	boolean orOtherServer() default false;

	int timeout() default 30_000;

	int version() default 0;
}
