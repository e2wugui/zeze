package Zeze.Util;

/** 框架不捕获的 Error 异常，用于测试断言或特殊控制流穿透。 */
public class ThrowAgainException extends Error {
	public ThrowAgainException() {
	}

	public ThrowAgainException(String message) {
		super(message);
	}
}
