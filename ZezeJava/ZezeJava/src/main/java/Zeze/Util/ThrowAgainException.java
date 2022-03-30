package Zeze.Util;

// 框架不会捕捉这个异常，用来测试或者特别框架。
public class ThrowAgainException extends Error {
	public ThrowAgainException() {
	}

	public ThrowAgainException(String message) {
		super(message);
	}
}
