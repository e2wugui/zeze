package Zeze.Util;

// 单参带返回值回调（可抛异常）
@FunctionalInterface
public interface Func1<T, R> {
	R call(T t1) throws Exception;
}
