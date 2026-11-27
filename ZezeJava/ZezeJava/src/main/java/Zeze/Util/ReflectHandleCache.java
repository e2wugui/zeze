package Zeze.Util;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** 按类名反射实例化并缓存无参构造的句柄对象（Timer / SafeBatch 共用）。 */
public final class ReflectHandleCache<THandle> {
	private final Class<THandle> handleType;
	private final ConcurrentHashMap<String, THandle> handleCache = new ConcurrentHashMap<>();
	private final ReentrantLock lock = new ReentrantLock();

	public ReflectHandleCache(Class<THandle> handleType) {
		this.handleType = Objects.requireNonNull(handleType, "handleType");
	}

	public THandle findHandle(String handleClassName) {
		var handle = handleCache.get(handleClassName);
		if (handle != null)
			return handle;

		lock.lock();
		try {
			handle = handleCache.get(handleClassName);
			if (handle != null)
				return handle;

			var handleClass = Class.forName(handleClassName);
			if (!handleType.isAssignableFrom(handleClass)) {
				throw new ClassCastException(handleClass.getName() + " is not assignable to " + handleType.getName());
			}
			var ctor = handleClass.getDeclaredConstructor();
			ctor.setAccessible(true);
			handle = handleType.cast(ctor.newInstance());
			handleCache.put(handleClassName, handle);
			return handle;
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		} finally {
			lock.unlock();
		}
	}
}
