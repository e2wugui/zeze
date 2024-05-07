package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta1;
import Zeze.Transaction.Log;
import Zeze.Util.Task;

public class LogBeanKey<T extends Serializable> extends Log {
	protected final Meta1<T> meta;
	public T value;

	// 事务修改过程中不需要Factory。
	public LogBeanKey(Class<T> beanClass, Bean belong, int varId, T value) {
		setBelong(belong);
		setVariableId(varId);
		meta = Meta1.getBeanMeta(beanClass);
		this.value = value;
	}

	public LogBeanKey(Class<T> beanClass) {
		meta = Meta1.getBeanMeta(beanClass);
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(ByteBuffer bb) {
		value.encode(bb);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(IByteBuffer bb) {
		try {
			value = (T)meta.valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
		}
		value.decode(bb);
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
