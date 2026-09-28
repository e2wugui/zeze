package Zeze.Util;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;

// Cache 缓存对象接口：cacheId 为缓存键（NullCache 哨兵表示负缓存占位）
public interface CacheObject extends Serializable {
	/**
	 * 系列化时不包括cacheId。
	 */
	String cacheId();

	static boolean isNull(CacheObject object) {
		return object.cacheId().isEmpty();
	}

	class NullCache implements CacheObject {
		public final long CreateTime = System.currentTimeMillis();

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String cacheId() {
			return "";
		}
	}
}
