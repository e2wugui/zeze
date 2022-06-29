package Zeze.Util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ShutdownHook {
	private static final Logger logger = LogManager.getLogger(ShutdownHook.class);
	private static final LinkedHashMap<Object, Action0> shutdownActions = new LinkedHashMap<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook") {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				logger.info("ShutdownHook begin");
				Map.Entry<Object, Action0>[] entries;
				synchronized (shutdownActions) {
					entries = shutdownActions.entrySet().toArray(new Map.Entry[shutdownActions.size()]);
				}
				for (int i = entries.length - 1; i >= 0; i--) { // 按add的逆序执行各action
					var entry = entries[i];
					try {
						entry.getValue().run();
					} catch (Throwable e) {
						logger.error("action(" + entry.getKey() + ").run exception:", e);
					}
				}
				logger.info("ShutdownHook end");
			}
		});
	}

	public static void add(Action0 action) {
		add(new Object(), action);
	}

	public static void add(Object key, Action0 action) {
		synchronized (shutdownActions) {
			shutdownActions.put(key, action);
		}
	}

	public static Action0 remove(Object key) {
		synchronized (shutdownActions) {
			return shutdownActions.remove(key);
		}
	}

	private ShutdownHook() {
	}
}
