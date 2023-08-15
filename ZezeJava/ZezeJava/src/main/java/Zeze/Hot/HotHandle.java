package Zeze.Hot;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;

public class HotHandle<THandle> {
	private final ConcurrentHashMap<String, THandle> handleCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<HotModule, HashSet<String>> classNameWithHotModule = new ConcurrentHashMap<>();

	private void onHotModuleStop(HotModule hot) {
		var classNames = classNameWithHotModule.remove(hot);
		if (null == classNames)
			return;

		for (var name : classNames) {
			handleCache.remove(name);
		}
	}

	public THandle findHandle(Application zeze, String handleClassName) throws Exception {
		var handle = handleCache.get(handleClassName);
		if (null != handle)
			return handle;

		synchronized (handleCache) {
			handle = handleCache.get(handleClassName);
			if (null != handle)
				return handle;

			Class<?> handleClass = (null == zeze.getHotManager())
					? Class.forName(handleClassName)
					: zeze.getHotManager().getHotRedirect().loadClass(handleClassName);

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
		}
	}
}
