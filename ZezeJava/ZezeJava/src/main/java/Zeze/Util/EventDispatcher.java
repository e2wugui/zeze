package Zeze.Util;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventDispatcher {
	private String name;
	public String getName() {
		return name;
	}

	public EventDispatcher(String name) {
		this.name = name;
	}

	@FunctionalInterface
	public interface EventHandle {
		long invoke(Object sender, EventArgument arg) throws Throwable;
	}

	public static class EventArgument {
	}

	public enum Mode {
		RunEmbed,
		RunProcedure,
		RunThread,
	}

	public static class Events {
		private final ConcurrentHashMap<Long, EventHandle> handles = new ConcurrentHashMap<>();
		private AtomicLong nextId = new AtomicLong();

		public class Canceler {
			private long id;
			public Canceler(long id) {
				this.id = id;
			}
			public void cancel() {
				handles.remove(id);
			}
		}

		public Canceler add(EventHandle handle) {
			var next = nextId.incrementAndGet();
			if (handles.putIfAbsent(next, handle) != null) {
				throw new RuntimeException("Impossible!");
			}
			return new Canceler(next);
		}

		public Collection<EventHandle> values() {
			return this.handles.values();
		}
	}

	private Events runEmbedEvents = new Events();
	private Events runProcedureEvents = new Events();
	private Events runThreadEvents = new Events();

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
	 * 注册事件处理函数，
	 * @return 如果需要取消注册，请保存返回值，并调用其cancel。
 	 */
	public final Events.Canceler add(Mode mode, EventHandle handle) {
		switch (mode) {
		case RunEmbed:
			return runEmbedEvents.add(handle);

		case RunProcedure:
			return runProcedureEvents.add(handle);

		case RunThread:
			return runThreadEvents.add(handle);
		}
		throw new RuntimeException("Unknown mode=" + mode);
	}

	// 事件派发。需要触发者在明确的地方显式的调用。

	// 启动新的线程执行。
	public final void triggerThread(Object sender, EventArgument arg) {
		for (var handle : runThreadEvents.values()) {
			Task.run(() -> handle.invoke(sender, arg), "EventDispatch." + name + ".runAsync");
		}
	}

	static final Logger logger = LogManager.getLogger(EventDispatcher.class);
	// 嵌入当前线程执行，所有错误都报告出去，忽略所有错误。
	public final void triggerEmbedIgnoreError(Object sender, EventArgument arg) {
		for (var handle : runEmbedEvents.values()) {
			try {
				handle.invoke(sender, arg);
			} catch (Throwable ex) {
				logger.error(ex);
			}
		}
	}

	// 在当前线程中，创建新的存储过程并执行，忽略所有错误。
	public final void triggerProcedureIgnoreError(Application app, Object sender, EventArgument arg) {
		for (var handle : runProcedureEvents.values()) {
			try {
				app.NewProcedure(() -> {
					handle.invoke(sender, arg);
					return 0L;
				}, "").Call();
				// 返回错误码时是逻辑错误，这里不需要记录日志。内部已经记录了。
			} catch (Throwable ex) {
				logger.error(ex); // 除了框架错误，一般情况下，错误不会到达这里。
			}
		}
	}

	// 嵌入当前线程执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public final void triggerEmbed(Object sender, EventArgument arg) throws Throwable {
		for (var handle : runEmbedEvents.values()) {
			handle.invoke(sender, arg);
		}
	}

	// 在当前线程中，创建新的存储过程并执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public final void triggerProcedure(Application app, Object sender, EventArgument arg) throws Throwable {
		for (var handle : runProcedureEvents.values()) {
			if (0L != app.NewProcedure(() -> { handle.invoke(sender, arg); return 0L; }, "").Call())
				throw new RuntimeException("Nest Call Fail");
		}
	}

}
