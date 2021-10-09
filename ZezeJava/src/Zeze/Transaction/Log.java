package Zeze.Transaction;

import Zeze.*;

/** 
 操作日志。
 主要用于 bean.variable 的修改。
 用于其他非 bean 的日志时，也需要构造一个 bean，用来包装日志。
*/
public abstract class Log {
	public abstract void Commit();
	//public void Rollback() { } // 一般的操作日志不需要实现，特殊日志可能需要。先不实现，参见Savepoint.
	public abstract long getLogKey();
	private Bean Bean;
	public final Bean getBean() {
		return Bean;
	}
	public final void setBean(Bean value) {
		Bean = value;
	}
	public Log(Bean bean) {
		setBean(bean);
	}
	public final int getVariableId() {
		return (int)(getLogKey() & getBean().MaxVariableId);
	}
}