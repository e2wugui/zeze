package Zeze.Transaction;

/** 事务流程控制异常：用于把 Abort/Redo 等控制信号抛回 perform 主循环，不是业务错误。 */
public class GoBackZeze extends Error {
	public GoBackZeze(String msg) {
		super(msg);
	}

	public GoBackZeze(String msg, Throwable cause) {
		super(msg, cause);
	}
}
