package Zeze.Util;

import org.jspecify.annotations.Nullable;

// 无参带返回值回调（可抛异常）
@FunctionalInterface
public interface Func0<R extends @Nullable Object> {
	R call() throws Exception;
}
