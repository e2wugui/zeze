package Zeze.Util;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public interface CacheObject extends Serializable {
	/**
	 * 系列化时不包括cacheId。
	 */
	public String cacheId();

	public static boolean isNull(CacheObject object) {
		return object.cacheId().isEmpty();
	}

	public static class NullCache implements CacheObject {
		public final long CreateTime = System.currentTimeMillis();

		@Override
		public void encode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void decode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String cacheId() {
			return "";
		}
	}
}
