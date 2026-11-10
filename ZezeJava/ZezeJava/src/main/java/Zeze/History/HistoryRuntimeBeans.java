package Zeze.History;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;

/** 多个 History 实例共享进程级运行时日志注册。 */
final class HistoryRuntimeBeans {
	private static final Set<Class<? extends Bean>> prepared = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Consumer<Class<? extends Bean>> onBeanType = beanClass -> prepare(List.of(beanClass));
	private static int subscriptions;
	private boolean closed;

	private HistoryRuntimeBeans() {
	}

	static HistoryRuntimeBeans subscribe() {
		var subscription = new HistoryRuntimeBeans();
		List<Class<? extends Bean>> snapshot;
		synchronized (prepared) {
			snapshot = BeanFactory.watchRuntimeBeanTypes(onBeanType);
			++subscriptions;
		}
		boolean started = false;
		try {
			prepare(snapshot);
			started = true;
			return subscription;
		} finally {
			if (!started)
				subscription.unsubscribe();
		}
	}

	private static void prepare(List<Class<? extends Bean>> types) {
		var batch = new ArrayList<Class<? extends Bean>>();
		synchronized (prepared) {
			for (var type : types)
				if (!prepared.contains(type))
					batch.add(type);
		}
		if (batch.isEmpty())
			return;
		// 类初始化可能再次登记类型，扫描在锁外执行。
		Helper.registerRuntimeBeanLogs(batch);
		synchronized (prepared) {
			prepared.addAll(batch);
		}
	}

	void unsubscribe() {
		synchronized (prepared) {
			if (closed)
				return;
			closed = true;
			if (--subscriptions == 0)
				BeanFactory.unwatchRuntimeBeanTypes(onBeanType);
		}
	}
}
