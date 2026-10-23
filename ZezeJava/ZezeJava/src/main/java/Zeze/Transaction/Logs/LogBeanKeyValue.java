package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Log;
import Zeze.Util.Task;

/** Complete field change value, independent of any local transaction target. */
public class LogBeanKeyValue<T extends Serializable> extends Log {

	private final BeanKeyMeta<T> meta;
	public T value;

	protected LogBeanKeyValue(int varId, BeanKeyMeta<T> meta, T value) {
		super(varId);
		this.meta = meta;
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	@Override
	public void encode(ByteBuffer bb) {
		value.encode(bb);
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> LogBeanKeyValue<T> decode(int varId, BeanKeyMeta<T> meta, IByteBuffer bb) {
		T value;
		try {
			//noinspection DataFlowIssue
			value = (T)meta.valueFactory.invoke();
		} catch (Throwable e) {
			throw Task.forceThrow(e);
		}
		value.decode(bb);
		return new LogBeanKeyValue<>(varId, meta, value);
	}
}
