package Zeze.Util;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventDispatcher {
	private static final Logger logger = LogManager.getLogger(EventDispatcher.class);

	@FunctionalInterface
	public interface EventHandle {
		long invoke(Object sender, EventArgument arg) throws Throwable;
	}

	public interface EventArgument {
	}

	public interface Canceler {
		void cancel();
	}

	public static enum Mode {
		RunEmbed,
		RunProcedure,
		RunThread,
	}

	public static final class Events extends ArrayList<EventHandle> {
		private final ReentrantLock lock = new ReentrantLock();
		private final ArrayList<EventHandle> delayRemoves = new ArrayList<>();

		public synchronized void remove(EventHandle handle) {
			if (lock.isLocked() || !lock.tryLock()) // 任何线程持有锁(包括当前线程),或者尝试获取锁失败(有小概率恰好被抢走)
				delayRemoves.add(handle);
			else {
				try {
					if (!delayRemoves.isEmpty()) {
						for (EventHandle h : delayRemoves)
							super.remove(h);
						delayRemoves.clear();
					}
					super.remove(handle);
				} finally {
					lock.unlock();
				}
			}
		}

		public synchronized void tryRemoveDelay() {
			if (!lock.isLocked() && lock.tryLock()) {
				try {
					if (!delayRemoves.isEmpty()) {
						for (EventHandle h : delayRemoves)
							super.remove(h);
						delayRemoves.clear();
					}
				} finally {
					lock.unlock();
				}
			}
		}
	}

	private final String name;
	private final Events runEmbedEvents = new Events();
	private final Events runProcedureEvents = new Events();
	private final Events runThreadEvents = new Events();

	public EventDispatcher(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Events getRunEmbedEvents() {
		return runEmbedEvents;
	}

	public Events getRunProcedureEvents() {
		return runProcedureEvents;
	}

	public Events getRunThreadEvents() {
		return runThreadEvents;
	}

	/**
	 * 注册事件处理函数，如果在事件触发过程中执行注册,则等下次再触发
	 *
	 * @return 如果需要取消注册，请保存返回值，并调用其cancel。
	 */
	public synchronized Canceler add(Mode mode, EventHandle handle) {
		Events events;
		switch (mode) {
		case RunEmbed:
			events = runEmbedEvents;
			break;
		case RunProcedure:
			events = runProcedureEvents;
			break;
		case RunThread:
			events = runThreadEvents;
			break;
		default:
			throw new RuntimeException("Unknown mode=" + mode);
		}
		events.lock.lock();
		try {
			events.add(handle);
		} finally {
			events.lock.unlock();
		}
		return () -> events.remove(handle);
	}

	// 事件派发。需要触发者在明确的地方显式的调用。

	// 启动新的线程执行。
	public void triggerThread(Object sender, EventArgument arg) {
		runThreadEvents.lock.lock();
		try {
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0, n = runThreadEvents.size(); i < n; i++) {
				var handle = runThreadEvents.get(i);
				Task.run(() -> handle.invoke(sender, arg), "EventDispatch." + name + ".runAsync");
			}
		} finally {
			runThreadEvents.lock.unlock();
			runThreadEvents.tryRemoveDelay();
		}
	}

	// 嵌入当前线程执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public synchronized void triggerEmbed(Object sender, EventArgument arg) throws Throwable {
		runEmbedEvents.lock.lock();
		try {
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0, n = runEmbedEvents.size(); i < n; i++) {
				var handle = runEmbedEvents.get(i);
				handle.invoke(sender, arg);
			}
		} finally {
			runEmbedEvents.lock.unlock();
			runEmbedEvents.tryRemoveDelay();
		}
	}

	// 在当前线程中，创建新的存储过程并执行，忽略所有错误。
	public synchronized void triggerProcedureIgnoreError(Application app, Object sender, EventArgument arg) {
		runProcedureEvents.lock.lock();
		try {
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0, n = runProcedureEvents.size(); i < n; i++) {
				var handle = runProcedureEvents.get(i);
				try {
					app.NewProcedure(() -> {
						handle.invoke(sender, arg);
						return 0L;
					}, "EventDispatcher.triggerProcedureIgnoreError").Call();
					// 返回错误码时是逻辑错误，这里不需要记录日志。内部已经记录了。
				} catch (Throwable ex) {
					logger.error(ex); // 除了框架错误，一般情况下，错误不会到达这里。
				}
			}
		} finally {
			runProcedureEvents.lock.unlock();
			runProcedureEvents.tryRemoveDelay();
		}
	}

	// 在当前线程中，创建新的存储过程并执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public synchronized void triggerProcedure(Application app, Object sender, EventArgument arg) throws Throwable {
		runProcedureEvents.lock.lock();
		try {
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0, n = runProcedureEvents.size(); i < n; i++) {
				var handle = runProcedureEvents.get(i);
				if (0L != app.NewProcedure(() -> {
					handle.invoke(sender, arg);
					return 0L;
				}, "EventDispatcher.triggerProcedure").Call())
					throw new RuntimeException("Nest Call Fail");
			}
		} finally {
			runProcedureEvents.lock.unlock();
			runProcedureEvents.tryRemoveDelay();
		}
	}
}
