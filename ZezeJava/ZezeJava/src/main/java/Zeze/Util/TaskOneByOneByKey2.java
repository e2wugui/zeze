package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 按 key 分桶的无锁串行任务队列：与 {@link TaskOneByOneByKey} 类似，使用 {@link ConcurrentLinkedQueue}
 * 替代锁与数组队列，不支持 shutdown 与外部 cancel。
 */
public final class TaskOneByOneByKey2 extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger(TaskOneByOneByKey2.class);
	private static final VarHandle vhSubmitted;
	private static final ConcurrentHashMap<TaskOneByOne.Dispatch, Executor> pendingDispatches = new ConcurrentHashMap<>();

	static {
		try {
			vhSubmitted = MethodHandles.lookup().findVarHandle(TaskOneByOne.class, "submitted", boolean.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final TaskOneByOne [] concurrency;
	private final int hashMask;
	private final @Nullable Executor executor;

	public TaskOneByOneByKey2(@Nullable Executor executor) {
		this(1024, executor);
	}

	public TaskOneByOneByKey2() {
		this(1024, null);
	}

	public TaskOneByOneByKey2(int concurrencyLevel) {
		this(concurrencyLevel, null);
	}

	public TaskOneByOneByKey2(int concurrencyLevel, @Nullable Executor executor) {
		this.executor = executor;
		if (concurrencyLevel < 1 || concurrencyLevel > 0x4000_0000)
			throw new IllegalArgumentException("Illegal concurrencyLevel: " + concurrencyLevel);

		int capacity = 1;
		while (capacity < concurrencyLevel)
			capacity <<= 1;
		concurrency = new TaskOneByOne[capacity];
		for (int i = 0; i < concurrency.length; i++)
			concurrency[i] = new TaskOneByOne();
		hashMask = capacity - 1;
	}

	public int getConcurrencyLevel() {
		return concurrency.length;
	}

	public int getQueueSize(int index) {
		return Integer.compareUnsigned(index, concurrency.length) < 0 ? concurrency[index].queue.size() : -1; // 可能有并发问题导致结果不准确,但通常问题不大
	}

	static abstract class Barrier {
		private enum State { Waiting, Running, Finished, Canceled }

		private final HashSet<TaskOneByOne> reached = new HashSet<>();
		private int count;
		private State state = State.Waiting; // count/reached/state 均由 synchronized(this) 守护。

		Barrier(int count) {
			this.count = count;
		}

		abstract String getName();

		abstract void run() throws Exception;

		// 锁内快照清空保证仅推进一次，锁外调用 runNext
		private void reachedRunNext() {
			TaskOneByOne[] batches;
			synchronized (this) {
				if (reached.isEmpty())
					return;
				batches = reached.toArray(new TaskOneByOne[0]);
				reached.clear();
			}
			Throwable failure = null;
			for (var taskOneByOne : batches) {
				try {
					taskOneByOne.runNext();
				} catch (Throwable e) {
					if (failure == null)
						failure = e;
					else if (failure != e)
						failure.addSuppressed(e);
				}
			}
			if (failure != null)
				throw Zeze.Util.Task.forceThrow(failure);
		}

		/** @return true 表示屏障已取消可继续消费；false 表示挂起等待屏障放行 */
		boolean reach(TaskOneByOne taskOneByOne, int sum) {
			synchronized (this) {
				if (state != State.Waiting)
					return true;
				reached.add(taskOneByOne);
				count -= sum;
				if (count > 0)
					return false;
				state = State.Running;
			}

			try {
				run(); // 锁外执行，避免回调提交新任务引发锁序反转
			} catch (Throwable ex) { // logger.error
				logger.error("{} run exception:", getName(), ex);
			} finally {
				synchronized (this) {
					state = State.Finished;
				}
				reachedRunNext();
			}
			return false;
		}

		// 桶派发失败时取消屏障并解冻已到达的桶，不重抛以免掩盖原始异常
		void cancel() {
			synchronized (this) {
				if (state != State.Waiting)
					return;
				state = State.Canceled;
			}
			try {
				reachedRunNext();
			} catch (Throwable ex) { // logger.error
				logger.error("{} cancel exception:", getName(), ex);
			}
		}
	}

	static final class BarrierProcedure extends Barrier {
		private final Procedure procedure;

		BarrierProcedure(Procedure procedure, int count) {
			super(count);
			this.procedure = procedure;
		}

		@Override
		String getName() {
			return procedure.getActionName();
		}

		@Override
		void run() {
			procedure.call();
		}
	}

	static final class BarrierAction extends Barrier {
		private final Action0 action;
		private final String actionName;

		BarrierAction(String actionName, Action0 action, int count) {
			super(count);
			this.action = action;
			this.actionName = actionName;
		}

		@Override
		String getName() {
			return actionName;
		}

		@Override
		void run() throws Exception {
			action.run();
		}
	}

	public <T> void executeCyclicBarrier(Collection<T> keys, Procedure procedure,
										 @Nullable DispatchMode mode) {
		lock();
		try {
			if (keys.isEmpty())
				throw new IllegalArgumentException("CyclicBarrier keys is empty.");

			var group = new HashMap<TaskOneByOne, OutInt>();
			int count = 0;
			for (var key : keys) {
				group.computeIfAbsent(bucket(key), __ -> new OutInt()).value++;
				count++;
			}
			var barrier = new BarrierProcedure(procedure, count);
			try {
				for (var e : group.entrySet()) {
					var sum = e.getValue().value;
					e.getKey().executeBarrier(barrier, sum, mode);
				}
			} catch (Throwable ex) {
				barrier.cancel();
				throw ex;
			}
		} finally {
			unlock();
		}
	}

	public <T> void executeCyclicBarrier(Collection<T> keys, String actionName,
										 Action0 action, @Nullable DispatchMode mode) {
		lock();
		try {
			if (keys.isEmpty())
				throw new IllegalArgumentException("CyclicBarrier keys is empty.");

			var group = new HashMap<TaskOneByOne, OutInt>();
			int count = 0;
			for (var key : keys) {
				group.computeIfAbsent(bucket(key), __ -> new OutInt()).value++;
				count++;
			}
			var barrier = new BarrierAction(actionName, action, count);
			try {
				for (var e : group.entrySet()) {
					var sum = e.getValue().value;
					e.getKey().executeBarrier(barrier, sum, mode);
				}
			} catch (Throwable ex) {
				barrier.cancel();
				throw ex;
			}
		} finally {
			unlock();
		}
	}

	public static class Batch<T> {
		private final AtomicInteger keysCount;
		private final Action1<T> action;
		private final Action0 batchEnd;

		public Batch(int keysSize, Action1<T> action, Action0 batchEnd) {
			this.keysCount = new AtomicInteger(keysSize);
			this.action = action;
			this.batchEnd = batchEnd;
		}

		/** 每个 key 至多核销一次（正常完成、未入队或批量中止共用）。 */
		public final class Settle implements Action0 {
			private final AtomicBoolean settled = new AtomicBoolean();

			@Override
			public void run() {
				if (!settled.compareAndSet(false, true))
					return;
				if (keysCount.decrementAndGet() == 0)
					runBatchEndDirect(batchEnd);
			}
		}

		public Settle newSettle() {
			return new Settle();
		}

		public void run(T key, Settle settle) throws Exception {
			try {
				action.run(key);
			} finally {
				settle.run();
			}
		}

		/** 提交循环中断时，未尝试key的一次性核销。 */
		public void abandon(int count) {
			if (count > 0 && keysCount.addAndGet(-count) == 0)
				runBatchEndDirect(batchEnd);
		}
	}

	private static void runBatchEndDirect(Action0 batchEnd) {
		try {
			batchEnd.run();
		} catch (Throwable e) { // logger.error
			logger.error("executeBatch: batchEnd exception", e);
		}
	}

	public <T> void executeBatch(Collection<T> keys, Action1<T> action, Action0 batchEnd,
								 @Nullable DispatchMode mode) {
		if (keys.isEmpty()) {
			runBatchEndDirect(batchEnd);
			return;
		}
		var batch = new Batch<>(keys.size(), action, batchEnd);
		var remaining = keys.size();
		for (var key : keys) {
			remaining--;
			var settle = batch.newSettle();
			try {
				executeCore(key.hashCode(), new TaskBody.OfAction(() -> batch.run(key, settle)), null, mode, settle);
			} catch (Throwable ex) {
				batch.abandon(remaining);
				throw ex;
			}
		}
	}

	public void executeBatch(LongList keys, Action1<Long> action, Action0 batchEnd,
							 @Nullable DispatchMode mode) {
		if (keys.isEmpty()) {
			runBatchEndDirect(batchEnd);
			return;
		}
		var batch = new Batch<>(keys.size(), action, batchEnd);
		final var remaining = new int[]{keys.size()};
		keys.foreach((key) -> {
			remaining[0]--;
			var settle = batch.newSettle();
			try {
				executeCore(Long.hashCode(key), new TaskBody.OfAction(() -> batch.run(key, settle)), null, mode, settle);
			} catch (Throwable ex) {
				batch.abandon(remaining[0]);
				throw ex;
			}
		});
	}

	void executeCore(int key, TaskBody<?> body, @Nullable String name, @Nullable DispatchMode mode) {
		concurrency[hash(key) & hashMask].execute(body, name, mode);
	}

	void executeCore(int key, TaskBody<?> body, @Nullable String name, @Nullable DispatchMode mode,
					 @Nullable Action0 notEnqueued) {
		concurrency[hash(key) & hashMask].execute(body, name, mode, notEnqueued);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Action0 action) {
		TaskSpec.ofAction(action).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Action0 action, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Action0 action, @Nullable String name,
						@Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Func0<?> func) {
		TaskSpec.ofFunc0(func).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Func0<?> func, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc0(func).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Func0<?> func, @Nullable String name) {
		TaskSpec.ofFunc0(func).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Func0<?> func, @Nullable String name,
						@Nullable DispatchMode mode) {
		TaskSpec.ofFunc0(func).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Procedure procedure) {
		TaskSpec.ofProcedure(procedure).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Action0 action) {
		TaskSpec.ofAction(action).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Action0 action, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	private TaskOneByOne bucket(Object key) {
		return concurrency[hash(key.hashCode()) & hashMask];
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Func0<?> func) {
		TaskSpec.ofFunc0(func).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Func0<?> func, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc0(func).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Func0<?> func, @Nullable String name) {
		TaskSpec.ofFunc0(func).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Func0<?> func, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc0(func).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Procedure procedure) {
		TaskSpec.ofProcedure(procedure).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Action0 action) {
		TaskSpec.ofAction(action).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Action0 action, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Func0<?> func) {
		TaskSpec.ofFunc0(func).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Func0<?> func, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc0(func).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Func0<?> func, @Nullable String name) {
		TaskSpec.ofFunc0(func).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Func0<?> func, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc0(func).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Procedure procedure) {
		TaskSpec.ofProcedure(procedure).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (int i = 0; i < concurrency.length; i++) {
			var s = concurrency[i].toString();
			if (s.length() > 2)
				sb.append(i).append(": ").append(s).append('\n');
		}
		return sb.toString();
	}

	/** 扰动哈希，降低 2 的幂次桶数组低位冲突。 */
	private static int hash(int _h) {
		int h = _h;
		h ^= (h >>> 20) ^ (h >>> 12);
		return (h ^ (h >>> 7) ^ (h >>> 4));
	}

	private Executor getExecutor(@Nullable DispatchMode mode) {
		return executor != null ? executor
				: Zeze.Util.Task.poolOrThrow(mode == DispatchMode.Critical);
	}

	static void cancelPendingDispatches(Executor executor) {
		for (var entry : pendingDispatches.entrySet()) {
			if (entry.getValue() == executor)
				entry.getKey().cancelBeforeRun();
		}
	}

	static abstract class Task {
		final String name;
		final @Nullable DispatchMode mode;

		Task(String name, @Nullable DispatchMode mode) {
			this.name = name;
			this.mode = mode;
		}

		abstract boolean process();
	}

	/** 统一任务载荷封装，丢弃返回值。 */
	static final class TaskBodyTask extends Task {
		private final TaskBody<?> body;

		TaskBodyTask(TaskBody<?> body, @Nullable String name, @Nullable DispatchMode mode) {
			super(body.logName(name), mode);
			this.body = body;
		}

		@Override
		boolean process() {
			try {
				body.callRaw();
			} catch (Throwable e) { // logger.error
				logger.error("TaskBodyTask run exception: {}", name, e);
			}
			return true;
		}
	}

	final class TaskOneByOne implements Runnable {
		final class TaskBarrierProcedure extends Task {
			private final BarrierProcedure barrier;
			private final int sum;

			TaskBarrierProcedure(BarrierProcedure barrier, int sum, @Nullable DispatchMode mode) {
				super(barrier.getName(), mode);
				this.barrier = barrier;
				this.sum = sum;
			}

			@Override
			boolean process() {
				return barrier.reach(TaskOneByOne.this, sum);
			}
		}

		final class TaskBarrierAction extends Task {
			private final BarrierAction barrier;
			private final int sum;

			TaskBarrierAction(BarrierAction barrier, int sum, @Nullable DispatchMode mode) {
				super(barrier.actionName, mode);
				this.barrier = barrier;
				this.sum = sum;
			}

			@Override
			boolean process() {
				return barrier.reach(TaskOneByOne.this, sum);
			}
		}

		private final ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>();
		private volatile boolean submitted;

		void execute(TaskBody<?> body, @Nullable String name, @Nullable DispatchMode mode) {
			submit(new TaskBodyTask(body, name, mode));
		}

		void execute(TaskBody<?> body, @Nullable String name, @Nullable DispatchMode mode,
					 @Nullable Action0 notEnqueued) {
			submit(new TaskBodyTask(body, name, mode), notEnqueued);
		}

		void executeBarrier(BarrierProcedure barrier, int sum, @Nullable DispatchMode mode) {
			submit(new TaskBarrierProcedure(barrier, sum, mode));
		}

		void executeBarrier(BarrierAction barrier, int sum, @Nullable DispatchMode mode) {
			submit(new TaskBarrierAction(barrier, sum, mode));
		}

		private void submit(Task task) {
			submit(task, null);
		}

		/** notEnqueued：入队前校验失败时的核销回调（executeBatch 用）。 */
		private void submit(Task task, @Nullable Action0 notEnqueued) {
			// 入队前校验执行器可用性，避免无效任务入队
			try {
				getExecutor(task.mode);
			} catch (Throwable e) {
				if (notEnqueued != null) {
					try {
						notEnqueued.run();
					} catch (Throwable ex) { // logger.error
						logger.error("executeBatch: notEnqueued exception", ex);
					}
				}
				throw e;
			}

			queue.offer(task);
			if ((boolean)vhSubmitted.compareAndSet(this, false, true))
				runNext(false);
		}

		private @Nullable Task pollTask() {
			for (; ; ) {
				var task = queue.poll();
				if (task != null)
					return task;
				submitted = false;
				task = queue.peek();
				if (task == null || !(boolean)vhSubmitted.compareAndSet(this, false, true))
					return null;
			}
		}

		// 并发生产者可能在两次读取之间入队
		@SuppressWarnings("ConstantConditions")
		private @Nullable Task peekTask() {
			for (; ; ) {
				var task = queue.peek();
				if (task != null)
					return task;
				submitted = false;
				task = queue.peek();
				if (task == null || !(boolean)vhSubmitted.compareAndSet(this, false, true))
					return null;
			}
		}

		void runNext() {
			runNext(true);
		}

		private void runNext(boolean continuation) {
			var task = peekTask();
			if (task != null)
				executeOrRollback(task, continuation);
		}

		/** 派发失败时回滚 submitted 认领并重抛，保留队列任务待后续提交重新驱动。 */
		private void executeOrRollback(Task task, boolean continuation) {
			Dispatch dispatch = null;
			try {
				var target = getExecutor(task.mode);
				dispatch = new Dispatch(target, task.name);
				pendingDispatches.put(dispatch, target);
				dispatch.submit(continuation);
			} catch (Throwable e) {
				rollbackFailedDispatch(dispatch, task.name, e);
				throw e;
			}
		}

		private void rollbackFailedDispatch(@Nullable Dispatch dispatch, String name, Throwable failure) {
			boolean rolledBack;
			if (dispatch == null) {
				submitted = false;
				rolledBack = true;
			} else
				rolledBack = dispatch.cancelBeforeRun();
			if (rolledBack)
				logger.warn("TaskOneByOneByKey2: dispatch rejected, rollback claim, {} pending task(s), head: [{}]",
						queue.size(), name, failure);
		}

		// 单次派发票据，保证同一票据至多运行或回滚一次
		private final class Dispatch extends TaskOneByOneDispatch {
			private final String name;

			Dispatch(Executor target, String name) {
				super(TaskOneByOne.this, target);
				this.name = name;
			}

			@Override
			void execute() {
				try {
					target.execute(this);
				} catch (Throwable e) {
					rollbackFailedDispatch(this, name, e);
					throw e;
				}
			}

			@Override
			public void run() {
				if (!compareAndSet(0, 1))
					return;
				pendingDispatches.remove(this);
				TaskOneByOne.this.run();
			}

			boolean cancelBeforeRun() {
				if (!compareAndSet(0, 2))
					return false;
				pendingDispatches.remove(this);
				submitted = false;
				return true;
			}
		}

		@Override
		public void run() {
			var task = pollTask();
			if (task == null)
				return;
			for (var mode = task.mode; ; ) {
				if (!task.process())
					return; // 稍后通过runNext继续执行
				var next = peekTask();
				if (next == null)
					return;
				if (next.mode != mode) {
					executeOrRollback(next, true);
					return;
				}
				// 必须先 poll 获取所有权再处理，避免与屏障续轮并发驱动碰撞导致重复或丢失任务
				task = pollTask();
				if (task == null)
					return;
			}
		}

		@Override
		public String toString() {
			var sb = new StringBuilder().append('[');
			for (var task : queue)
				sb.append(task.name).append(',');
			int n = sb.length();
			if (n > 1)
				sb.setLength(n - 1);
			return sb.append(']').toString();
		}
	}
}
