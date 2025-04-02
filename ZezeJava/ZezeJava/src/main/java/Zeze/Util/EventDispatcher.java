package Zeze.Util;

import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Application;
import Zeze.Transaction.DispatchMode;
import org.jetbrains.annotations.NotNull;

public class EventDispatcher {
	// private static final @NotNull Logger logger = LogManager.getLogger(EventDispatcher.class);

	@FunctionalInterface
	public interface EventHandle {
		long invoke(@NotNull Object sender, @NotNull EventArgument arg) throws Exception;
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

	static class HandleClass {
		final @NotNull Object classNameOrHandle; // EventHandle handle || String className

		HandleClass(@NotNull EventHandle handle) {
			this.classNameOrHandle = handle;
		}

		HandleClass(@NotNull String className) {
			this.classNameOrHandle = className;
		}
	}

	private final @NotNull Application zeze;
	private final @NotNull String name;
	private final ConcurrentLinkedQueue<HandleClass> runEmbedEvents = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<HandleClass> runProcedureEvents = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<HandleClass> runThreadEvents = new ConcurrentLinkedQueue<>();

	public EventDispatcher(@NotNull Application zeze, @NotNull String name) {
		this.zeze = zeze;
		this.name = name;
	}

	public @NotNull String getName() {
		return name;
	}

	private @NotNull ConcurrentLinkedQueue<HandleClass> getQueue(@NotNull Mode mode) {
		switch (mode) {
		case RunEmbed:
			return runEmbedEvents;
		case RunProcedure:
			return runProcedureEvents;
		case RunThread:
			return runThreadEvents;
		default:
			throw new IllegalArgumentException("Unknown mode=" + mode);
		}
	}

	/**
	 * 注册事件处理函数，如果在事件触发过程中执行注册,则等下次再触发
	 *
	 * @return 如果需要取消注册，请保存返回值，并调用其cancel。
	 */
	public @NotNull Canceler addHot(@NotNull Mode mode, @NotNull Class<? extends EventHandle> handleClass) {
		var events = getQueue(mode);
		var handle = new HandleClass(handleClass.getName());
		events.offer(handle);
		return () -> events.remove(handle);
	}

	/**
	 * 注册事件处理函数，如果在事件触发过程中执行注册,则等下次再触发
	 *
	 * @return 如果需要取消注册，请保存返回值，并调用其cancel。
	 */
	public @NotNull Canceler add(@NotNull Mode mode, @NotNull EventHandle handle_) {
		zeze.verifyCallerCold(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		var events = getQueue(mode);
		var handle = new HandleClass(handle_);
		events.offer(handle);
		return () -> events.remove(handle);
	}

	// 事件派发。需要触发者在明确的地方显式的调用。

	// 启动新的线程执行。
	public void triggerThread(@NotNull Object sender, @NotNull EventArgument arg) {
		for (var handle : runThreadEvents) {
			var classNameOrHandle = handle.classNameOrHandle;
			if (classNameOrHandle instanceof EventHandle) {
				Task.run(() -> ((EventHandle)classNameOrHandle).invoke(sender, arg),
						"EventDispatch." + name + ".runAsync", DispatchMode.Normal);
			} else {
				var className = (String)classNameOrHandle;
				Task.run(() -> zeze.getHotHandle().findHandle(zeze, className).invoke(sender, arg),
						"EventDispatch." + name + ".runAsync " + className, DispatchMode.Normal);
			}
		}
	}

	// 嵌入当前线程执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public long triggerEmbed(@NotNull Object sender, EventArgument arg) throws Exception {
		for (var handle : runEmbedEvents) {
			var classNameOrHandle = handle.classNameOrHandle;
			var ret = classNameOrHandle instanceof EventHandle
					? ((EventHandle)classNameOrHandle).invoke(sender, arg)
					: zeze.getHotHandle().findHandle(zeze, (String)classNameOrHandle).invoke(sender, arg);
			if (ret != 0)
				return ret;
		}
		return 0;
	}

	// 在当前线程中，创建新的存储过程并嵌套执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public void triggerProcedure(@NotNull Application app, @NotNull Object sender, @NotNull EventArgument arg) {
		for (var handle : runProcedureEvents) {
			var classNameOrHandle = handle.classNameOrHandle;
			if (classNameOrHandle instanceof EventHandle) {
				Task.call(app.newProcedure(() -> {
					((EventHandle)classNameOrHandle).invoke(sender, arg); // 忽略嵌套的存储的执行。
					return 0L;
				}, "EventDispatcher.triggerProcedure"));
			} else {
				var className = (String)classNameOrHandle;
				Task.call(app.newProcedure(() -> {
					zeze.getHotHandle().findHandle(zeze, className).invoke(sender, arg); // 忽略嵌套的存储的执行。
					return 0L;
				}, "EventDispatcher.triggerProcedure." + className));
			}
		}
	}
}
