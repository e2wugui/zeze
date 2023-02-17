package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta1;
import Zeze.Transaction.Log;

public abstract class LogBeanKey<T extends Serializable> extends Log {
	protected final Meta1<T> meta;
	public T value;

	public LogBeanKey(Class<T> beanClass) {
		meta = Meta1.getBeanMeta(beanClass);
	}

	// 事务修改过程中不需要Factory。
	public LogBeanKey(Class<T> cls, Bean belong, int varId, T value) {
		this(cls);
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public void encode(ByteBuffer bb) {
		value.encode(bb);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(ByteBuffer bb) {
		try {
			value = (T)meta.valueFactory.invoke();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
		value.decode(bb);
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
