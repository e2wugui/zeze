package Zeze.Util;

// 四参带返回值回调（可抛异常）
@FunctionalInterface
public interface Func4<T1, T2, T3, T4, R> {
	R call(T1 t1, T2 t2, T3 t3, T4 t4) throws Exception;
}
