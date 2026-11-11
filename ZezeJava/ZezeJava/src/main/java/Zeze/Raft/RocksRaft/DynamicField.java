package Zeze.Raft.RocksRaft;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import org.jspecify.annotations.Nullable;

/** Declared field identity and type mapping for RocksRaft values and replicated logs. */
public final class DynamicField {
	private record Id(long ownerSchemaId, int fieldId) {
	}

	private record Decoder(DynamicField field, int typeId, Supplier<? extends LogBean> factory) {
	}

	private static final ConcurrentHashMap<Id, Decoder> decoders = new ConcurrentHashMap<>();

	private final Id id;
	private final ToLongFunction<Bean> identify;
	private final LongFunction<@Nullable Bean> create;
	private final BeanValueFactory<DynamicBean> values = new BeanValueFactory<>(this::newValue, this);

	public DynamicField(long ownerSchemaId, int fieldId, ToLongFunction<Bean> identify,
						LongFunction<@Nullable Bean> create) {
		if (fieldId <= 0 || fieldId > Bean.MAX_VARIABLE_ID)
			throw new IllegalArgumentException("invalid RocksRaft dynamic fieldId=" + fieldId);
		id = new Id(ownerSchemaId, fieldId);
		this.identify = identify;
		this.create = create;
	}

	public DynamicBean newValue() {
		return new DynamicBean(this, id.fieldId());
	}

	public void registerValueLog() {
		register(LogDynamic.TYPE_ID, () -> new LogDynamic(this));
	}

	public void registerListLog() {
		int typeId = LogList2.typeId(DynamicBean.class);
		register(typeId, () -> new LogList2<>(typeId, values));
	}

	public <K> void registerMapLog(Class<K> keyClass) {
		int typeId = LogMap2.typeId(keyClass, DynamicBean.class);
		var keyCodec = SerializeHelper.createCodec(keyClass);
		register(typeId, () -> new LogMap2<>(typeId, keyCodec, values));
	}

	public <K extends Comparable<K>> void registerSortedMapLog(Class<K> keyClass) {
		int typeId = LogSortedMap2.typeId(keyClass, DynamicBean.class);
		var keyCodec = SerializeHelper.createCodec(keyClass);
		register(typeId, () -> new LogSortedMap2<>(typeId, keyCodec, values));
	}

	private void register(int typeId, Supplier<? extends LogBean> factory) {
		var existing = decoders.putIfAbsent(id, new Decoder(this, typeId, factory));
		if (existing != null && (existing.field != this || existing.typeId != typeId))
			throw new IllegalStateException("duplicate RocksRaft dynamic field " + id);
	}

	BeanValueFactory<DynamicBean> values() {
		return values;
	}

	void encodeOwner(ByteBuffer bb, int variableId) {
		if (variableId != id.fieldId())
			throw new IllegalStateException("RocksRaft log variableId " + variableId + " does not match " + id);
		bb.WriteLong(id.ownerSchemaId());
	}

	long identify(Bean bean) {
		long type = identify.applyAsLong(bean);
		if (type == 0)
			throw new IllegalArgumentException("reserved empty type for RocksRaft dynamic field " + id);
		var decoded = create.apply(type);
		if (decoded == null)
			throw new IllegalArgumentException("unknown RocksRaft dynamic type " + type + " for " + id);
		if (decoded.getClass() != bean.getClass() || decoded.isManaged())
			throw new IllegalArgumentException("RocksRaft dynamic type " + type + " for " + id
					+ " must create an unmanaged " + bean.getClass().getName()
					+ ", got " + decoded.getClass().getName() + (decoded.isManaged() ? " (managed)" : ""));
		return type;
	}

	@Nullable Bean readValue(long type, IByteBuffer bb) {
		if (type == 0)
			return null;
		var bean = create.apply(type);
		if (bean == null)
			throw new IllegalStateException("unknown RocksRaft dynamic type " + type + " for " + id);
		if (bean instanceof DynamicBean || bean instanceof Collection || bean.isManaged())
			throw new IllegalStateException("invalid RocksRaft payload for dynamic type " + type + " in " + id);
		bean.decode(bb);
		return bean;
	}

	static void checkMatch(@Nullable DynamicField expected, @Nullable DynamicField actual) {
		if (expected != actual)
			throw new IllegalArgumentException("RocksRaft dynamic field " + (actual != null ? actual.id : null)
					+ " does not match " + (expected != null ? expected.id : null));
	}

	static LogBean newLog(long ownerSchemaId, int fieldId, int typeId) {
		var id = new Id(ownerSchemaId, fieldId);
		var decoder = decoders.get(id);
		if (decoder == null)
			throw new IllegalStateException("unknown RocksRaft dynamic field " + id);
		if (decoder.typeId != typeId)
			throw new IllegalStateException("unknown RocksRaft dynamic log type " + typeId + " for " + id);
		return decoder.factory.get();
	}
}
