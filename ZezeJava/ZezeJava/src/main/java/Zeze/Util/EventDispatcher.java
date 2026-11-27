package Zeze.Util;

import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Application;
import org.jspecify.annotations.Nullable;

/** 事件分发器：按事件注册处理器，支持嵌入/存储过程/线程三种派发模式并聚合结果码。 */
public class EventDispatcher {
	@FunctionalInterface
	public interface EventHandle {
		long invoke(Object sender, EventArgument arg) throws Exception;
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

	// 每次注册独立槽位，按对象身份判等移除：同时保证重复 cancel 幂等、移除精确槽位、不受 handle.equals 影响。
	private static final class Slot {
		final EventHandle handle;

		Slot(EventHandle handle) {
			this.handle = handle;
		}
	}

	private final String name;
	private final ConcurrentLinkedQueue<Slot> runEmbedEvents = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<Slot> runProcedureEvents = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<Slot> runThreadEvents = new ConcurrentLinkedQueue<>();

	public EventDispatcher(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	private ConcurrentLinkedQueue<Slot> getQueue(Mode mode) {
		return switch (mode) {
			case RunEmbed -> runEmbedEvents;
			case RunProcedure -> runProcedureEvents;
			case RunThread -> runThreadEvents;
		};
	}

	/**
	 * 注册事件处理函数，如果在事件触发过程中执行注册,则等下次再触发
	 *
	 * @return 如果需要取消注册，请保存返回值，并调用其cancel。
	 */
	public Canceler add(Mode mode, EventHandle handle) {
		var events = getQueue(mode);
		var slot = new Slot(handle);
		events.offer(slot);
		return () -> events.remove(slot);
	}

	// 事件派发。需要触发者在明确的地方显式的调用。

	// 启动新的线程执行。
	public void triggerThread(Application app, Object sender, EventArgument arg, @Nullable Object oneByOneKey) {
		var slots = runThreadEvents.toArray(Slot[]::new);
		if (oneByOneKey == null) {
			for (var slot : slots) {
				var handle = slot.handle;
				var actionName = "EventDispatch." + name + ".runAsync." + handle.getClass().getName();
				TaskSpec.ofProcedure(app.newProcedure(() -> handle.invoke(sender, arg), actionName)).run();
			}
		} else {
			for (var slot : slots) {
				var handle = slot.handle;
				var actionName = "EventDispatch." + name + ".runAsync." + handle.getClass().getName();
				TaskSpec.ofProcedure(app.newProcedure(
						() -> handle.invoke(sender, arg), actionName)).executeOneByOne(oneByOneKey);
			}
		}
	}

	// 嵌入当前线程执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public long triggerEmbed(Object sender, EventArgument arg) throws Exception {
		for (var slot : runEmbedEvents.toArray(Slot[]::new)) {
			var ret = slot.handle.invoke(sender, arg);
			if (ret != 0)
				return ret;
		}
		return 0;
	}

	// 在当前线程中，创建新的存储过程并嵌套执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
	public void triggerProcedure(Application app, Object sender, EventArgument arg) {
		for (var slot : runProcedureEvents.toArray(Slot[]::new)) {
			var handle = slot.handle;
			var actionName = "EventDispatcher." + name + ".triggerProcedure." + handle.getClass().getName();
			TaskSpec.ofProcedure(app.newProcedure(() -> handle.invoke(sender, arg), actionName)).call();
		}
	}
}
