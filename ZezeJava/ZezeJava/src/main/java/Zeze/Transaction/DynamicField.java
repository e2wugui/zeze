package Zeze.Transaction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

/** 生成字段与 History 解码共用的动态字段描述符。 */
public final class DynamicField {
	record Id(long ownerSchemaId, int fieldId) {
		static Id decode(IByteBuffer bb) {
			return new Id(bb.ReadLong(), bb.ReadUInt());
		}
	}

	private static final ClassValue<Map<Integer, DynamicField>> declarations = new ClassValue<>() {
		@Override
		protected Map<Integer, DynamicField> computeValue(Class<?> type) {
			return new ConcurrentHashMap<>();
		}
	};

	final Id id;
	final ToLongFunction<? super Bean> get;
	final LongFunction<@Nullable Bean> create;
	final Class<?>[] dependencies;
	private final Map<Integer, LogRegistry.LogDecoder> decoders = new HashMap<>();
	private boolean sealed;

	private DynamicField(long ownerSchemaId, int fieldId, ToLongFunction<? super Bean> get,
						 LongFunction<@Nullable Bean> create, Class<?>... dependencies) {
		id = new Id(ownerSchemaId, fieldId);
		this.get = get;
		this.create = create;
		this.dependencies = dependencies.clone();
	}

	/** 由生成类的静态字段初始化器调用。 */
	public static DynamicField declare(Class<? extends Bean> owner, long ownerSchemaId, int fieldId,
									   ToLongFunction<? super Bean> get, LongFunction<@Nullable Bean> create,
									   Class<?>... dependencies) {
		var field = new DynamicField(ownerSchemaId, fieldId, get, create, dependencies);
		if (declarations.get(owner).putIfAbsent(fieldId, field) != null)
			throw new IllegalStateException("duplicate dynamic field " + field.id);
		return field;
	}

	static Collection<DynamicField> declaredFields(Class<?> owner) {
		return declarations.get(owner).values();
	}

	/** 集合 Meta 须在 History 注册前绑定解码器。 */
	public synchronized void bindLog(int typeId, LogRegistry.LogDecoder decoder) {
		if (sealed || decoders.putIfAbsent(typeId, decoder) != null)
			throw new IllegalStateException("cannot bind dynamic log " + id + ", typeId=" + typeId);
	}

	synchronized void seal() {
		sealed = true;
	}

	Log decodeLog(int typeId, int variableId, IByteBuffer bb) {
		var decoder = decoders.get(typeId);
		if (decoder == null)
			throw new IllegalStateException("unknown dynamic log " + id + ", typeId=" + typeId);
		return decoder.decode(variableId, bb);
	}

	void encodeId(ByteBuffer bb) {
		bb.WriteLong(id.ownerSchemaId());
		bb.WriteUInt(id.fieldId());
	}

	public static void checkTarget(@Nullable DynamicField source, @Nullable DynamicField target) {
		if (source != target && (source == null || target == null || !source.id.equals(target.id)))
			throw new IllegalArgumentException("dynamic log field " + (source != null ? source.id : null)
				+ " does not match target " + (target != null ? target.id : null));
	}

	/** 直接插入时，包装器须由目标字段创建。 */
	public static void checkValueOwner(@Nullable DynamicField field, Bean value) {
		if (field != null && value instanceof DynamicBean dynamic && dynamic.dynamicField() != field)
			throw new IllegalArgumentException("dynamic value was not created for field " + field.id);
	}

	/** 同字段复制保留原编号。跨字段复制使用目标字段的映射。 */
	@SuppressWarnings("unchecked")
	public static <V extends Bean> V copyValue(@Nullable DynamicField target, V value) {
		if (target == null || !(value instanceof DynamicBean dynamic) || dynamic.dynamicField() == target)
			return (V)value.copy();
		var copy = new DynamicBean(target);
		if (!dynamic.isEmpty())
			copy.assign(dynamic);
		return (V)copy;
	}
}
