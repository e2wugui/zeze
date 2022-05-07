package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import org.apache.commons.lang3.NotImplementedException;

public abstract class Log1<TBean extends Bean, TValue> extends Log {
	private TValue Value;
	public final TValue getValue() {
		return Value;
	}
	public final void setValue(TValue value) {
		Value = value;
	}

	// TODO delete me
	protected Log1(Bean bean, TValue value) {
		super(bean);
		this.setValue(value);
	}

	protected Log1(Bean bean, int varId, TValue value) {
		super(bean, varId);
		this.setValue(value);
	}

	@SuppressWarnings("unchecked")
	public final TBean getBeanTyped() {
		return (TBean)getBean();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		throw new NotImplementedException("");
	}

	@Override
	public void Decode(ByteBuffer bb) {
		throw new NotImplementedException("");
	}

	@Override
	public void EndSavepoint(Savepoint currentsp) {
		currentsp.getLogs().put(getLogKey(), this);
	}

	@Override
	public Log BeginSavepoint() {
		return this;
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
