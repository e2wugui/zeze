package Zeze.Util;

@FunctionalInterface
public interface Func1<T, R> {
	R call(T t1) throws Throwable;
}
