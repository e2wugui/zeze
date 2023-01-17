package Zeze.Util;

@FunctionalInterface
public interface Action3<T1, T2, T3> {
	void run(T1 t1, T2 t2, T3 t3) throws Exception;
}
