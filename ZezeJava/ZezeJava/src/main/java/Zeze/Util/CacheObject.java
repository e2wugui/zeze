package Zeze.Util;

import Zeze.Serialize.Serializable;

public interface CacheObject extends Serializable {
	/**
	 * 系列化时不包括cacheId。
	 */
	public int cacheId();
}
