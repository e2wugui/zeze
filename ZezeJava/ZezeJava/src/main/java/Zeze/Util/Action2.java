package Zeze.Util;

// 双参无返回值回调（可抛异常）
@FunctionalInterface
public interface Action2<T1, T2> {
	void run(T1 t1, T2 t2) throws Exception;
}
