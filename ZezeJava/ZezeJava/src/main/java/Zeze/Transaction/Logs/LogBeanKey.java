package Zeze.Transaction.Logs;

import java.lang.invoke.MethodHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Util.Reflect;

public abstract class LogBeanKey<T extends Serializable> extends Log {
	public T value;
	private final MethodHandle valueFactory;

	public LogBeanKey(Class<T> valueClass) {
		super("Zeze.Transaction.Log<" + Reflect.getStableName(valueClass) + '>');
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
	public void decode(ByteBuffer bb) {
		try {
			value = (T)valueFactory.invoke();
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
