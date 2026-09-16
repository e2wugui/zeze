package Zeze.Util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JVM停机回调栈，LIFO契约：后注册先执行（按add逆序执行）。
 * 由此推出的注册时序约定：资源用户（如Raft）须先于其owner（如Application.start的
 * 注册点）注册——这样owner的拆卸先执行（停掉入口、静默化产生者），用户的收尾后执行；
 * 动态创建的资源请保证在其owner的注册之前完成注册，否则用户动作会先于owner执行，
 * 与owner尚在运转的资源并发。
 */
public final class ShutdownHook {
	private static final @NotNull Logger logger = LogManager.getLogger(ShutdownHook.class);
	private static final LinkedHashMap<Object, Action0> shutdownActions = new LinkedHashMap<>();
	private static final FastLock shutdownActionsLock = new FastLock();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook") {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				logger.info("ShutdownHook begin");
				Map.Entry<Object, Action0>[] entries;
				shutdownActionsLock.lock();
				try {
					entries = shutdownActions.entrySet().toArray(new Map.Entry[0]);
				} finally {
					shutdownActionsLock.unlock();
				}
				for (int i = entries.length - 1; i >= 0; i--) { // 按add的逆序执行各action
					var entry = entries[i];
					try {
						entry.getValue().run();
					} catch (Throwable e) { // run handle.
						logger.error("action({}).run exception:", entry.getKey(), e);
					}
				}
				logger.info("ShutdownHook end");
				LogManager.shutdown();
			}
		});
	}

	public static void init() {
		// 只用来确保上面的static块已执行
	}

	public static void add(@NotNull Action0 action) {
		add(new Object(), action);
	}

	public static void add(@NotNull Object key, @NotNull Action0 action) {
		shutdownActionsLock.lock();
		try {
			shutdownActions.put(key, action);
		} finally {
			shutdownActionsLock.unlock();
		}
	}

	public static @Nullable Action0 remove(@NotNull Object key) {
		shutdownActionsLock.lock();
		try {
			return shutdownActions.remove(key);
		} finally {
			shutdownActionsLock.unlock();
		}
	}

	private ShutdownHook() {
	}
}
