package harness;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/** 每个测试方法及其JUnit生命周期在独立子JVM中执行，致命退出不会终止父worker。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ForkedJvmExtension.class)
public @interface ForkedJvm {
	int timeoutSeconds() default 300;

	/** 必须命中指定致命日志、完成fatal回调断言并实际halt；可在退出后重开持久库验证。 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ExpectHalt {
		String value();

		/** 当前测试类上的public static void方法，参数为子进程工作目录Path。 */
		String verifyAfterHalt() default "";
	}
}
