package Zeze.Util;

// 三参带返回值回调（可抛异常）
@FunctionalInterface
public interface Func3<T1, T2, T3, R> {
	R call(T1 t1, T2 t2, T3 t3) throws Exception;
}
