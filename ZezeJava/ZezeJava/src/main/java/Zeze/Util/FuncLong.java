package Zeze.Util;

// 无参返回 long 的回调（可抛异常）
@FunctionalInterface
public interface FuncLong {
	long call() throws Exception;
}
