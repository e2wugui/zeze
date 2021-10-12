package Zeze.Util;

@FunctionalInterface
public interface Action2<T1, T2> {
	public void run(T1 t1, T2 t2);
}
