package Zeze.Util;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;

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
