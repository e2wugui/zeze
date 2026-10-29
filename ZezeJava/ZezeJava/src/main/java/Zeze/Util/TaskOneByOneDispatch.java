package Zeze.Util;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

/** 循环排空单桶的内联派发请求，避免续轮递归；每次 run 仍完整地处于执行器的装饰上下文内。 */
abstract class TaskOneByOneDispatch extends AtomicInteger implements Runnable {
	private static final ThreadLocal<@Nullable Context> current = new ThreadLocal<>();
	private final Object owner;
	final Executor target;

	TaskOneByOneDispatch(Object owner, Executor target) {
		this.owner = owner;
		this.target = target;
	}

	abstract void execute();

	final void submit(boolean continuation) {
		var previous = current.get();
		// 新的空桶认领保留原内联行为（包括取消回调重新提交）；只延后已有驱动的续轮。
		for (var context = continuation ? previous : null; context != null; context = context.parent) {
			if (context.owner == owner) {
				context.defer(this);
				return;
			}
		}
		// 不同桶保留执行器原有的内联语义，不能将其一起延后，否则桶间同步调用可能互等。
		// 延后的是 execute 请求而非 run：装饰执行器可以在 run 前后设置/清理线程上下文。
		var context = new Context(owner, previous);
		current.set(context);
		Throwable failure = null;
		try {
			for (TaskOneByOneDispatch next = this; next != null; next = context.poll()) {
				try {
					if (next.get() == 0) // 延后期间仍可被 cancelBeforeRun 作废。
						next.execute();
				} catch (Throwable e) {
					if (failure == null)
						failure = e;
					else if (failure != e)
						failure.addSuppressed(e);
				}
			}
		} finally {
			if (previous == null)
				current.remove();
			else
				current.set(previous);
		}
		if (failure != null)
			throw Task.forceThrow(failure);
	}

	private static final class Context {
		final Object owner;
		final @Nullable Context parent;
		private @Nullable ArrayDeque<TaskOneByOneDispatch> deferred;

		Context(Object owner, @Nullable Context parent) {
			this.owner = owner;
			this.parent = parent;
		}

		void defer(TaskOneByOneDispatch dispatch) {
			if (deferred == null)
				deferred = new ArrayDeque<>();
			deferred.addLast(dispatch);
		}

		@Nullable TaskOneByOneDispatch poll() {
			return deferred != null ? deferred.pollFirst() : null;
		}
	}
}
