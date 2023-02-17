package Zeze.Transaction;

public abstract class Log1<TBean extends Bean, TValue> extends Log {
	private final TValue value;

	protected Log1(Bean bean, int varId, TValue value) {
		setBean(bean);
		setVariableId(varId);
		this.value = value;
	}

	public final TValue getValue() {
		return value;
	}

	@SuppressWarnings("unchecked")
	public final TBean getBeanTyped() {
		return (TBean)getBean();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
