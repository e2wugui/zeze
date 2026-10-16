package Zeze.Util;

import org.jspecify.annotations.Nullable;

// 三参无返回值回调（可抛异常）
@FunctionalInterface
public interface Action3<T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> {
	void run(T1 t1, T2 t2, T3 t3) throws Exception;
}
