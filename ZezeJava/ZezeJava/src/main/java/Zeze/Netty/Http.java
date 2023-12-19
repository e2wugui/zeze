package Zeze.Netty;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.Objects;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Http {
	private Http() {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Path {
		@Nullable String path() default ""; // 空表示没有父路径,否则必须以'/'开头
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Get {
		@Nullable String path() default ""; // 空表示以方法名命名,否则必须以'/'开头

		@Nullable TransactionLevel transactionLevel() default TransactionLevel.None;

		@Nullable DispatchMode dispatchMode() default DispatchMode.Normal;
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Post {
		@Nullable String path() default ""; // 空表示以方法名命名,否则必须以'/'开头

		int maxContentLength() default 4096;

		@Nullable TransactionLevel transactionLevel() default TransactionLevel.None;

		@Nullable DispatchMode dispatchMode() default DispatchMode.Normal;
	}

	public static int register(@NotNull HttpServer httpServer, @NotNull Class<?> handleClass) {
		int n = 0;
		var pathAnno = handleClass.getDeclaredAnnotation(Path.class);
		var parentPath = pathAnno != null ? pathAnno.path() : "";
		if (parentPath == null)
			parentPath = "";
		else if (!(parentPath = parentPath.trim()).isEmpty() && parentPath.charAt(0) != '/') {
			throw new IllegalStateException("Http.register: path must be started with '/' in class: "
					+ handleClass.getName());
		}

		var lookup = MethodHandles.lookup();
		for (var method : handleClass.getDeclaredMethods()) {
			Annotation httpAnno = null;
			for (var anno : method.getAnnotations()) {
				var annoType = anno.annotationType();
				if (annoType == Get.class || annoType == Post.class) {
					if (httpAnno != null) {
						throw new IllegalStateException("Http.register: duplicated annotations in method: "
								+ method.getName() + " @ " + handleClass.getName());
					}
					httpAnno = anno;
				}
			}
			if (httpAnno == null)
				continue;
			if ((method.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) != (Modifier.PUBLIC | Modifier.STATIC)) {
				throw new IllegalStateException("Http.register: not public static method: "
						+ method.getName() + " @ " + handleClass.getName());
			}
			if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != HttpExchange.class) {
				throw new IllegalStateException("Http.register: must be one HttpExchange parameter in method: "
						+ method.getName() + " @ " + handleClass.getName());
			}

			String path;
			int maxContentLength;
			TransactionLevel transactionLevel;
			DispatchMode dispatchMode;
			if (httpAnno.annotationType() == Get.class) {
				var getAnno = (Get)httpAnno;
				path = getAnno.path();
				maxContentLength = 0;
				transactionLevel = getAnno.transactionLevel();
				dispatchMode = getAnno.dispatchMode();
			} else { // Post.class
				var postAnno = (Post)httpAnno;
				path = postAnno.path();
				maxContentLength = postAnno.maxContentLength();
				transactionLevel = postAnno.transactionLevel();
				dispatchMode = postAnno.dispatchMode();
			}
			if (path == null || (path = path.trim()).isEmpty())
				path = '/' + method.getName();
			else if (path.charAt(0) != '/') {
				throw new IllegalStateException("Http.register: path must be started with '/' in method: "
						+ method.getName() + " @ " + handleClass.getName());
			}

			HttpEndStreamHandle handler;
			try {
				var mt = MethodType.methodType(void.class, HttpExchange.class);
				var callSite = LambdaMetafactory.metafactory(lookup, "onEndStream",
						MethodType.methodType(HttpEndStreamHandle.class), mt, lookup.unreflect(method), mt);
				handler = Objects.requireNonNull((HttpEndStreamHandle)callSite.getTarget().invokeExact());
			} catch (Throwable e) { // rethrow
				Task.forceThrow(e);
				break; // never run here
			}
			httpServer.addHandler(parentPath + path, maxContentLength, transactionLevel, dispatchMode, handler);
			n++;
		}
		return n;
	}
}
