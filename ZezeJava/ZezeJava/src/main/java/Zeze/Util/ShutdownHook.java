package Zeze.Util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ShutdownHook {
	private static final Logger logger = LogManager.getLogger(ShutdownHook.class);
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
					entries = shutdownActions.entrySet().toArray(new Map.Entry[shutdownActions.size()]);
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

	public static void add(Action0 action) {
		add(new Object(), action);
	}

	public static void add(Object key, Action0 action) {
		shutdownActionsLock.lock();
		try {
			shutdownActions.put(key, action);
		} finally {
			shutdownActionsLock.unlock();
		}
	}

	public static Action0 remove(Object key) {
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
