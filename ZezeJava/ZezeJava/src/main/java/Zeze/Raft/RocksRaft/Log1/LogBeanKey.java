package Zeze.Raft.RocksRaft.Log1;

import org.jspecify.annotations.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Encodable;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.BeanKey;
import Zeze.Util.Reflect;

/** RocksRaft 存储的 BeanKey 字段修改日志；亦兼作记录 put/remove（value=null 即 remove）。 */
public class LogBeanKey<T extends Encodable> extends Log {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.Log<");

	// Cache the reader by class while retaining the historical log type id.
	private record TypeInfo(int logTypeId, Function<IByteBuffer, ?> valueDecoder) {
		private static final ConcurrentHashMap<Class<?>, TypeInfo> typeInfos = new ConcurrentHashMap<>();

		private static TypeInfo of(Class<?> cls) {
			return typeInfos.computeIfAbsent(cls,
					c -> new TypeInfo(Zeze.Transaction.Bean.hashLog(logTypeIdHead, c),
							BeanKey.class.isAssignableFrom(c) ? SerializeHelper.createDecodeFunc(c)
									: SerializeHelper.createCodec(c, Reflect.getDefaultConstructor(c)).decoder));
		}
	}

	public @Nullable T value;
	private final Function<IByteBuffer, ?> valueDecoder;

	public LogBeanKey(Class<T> valueClass) {
		this(TypeInfo.of(valueClass));
	}

	private LogBeanKey(TypeInfo typeInfo) {
		super(typeInfo.logTypeId());
		valueDecoder = typeInfo.valueDecoder();
	}

	// 事务修改过程中不需要Factory。
	public LogBeanKey(Class<T> cls, Bean belong, int varId, @Nullable T value) {
		this(cls);
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		//noinspection DataFlowIssue
		value.encode(bb);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(IByteBuffer bb) {
		value = (T)valueDecoder.apply(bb);
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
