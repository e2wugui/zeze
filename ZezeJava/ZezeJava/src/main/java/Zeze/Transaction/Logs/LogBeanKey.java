package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta1;
import Zeze.Transaction.Log;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public class LogBeanKey<T extends Serializable> extends Log {
	private final @NotNull Meta1<T> meta;
	private final VarHandle vh;
	public T value;

	// 事务修改过程中不需要Factory。
	@SuppressWarnings("unchecked")
	public LogBeanKey(Bean belong, int varId, VarHandle vh, @NotNull T value) {
		super(belong, varId);
		meta = Meta1.getBeanMeta((Class<T>)value.getClass());
		this.vh = vh;
		this.value = value;
	}

	public LogBeanKey(int varId, @NotNull Class<T> beanClass) {
		super(null, varId);
		meta = Meta1.getBeanMeta(beanClass);
		vh = null;
	}

	@Override
	public Category category() {
		return Category.eHistory;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public @NotNull String getTypeName() {
		return meta.name;
	}

	@Override
	public void commit() {
		vh.set(getBelong(), value);
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
