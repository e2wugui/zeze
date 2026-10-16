package Zeze.Util;

import org.jspecify.annotations.Nullable;

// 双参无返回值回调（可抛异常）
@FunctionalInterface
public interface Action2<T1 extends @Nullable Object, T2 extends @Nullable Object> {
	void run(T1 t1, T2 t2) throws Exception;
}
