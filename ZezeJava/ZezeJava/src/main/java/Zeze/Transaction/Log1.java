package Zeze.Transaction;

public abstract class Log1<TBean extends Bean, TValue> extends Log {
	private TValue Value;
	public final TValue getValue() {
		return Value;
	}
	public final void setValue(TValue value) {
		Value = value;
	}

	protected Log1(Bean bean, TValue value) {
		super(bean);
		this.setValue(value);
	}

	@SuppressWarnings("unchecked")
	public final TBean getBeanTyped() {
		return (TBean)getBean();
	}
}