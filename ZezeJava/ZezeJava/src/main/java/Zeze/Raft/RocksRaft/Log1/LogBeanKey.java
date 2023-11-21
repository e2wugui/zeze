package Zeze.Raft.RocksRaft.Log1;

import java.lang.invoke.MethodHandle;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Reflect;
import Zeze.Util.Task;

public class LogBeanKey<T extends Serializable> extends Log {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.Log<");

	public T value;
	private final MethodHandle valueFactory;

	public LogBeanKey(Class<T> valueClass) {
		super(Zeze.Transaction.Bean.hashLog(logTypeIdHead, valueClass));
		valueFactory = Reflect.getDefaultConstructor(valueClass);
	}

	// 事务修改过程中不需要Factory。
	public LogBeanKey(Class<T> cls, Bean belong, int varId, T value) {
		this(cls);
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		value.encode(bb);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(IByteBuffer bb) {
		try {
			value = (T)valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
		}
		value.decode(bb);
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
