package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 同TaskOneByOneByKey,只是用ConcurrentLinkedQueue代替ArrayDeque和锁.
 * 另外由于不用临界区,shutdown和加任务的并发很难做,所以暂时不支持shutdown,也不支持cancel了.
 */
public final class TaskOneByOneByKey2 extends ReentrantLock {
	private static final @NotNull Logger logger = LogManager.getLogger(TaskOneByOneByKey2.class);
	private static final @NotNull VarHandle vhSubmitted;

	static {
		try {
			vhSubmitted = MethodHandles.lookup().findVarHandle(TaskOneByOne.class, "submitted", boolean.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final @NotNull TaskOneByOne @NotNull [] concurrency;
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
		private static final @NotNull VarHandle vhCount;

		static {
			try {
				vhCount = MethodHandles.lookup().findVarHandle(Barrier.class, "count", int.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		private final ConcurrentHashSet<TaskOneByOne> reached = new ConcurrentHashSet<>();
		@SuppressWarnings("FieldMayBeFinal")
		private volatile int count;

		Barrier(int count) {
			this.count = count;
		}

		abstract @NotNull String getName();

		abstract void run() throws Exception;

		private void reachedRunNext() {
			for (var taskOneByOne : reached)
				taskOneByOne.runNext();
		}

		void reach(@NotNull TaskOneByOne taskOneByOne, int sum) {
			reached.add(taskOneByOne);
			if ((int)vhCount.getAndAdd(this, -sum) > sum)
				return;

			try {
				run();
			} catch (Throwable ex) { // logger.error
				logger.error("{} run exception:", getName(), ex);
			} finally {
				// 成功执行
				// 1. 触发所有桶的runNext，
				// 2. 自己也返回false，不再继续runNext。
				reachedRunNext();
			}
		}
	}

	static final class BarrierProcedure extends Barrier {
		private final @NotNull Procedure procedure;

		BarrierProcedure(@NotNull Procedure procedure, int count) {
			super(count);
			this.procedure = procedure;
		}

		@Override
		@NotNull String getName() {
			return procedure.getActionName();
		}

		@Override
		void run() {
			procedure.call();
		}
	}

	static final class BarrierAction extends Barrier {
		private final @NotNull Action0 action;
		private final @NotNull String actionName;

		BarrierAction(@NotNull String actionName, @NotNull Action0 action, int count) {
			super(count);
			this.action = action;
			this.actionName = actionName;
		}

		@Override
		@NotNull String getName() {
			return actionName;
		}

		@Override
		void run() throws Exception {
			action.run();
		}
	}

	public <T> void executeCyclicBarrier(@NotNull Collection<T> keys, @NotNull Procedure procedure,
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
			for (var e : group.entrySet()) {
				var sum = e.getValue().value;
				e.getKey().executeBarrier(barrier, sum, mode);
			}
		} finally {
			unlock();
		}
	}

	public <T> void executeCyclicBarrier(@NotNull Collection<T> keys, @NotNull String actionName,
										 @NotNull Action0 action, @Nullable DispatchMode mode) {
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
			for (var e : group.entrySet()) {
				var sum = e.getValue().value;
				e.getKey().executeBarrier(barrier, sum, mode);
			}
		} finally {
			unlock();
		}
	}

	public static class Batch<T> {
		private final @NotNull AtomicInteger keysCount;
		private final @NotNull Action1<T> action;
		private final @NotNull Action0 batchEnd;

		public Batch(int keysSize, @NotNull Action1<T> action, @NotNull Action0 batchEnd) {
			this.keysCount = new AtomicInteger(keysSize);
			this.action = action;
			this.batchEnd = batchEnd;
		}

		public void run(@NotNull T key) throws Exception {
			try {
				action.run(key);
			} finally {
				if (keysCount.decrementAndGet() == 0)
					batchEnd.run();
			}
		}
	}

	private static void runBatchEndDirect(@NotNull Action0 batchEnd) {
		try {
			batchEnd.run();
		} catch (Throwable e) { // logger.error
			logger.error("executeBatch: batchEnd exception", e);
		}
	}

	public <T> void executeBatch(@NotNull Collection<T> keys, @NotNull Action1<T> action, @NotNull Action0 batchEnd,
								 @Nullable DispatchMode mode) {
		if (keys.isEmpty()) {
			runBatchEndDirect(batchEnd);
			return;
		}
		var batch = new Batch<>(keys.size(), action, batchEnd);
		for (var key : keys)
			executeCore(key.hashCode(), new TaskBody.OfAction(() -> batch.run(key)), null, mode);
	}

	public void executeBatch(@NotNull LongList keys, @NotNull Action1<Long> action, @NotNull Action0 batchEnd,
							 @Nullable DispatchMode mode) {
		if (keys.isEmpty()) {
			runBatchEndDirect(batchEnd);
			return;
		}
		var batch = new Batch<>(keys.size(), action, batchEnd);
		keys.foreach((key) -> executeCore(Long.hashCode(key), new TaskBody.OfAction(() -> batch.run(key)), null, mode));
	}

	// 统一核心：所有 Execute 重载与 TaskSpec.executeOneByOne 最终都委托到这里。
	// 载荷差异（调用形态/名字解析）由 TaskBody 封装，队列语义无返回值消费者。
	void executeCore(int key, @NotNull TaskBody<?> body, @Nullable String name, @Nullable DispatchMode mode) {
		concurrency[hash(key) & hashMask].execute(body, name, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Action0 action) {
		executeCore(key.hashCode(), new TaskBody.OfAction(action), null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		executeCore(key.hashCode(), new TaskBody.OfAction(action), null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name) {
		executeCore(key.hashCode(), new TaskBody.OfAction(action), name, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name,
						@Nullable DispatchMode mode) {
		executeCore(key.hashCode(), new TaskBody.OfAction(action), name, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Func0<?> func) {
		executeCore(key.hashCode(), new TaskBody.OfFunc0<>(func), null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Func0<?> func, @Nullable DispatchMode mode) {
		executeCore(key.hashCode(), new TaskBody.OfFunc0<>(func), null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Func0<?> func, @Nullable String name) {
		executeCore(key.hashCode(), new TaskBody.OfFunc0<>(func), name, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Func0<?> func, @Nullable String name,
						@Nullable DispatchMode mode) {
		executeCore(key.hashCode(), new TaskBody.OfFunc0<>(func), name, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Procedure procedure) {
		executeCore(key.hashCode(), new TaskBody.OfProcedure(procedure), null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		executeCore(key.hashCode(), new TaskBody.OfProcedure(procedure), null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Action0 action) {
		executeCore(key, new TaskBody.OfAction(action), null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		executeCore(key, new TaskBody.OfAction(action), null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Action0 action, @Nullable String name) {
		executeCore(key, new TaskBody.OfAction(action), name, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		executeCore(key, new TaskBody.OfAction(action), name, mode);
	}

	private @NotNull TaskOneByOne bucket(@NotNull Object key) {
		return concurrency[hash(key.hashCode()) & hashMask];
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Func0<?> func) {
		executeCore(key, new TaskBody.OfFunc0<>(func), null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Func0<?> func, @Nullable DispatchMode mode) {
		executeCore(key, new TaskBody.OfFunc0<>(func), null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Func0<?> func, @Nullable String name) {
		executeCore(key, new TaskBody.OfFunc0<>(func), name, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Func0<?> func, @Nullable String name, @Nullable DispatchMode mode) {
		executeCore(key, new TaskBody.OfFunc0<>(func), name, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Procedure procedure) {
		executeCore(key, new TaskBody.OfProcedure(procedure), null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(int key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		executeCore(key, new TaskBody.OfProcedure(procedure), null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Action0 action) {
		executeCore(Long.hashCode(key), new TaskBody.OfAction(action), null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		executeCore(Long.hashCode(key), new TaskBody.OfAction(action), null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Action0 action, @Nullable String name) {
		executeCore(Long.hashCode(key), new TaskBody.OfAction(action), name, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		executeCore(Long.hashCode(key), new TaskBody.OfAction(action), name, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Func0<?> func) {
		executeCore(Long.hashCode(key), new TaskBody.OfFunc0<>(func), null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Func0<?> func, @Nullable DispatchMode mode) {
		executeCore(Long.hashCode(key), new TaskBody.OfFunc0<>(func), null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Func0<?> func, @Nullable String name) {
		executeCore(Long.hashCode(key), new TaskBody.OfFunc0<>(func), name, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Func0<?> func, @Nullable String name, @Nullable DispatchMode mode) {
		executeCore(Long.hashCode(key), new TaskBody.OfFunc0<>(func), name, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Procedure procedure) {
		executeCore(Long.hashCode(key), new TaskBody.OfProcedure(procedure), null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofFunc0/ofProcedure 工厂 + 链式 setter + executeOneByOne(key, taskOneByOneByKey2) 终结方法（key 重载 Object/int/long）。 */
	@Deprecated
	public void Execute(long key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		executeCore(Long.hashCode(key), new TaskBody.OfProcedure(procedure), null, mode);
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		for (int i = 0; i < concurrency.length; i++) {
			var s = concurrency[i].toString();
			if (s.length() > 2)
				sb.append(i).append(": ").append(s).append('\n');
		}
		return sb.toString();
	}

	/**
	 * Applies a supplemental hash function to a given hashCode, which defends
	 * against poor quality hash functions. This is critical because HashMap uses
	 * power-of-two length hash tables, that otherwise encounter collisions for
	 * hashCodes that do not differ in lower bits. Note: Null keys always map to
	 * hash 0, thus index 0.
	 *
	 * @see HashMap
	 */
	private static int hash(int _h) {
		int h = _h;
		h ^= (h >>> 20) ^ (h >>> 12);
		return (h ^ (h >>> 7) ^ (h >>> 4));
	}

	private @NotNull Executor getExecutor(@Nullable DispatchMode mode) {
		return executor != null ? executor : (mode == DispatchMode.Critical
				? Zeze.Util.Task.getCriticalThreadPool()
				: Zeze.Util.Task.getThreadPool());
	}

	static abstract class Task {
		final @NotNull String name;
		final @Nullable DispatchMode mode;

		Task(@NotNull String name, @Nullable DispatchMode mode) {
			this.name = name;
			this.mode = mode;
		}

		abstract boolean process();
	}

	/**
	 * 统一载荷任务：用 {@link TaskBody} 覆盖 action/func/procedure/func0 四种载荷。
	 * 队列语义无返回值消费者，callRaw 的结果丢弃；名字解析见 {@link TaskBody#logName}。
	 */
	static final class TaskBodyTask extends Task {
		private final @NotNull TaskBody<?> body;

		TaskBodyTask(@NotNull TaskBody<?> body, @Nullable String name, @Nullable DispatchMode mode) {
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
			private final @NotNull BarrierProcedure barrier;
			private final int sum;

			TaskBarrierProcedure(@NotNull BarrierProcedure barrier, int sum, @Nullable DispatchMode mode) {
				super(barrier.getName(), mode);
				this.barrier = barrier;
				this.sum = sum;
			}

			@Override
			boolean process() {
				barrier.reach(TaskOneByOne.this, sum);
				return false;
			}
		}

		final class TaskBarrierAction extends Task {
			private final @NotNull BarrierAction barrier;
			private final int sum;

			TaskBarrierAction(@NotNull BarrierAction barrier, int sum, @Nullable DispatchMode mode) {
				super(barrier.actionName, mode);
				this.barrier = barrier;
				this.sum = sum;
			}

			@Override
			boolean process() {
				barrier.reach(TaskOneByOne.this, sum);
				return false;
			}
		}

		private final ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>();
		private volatile boolean submitted;

		void execute(@NotNull TaskBody<?> body, @Nullable String name, @Nullable DispatchMode mode) {
			submit(new TaskBodyTask(body, name, mode));
		}

		void executeBarrier(@NotNull BarrierProcedure barrier, int sum, @Nullable DispatchMode mode) {
			submit(new TaskBarrierProcedure(barrier, sum, mode));
		}

		void executeBarrier(@NotNull BarrierAction barrier, int sum, @Nullable DispatchMode mode) {
			submit(new TaskBarrierAction(barrier, sum, mode));
		}

		private void submit(@NotNull Task task) {
			queue.offer(task);
			if ((boolean)vhSubmitted.compareAndSet(this, false, true))
				runNext();
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
			var task = peekTask();
			if (task != null)
				getExecutor(task.mode).execute(this);
		}

		@Override
		public void run() {
			var task = pollTask();
			if (task == null)
				return;
			for (var mode = task.mode; ; queue.poll()) {
				if (!task.process())
					return; // 稍后通过runNext继续执行
				task = peekTask();
				if (task == null)
					return;
				if (task.mode != mode) {
					getExecutor(task.mode).execute(this);
					return;
				}
			}
		}

		@Override
		public @NotNull String toString() {
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
