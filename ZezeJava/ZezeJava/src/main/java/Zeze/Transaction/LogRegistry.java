package Zeze.Transaction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.LongConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/** Protocol registration and construction of complete decoded change values. */
public final class LogRegistry {
	private LogRegistry() {
	}

	private static final Logger logger = LogManager.getLogger(LogRegistry.class);
	private static final LongConcurrentHashMap<Factory> factories = new LongConcurrentHashMap<>();

	private static final int DYNAMIC_FIELD_LOG = Bean.hash32("Zeze.Transaction.DynamicFieldLog");
	private static final ConcurrentHashMap<DynamicField.Id, DynamicField> dynamicFields = new ConcurrentHashMap<>();

	/** History 在 Bean 类初始化并绑定解码器后调用。 */
	public static void registerDynamicFields(Class<? extends Bean> beanClass, IntConsumer declaredField,
											 Consumer<Class<?>> dependency) {
		for (Class<?> owner = beanClass; owner != null && owner != Bean.class; owner = owner.getSuperclass()) {
			for (var field : DynamicField.declaredFields(owner)) {
				field.seal();
				var old = dynamicFields.putIfAbsent(field.id, field);
				if (old != null && old != field)
					throw new IllegalStateException("duplicate dynamic field " + field.id);
				declaredField.accept(field.id.fieldId());
				for (var type : field.dependencies)
					dependency.accept(type);
			}
		}
	}

	static DynamicField requireDynamicField(DynamicField.Id id) {
		var field = dynamicFields.get(id);
		if (field == null)
			throw new IllegalStateException("unknown dynamic field " + id);
		return field;
	}

	/** 写入 LogBean 子日志的头部。 */
	public static void encodeChildHeader(ByteBuffer bb, Log log, @Nullable DynamicField field) {
		bb.WriteInt4(field != null ? DYNAMIC_FIELD_LOG : log.getTypeId());
		bb.WriteUInt(log.getVariableId());
		if (field != null) {
			field.encodeId(bb);
			bb.WriteInt4(log.getTypeId());
		}
	}

	@FunctionalInterface
	public interface LogDecoder {
		/** Return a fresh complete value; failed decoding must not mutate a published source payload. */
		Log decode(int variableId, IByteBuffer input);
	}

	private record Factory(String typeName, LogDecoder decoder) {
	}

	/** 重复注册保留首个工厂。 */
	public static void register(int typeId, String typeName, LogDecoder decoder) {
		if (typeId == DYNAMIC_FIELD_LOG)
			throw new IllegalArgumentException("reserved dynamic log typeId=" + typeId);
		var factory = new Factory(typeName, decoder);
		var existing = factories.putIfAbsent(typeId, factory);
		if (existing == null)
			logger.debug("register log typeId({}): {}", typeId, factory.typeName());
		else if (!existing.typeName().equals(factory.typeName()))
			logger.error("register duplicated log typeId({}): {} & {}",
				typeId, existing.typeName(), factory.typeName());
		else
			logger.debug("register duplicated log typeId({}) with same name: {}, first registered wins",
				typeId, factory.typeName());
	}

	public static Log decode(int typeId, int variableId, IByteBuffer input) {
		if (typeId == DYNAMIC_FIELD_LOG) {
			var field = requireDynamicField(DynamicField.Id.decode(input));
			return field.decodeLog(input.ReadInt4(), variableId, input);
		}
		var factory = factories.get(typeId);
		if (factory == null)
			throw new UnsupportedOperationException("unknown log typeId=" + typeId);
		return factory.decoder().decode(variableId, input);
	}
}
