package Zeze.Hot;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import org.jetbrains.annotations.NotNull;

public class HotHandle<THandle> extends ReentrantLock {
	private final ConcurrentHashMap<String, THandle> handleCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<HotModule, HashSet<String>> classNameWithHotModule = new ConcurrentHashMap<>();

	private void onHotModuleStop(@NotNull HotModule hot) {
		var classNames = classNameWithHotModule.remove(hot);
		if (classNames != null) {
			for (var name : classNames)
				handleCache.remove(name);
		}
	}

	public static @NotNull Class<?> findClass(@NotNull Application zeze, @NotNull String handleClassName)
			throws ClassNotFoundException {
		var hotManager = zeze.getHotManager();
		return hotManager == null
				? Class.forName(handleClassName)
				: hotManager.getHotRedirect().loadClass(handleClassName);
	}

	@SuppressWarnings("unchecked")
	public @NotNull THandle findHandle(@NotNull Application zeze, @NotNull String handleClassName)
			throws ReflectiveOperationException {
		var handle = handleCache.get(handleClassName);
		if (handle != null)
			return handle;

		lock();
		try {
			handle = handleCache.get(handleClassName);
			if (handle != null)
				return handle;

			var handleClass = findClass(zeze, handleClassName);
			var cl = handleClass.getClassLoader();
			if (HotManager.isHotModule(cl)) {
				var hotModule = (HotModule)cl;
				// 这里每次都注册，简化框架关联。
				classNameWithHotModule.computeIfAbsent(hotModule, (key) -> new HashSet<>()).add(handleClassName);
				hotModule.stopEvents.add(this::onHotModuleStop);
			}

			handle = (THandle)handleClass.getConstructor().newInstance();
			handleCache.put(handleClassName, handle);
			return handle;
		} finally {
			unlock();
		}
	}
}
