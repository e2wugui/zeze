package Zeze.Hot;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Util.Action1;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public class HotHandle<THandle> extends ReentrantLock {
	private final ConcurrentHashMap<String, THandle> handleCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<HotModule, HashSet<String>> classNameWithHotModule = new ConcurrentHashMap<>();

	// CARRY（FND-G1-8同型）：this::onHotModuleStop每次求值都产生新实例，而stopEvents
	// （ConcurrentHashSet，键=元素自身）按实例判等——每次缓存miss（含模块stop清缓存后的
	// 重新装载）都会叠加一条新回调，模块停止时同一回调被重复执行多次且无法移除。
	// 固定为实例字段只求值一次：同一HotHandle的登记幂等（总量有界：HotHandle实例数）。
	private final Action1<HotModule> onHotModuleStopRef = this::onHotModuleStop;

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
	public @NotNull THandle findHandle(@NotNull Application zeze, @NotNull String handleClassName) {
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
				hotModule.stopEvents.add(onHotModuleStopRef);
			}

			var ctorMethod = handleClass.getDeclaredConstructor((Class<?>[])null);
			ctorMethod.setAccessible(true);
			handle = (THandle)ctorMethod.newInstance((Object[])null);
			handleCache.put(handleClassName, handle);
			return handle;
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		} finally {
			unlock();
		}
	}
}
