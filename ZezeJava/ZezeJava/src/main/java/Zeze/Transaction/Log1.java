package Zeze.Transaction;

public abstract class Log1<TBean extends Bean, TValue> extends Log {
	private TValue Value;
	public final TValue getValue() {
		return Value;
	}
	public final void setValue(TValue value) {
		Value = value;
	}

	protected Log1(Bean bean, int varId, TValue value) {
		super(0); // 现在Log1仅用于特殊目的，不支持相关日志系列化。
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
