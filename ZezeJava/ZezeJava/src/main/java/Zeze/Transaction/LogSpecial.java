package Zeze.Transaction;

import org.jspecify.annotations.Nullable;

/** 特殊日志基类：不属于 bean 字段修改的值型日志（如 BeanKey/操作日志），携带单一值参与提交与序列化。 */
public abstract class LogSpecial<TBean extends Bean, TValue> extends Log {
	private final @Nullable TValue value;

	protected LogSpecial(Bean bean, int varId, @Nullable TValue value) {
		super(bean, varId);
		this.value = value;
	}

	@Override
	public Category category() {
		return Category.eSpecial;
	}

	public final @Nullable TValue getValue() {
		return value;
	}

	@SuppressWarnings("unchecked")
	public final @Nullable TBean getBeanTyped() {
		return (TBean)getBelong();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
