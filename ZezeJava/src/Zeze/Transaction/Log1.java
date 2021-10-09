package Zeze.Transaction;

import Zeze.*;

public abstract class Log<TBean extends Bean, TValue> extends Log {
	private TValue Value;
	public final TValue getValue() {
		return Value;
	}
	public final void setValue(TValue value) {
		Value = value;
	}

	protected Log(Bean bean, TValue value) {
		super(bean);
		this.setValue(value);
	}

	public final TBean getBeanTyped() {
		return (TBean)getBean();
	}
}