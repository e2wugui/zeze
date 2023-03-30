package Zeze.Util;

import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Application;
import Zeze.Transaction.DispatchMode;
import org.jetbrains.annotations.NotNull;

public class EventDispatcher {
	// private static final Logger logger = LogManager.getLogger(EventDispatcher.class);

	@FunctionalInterface
	public interface EventHandle {
		long invoke(@NotNull Object sender, EventArgument arg) throws Exception;
	}

	public interface EventArgument {
	}

	public interface Canceler {
		void cancel();
	}

	public enum Mode {
		RunEmbed,
		RunProcedure,
		RunThread,
	}

	private final @NotNull String name;
	private final ConcurrentLinkedQueue<EventHandle> runEmbedEvents = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<EventHandle> runProcedureEvents = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<EventHandle> runThreadEvents = new ConcurrentLinkedQueue<>();

	public EventDispatcher(@NotNull String name) {
		this.name = name;
	}

	public @NotNull String getName() {
		return name;
	}

	public @NotNull ConcurrentLinkedQueue<EventHandle> getRunEmbedEvents() {
		return runEmbedEvents;
	}

	public @NotNull ConcurrentLinkedQueue<EventHandle> getRunProcedureEvents() {
		return runProcedureEvents;
	}

	public @NotNull ConcurrentLinkedQueue<EventHandle> getRunThreadEvents() {
		return runThreadEvents;
	}

	/**
	 * 注册事件处理函数，如果在事件触发过程中执行注册,则等下次再触发
	 *
	 * @return 如果需要取消注册，请保存返回值，并调用其cancel。
	 */
	public Canceler add(@NotNull Mode mode, @NotNull EventHandle handle) {
		ConcurrentLinkedQueue<EventHandle> events;
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
			throw new IllegalArgumentException("Unknown mode=" + mode);
		}
		events.offer(handle);
		return () -> events.remove(handle);
	}

	// 事件派发。需要触发者在明确的地方显式的调用。

	// 启动新的线程执行。
	public void triggerThread(@NotNull Object sender, EventArgument arg) {
		for (EventHandle handle : runThreadEvents)
			Task.run(() -> handle.invoke(sender, arg), "EventDispatch." + name + ".runAsync", DispatchMode.Normal);
	}

	// 嵌入当前线程执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public long triggerEmbed(@NotNull Object sender, EventArgument arg) throws Exception {
		for (EventHandle handle : runEmbedEvents) {
			var ret = handle.invoke(sender, arg);
			if (ret != 0)
				return ret;
		}
		return 0;
	}

	// 在当前线程中，创建新的存储过程并嵌套执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public void triggerProcedure(@NotNull Application app, Object sender, EventArgument arg) {
		for (EventHandle handle : runProcedureEvents) {
			// 忽略嵌套的存储的执行。
			Task.call(app.newProcedure(() -> {
				handle.invoke(sender, arg);
				return 0L;
			}, "EventDispatcher.triggerProcedure"));
		}
	}
}
