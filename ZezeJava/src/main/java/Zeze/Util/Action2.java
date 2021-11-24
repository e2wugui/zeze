package Zeze.Util;

@FunctionalInterface
public interface Action2<T1, T2> {
	void run(T1 t1, T2 t2) throws Throwable;
}
