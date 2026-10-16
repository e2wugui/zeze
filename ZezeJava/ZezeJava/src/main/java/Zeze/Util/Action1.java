package Zeze.Util;

import org.jspecify.annotations.Nullable;

// 单参无返回值回调（可抛异常）
@FunctionalInterface
public interface Action1<T extends @Nullable Object> {
	void run(T t) throws Exception;
}
