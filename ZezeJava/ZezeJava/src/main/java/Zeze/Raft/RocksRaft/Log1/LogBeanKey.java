package Zeze.Raft.RocksRaft.Log1;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

/** RocksRaft 存储的 BeanKey 字段修改日志；亦兼作记录 put/remove（value=null 即 remove）。 */
public class LogBeanKey<T extends Serializable> extends Log {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.Log<");

	// typeId与构造器MethodHandle按类缓存；hashLog输入逐字保留，改动即typeId漂移。
	private record TypeInfo(int logTypeId, MethodHandle valueFactory) {
		private static final ConcurrentHashMap<Class<?>, TypeInfo> typeInfos = new ConcurrentHashMap<>();

		private static TypeInfo of(Class<?> cls) {
			return typeInfos.computeIfAbsent(cls,
					c -> new TypeInfo(Zeze.Transaction.Bean.hashLog(logTypeIdHead, c), Reflect.getDefaultConstructor(c)));
		}
	}

	public T value;
	private final MethodHandle valueFactory;

	public LogBeanKey(Class<T> valueClass) {
		this(TypeInfo.of(valueClass));
	}

	private LogBeanKey(TypeInfo typeInfo) {
		super(typeInfo.logTypeId());
		valueFactory = typeInfo.valueFactory();
	}

	// 事务修改过程中不需要Factory。
	public LogBeanKey(Class<T> cls, Bean belong, int varId, T value) {
		this(cls);
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		value.encode(bb);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(@NotNull IByteBuffer bb) {
		try {
			value = (T)valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
		value.decode(bb);
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
