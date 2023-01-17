package Zeze.Util;

@FunctionalInterface
public interface Action1<T> {
	void run(T t) throws Exception;
}
