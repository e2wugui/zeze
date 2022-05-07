package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Reflect;
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
		super("");
		setBean(bean);
		this.setValue(value);
	}

	protected Log1(Bean bean, int varId, TValue value) {
		super(""); // TODO
		setBean(bean);
		setVariableId(varId);
		this.setValue(value);
	}

	@SuppressWarnings("unchecked")
	public final TBean getBeanTyped() {
		return (TBean)getBean();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
