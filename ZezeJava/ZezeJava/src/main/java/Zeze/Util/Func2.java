package Zeze.Util;

// 双参带返回值回调（可抛异常）
@FunctionalInterface
public interface Func2<T1, T2, R> {
	R call(T1 t1, T2 t2) throws Exception;
}
