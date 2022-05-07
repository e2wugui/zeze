package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import org.apache.commons.lang3.NotImplementedException;

/**
 操作日志。
 主要用于 bean.variable 的修改。
 用于其他非 bean 的日志时，也需要构造一个 bean，用来包装日志。
*/
public abstract class Log implements Serializable {
	public abstract void Commit();
	//public void Rollback() { } // 一般的操作日志不需要实现，特殊日志可能需要。先不实现，参见Savepoint.

	public long getLogKey() {
		return Bean.getObjectId() + getVariableId();
	}
	private Bean Bean;
	private int  VariableId;
	public final Bean getBean() {
		return Bean;
	}
	public final void setBean(Bean value) {
		Bean = value;
	}

	public Log(Bean bean) {
		Bean = bean;
	}
	public Log(Bean bean, int varId) {
		Bean = bean;
		VariableId = varId;
	}
	public final int getVariableId() {
		return VariableId;
	}

	// TODO change to abstract
	public void EndSavepoint(Savepoint currentsp) {
		throw new NotImplementedException("");
	}
	public Log BeginSavepoint() {
		throw new NotImplementedException("");
	}
	public void Encode(ByteBuffer bb) {

	}
	public void Decode(ByteBuffer bb) {

	}
}