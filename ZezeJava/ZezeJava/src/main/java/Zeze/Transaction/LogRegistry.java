package Zeze.Transaction;

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

	@FunctionalInterface
	// Public because History supplies decoders from a different package.
	public interface LogDecoder {
		/** Return a fresh complete value; failed decoding must not mutate a published source payload. */
		Log decode(int variableId, IByteBuffer input);
	}

	public record Factory(int typeId, String typeName, LogDecoder decoder) {
	}

	/** 返回本次注册的候选工厂；重复注册仍保留首个工厂，替换时须按对象身份验证所有权。 */
	public static Factory register(int typeId, String typeName, LogDecoder decoder) {
		var factory = new Factory(typeId, typeName, decoder);
		Factory existing;
		synchronized (factories) {
			existing = factories.putIfAbsent(factory.typeId(), factory);
		}
		if (existing == null)
			logger.debug("register log typeId({}): {}", factory.typeId(), factory.typeName());
		else if (!existing.typeName().equals(factory.typeName()))
			logger.error("register duplicated log typeId({}): {} & {}",
				factory.typeId(), existing.typeName(), factory.typeName());
		else
			logger.debug("register duplicated log typeId({}) with same name: {}, first registered wins",
				factory.typeId(), factory.typeName());
		return factory;
	}

	public static Log decode(int typeId, int variableId, IByteBuffer input) {
		var factory = factories.get(typeId);
		if (factory == null)
			throw new UnsupportedOperationException("unknown log typeId=" + typeId);
		return factory.decoder().decode(variableId, input);
	}

	public static @Nullable Factory getRegistered(int typeId) {
		return factories.get(typeId);
	}

	public static boolean replaceRegistered(int typeId, Factory expected, Factory update) {
		// Record equality is structural; History owns only the exact registered instance.
		// Serialize registry writes so this identity check and replacement are atomic.
		synchronized (factories) {
			return factories.get(typeId) == expected && factories.replace(typeId, expected, update);
		}
	}
}
