package Zeze.Util;

// 单参无返回值回调（可抛异常）
@FunctionalInterface
public interface Action1<T> {
	void run(T t) throws Exception;
}
