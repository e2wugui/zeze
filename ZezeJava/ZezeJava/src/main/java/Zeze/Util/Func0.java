package Zeze.Util;

// 无参带返回值回调（可抛异常）
@FunctionalInterface
public interface Func0<R> {
	R call() throws Exception;
}
