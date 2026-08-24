package Zeze.Util;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 任务提交参数的统一描述，用来替代 {@link Task} 与 TaskOneByOne 家族的大量重载。
 * 工厂收载荷，setter 收任务属性，终结方法定执行方式：
 *
 * <pre>
 * TaskSpec.ofAction(() -> doSomething())              // 载荷（4 种工厂）
 *         .name("MyTask")                             // 任务属性（链式 setter）
 *         .dispatchMode(DispatchMode.Critical)
 *         .run();                                     // 执行方式（终结方法）
 * TaskSpec.ofAction(this::logout).executeOneByOne(account, oneByOne); // key+queue 是终结方法参数
 * </pre>
 *
 * 载荷与异常策略：
 * <ul>
 * <li>ofAction：Action0，异常吞掉只记日志，结果归一为 Long(0)；</li>
 * <li>ofFunc：FuncLong，返回 long 结果，异常翻错误码并走结果日志；</li>
 * <li>ofProcedure：Procedure，同 ofFunc，日志名固定使用 procedure.getActionName()（{@link #name} 对它无效）；</li>
 * <li>ofFunc0：Func0&lt;R&gt;，返回值与异常都经 Future 传播（call() 时直接抛给调用者）。</li>
 * </ul>
 *
 * 动词约定：无标记动词 = 事务感知（在运行中的事务内延迟到提交后执行/注册，rollback 不执行）；
 * Now 后缀 = 不等事务提交，立即入池/注册（scheduleNow 的 "now" 指注册不等提交，delay 后触发照旧）。
 * OneByOne 天然立即语义，故 executeOneByOne 无事务感知变体。
 *
 * <p>spec 实例是一次性的、非线程安全：任何终结方法执行后实例失效，再调 setter/终结方法抛
 * {@link IllegalStateException}。
 *
 * <p>fail-fast 校验（终结方法执行时）：
 * <ul>
 * <li>{@link #call} 显式设置过 dispatchMode/timeout/onCancel → IllegalArgumentException；</li>
 * <li>schedule 族显式设置过 dispatchMode/onCancel → IllegalArgumentException；</li>
 * <li>executeOneByOne 显式设置过 timeout → IllegalArgumentException；
 *     onCancel 仅 base 家族（含全局队列）支持，Key2 + onCancel → IllegalArgumentException。</li>
 * </ul>
 */
public final class TaskSpec<R> {
	// 4 个载荷字段有且仅有一个非空，终结方法按判空分派。
	private final @Nullable Action0 action; // 异常吞掉只记日志，结果归一为 Long(0)
	private final @Nullable FuncLong func; // 返回 long 结果，异常翻错误码并走结果日志
	private final @Nullable Procedure procedure; // 同 FuncLong，日志名固定使用 getActionName()
	private final @Nullable Func0<R> func0; // 返回值与异常都经 Future 传播

	private boolean consumed;
	private @Nullable String name;
	private @Nullable DispatchMode dispatchMode; // null 等同 Normal
	private long timeout = -1; // 哨兵值：<0 表示未设置，终结方法内取 Task.defaultTimeout
	private @Nullable Action0 onCancel; // 队列 shutdown(true) 丢弃本任务时的补偿回调；TaskOneByOneByKey2 不支持
	private boolean dispatchModeSet;
	private boolean timeoutSet;
	private boolean onCancelSet;

	private TaskSpec(@Nullable Action0 action, @Nullable FuncLong func,
	                 @Nullable Procedure procedure, @Nullable Func0<R> func0) {
		this.action = action;
		this.func = func;
		this.procedure = procedure;
		this.func0 = func0;
	}

	/**
	 * 普通无返回值任务（异常吞掉只记日志），结果归一为 Long(0)。
	 *
	 * @param action 不能为空
	 */
	public static @NotNull TaskSpec<Long> ofAction(@NotNull Action0 action) {
		return new TaskSpec<>(Objects.requireNonNull(action), null, null, null);
	}

	/**
	 * 返回 long 结果的任务（如返回错误码的处理器），异常翻错误码并走结果日志。
	 *
	 * @param func 不能为空
	 */
	public static @NotNull TaskSpec<Long> ofFunc(@NotNull FuncLong func) {
		return new TaskSpec<>(null, Objects.requireNonNull(func), null, null);
	}

	/**
	 * 带返回值的任务，返回值与异常都经 Future 传播（区别于 ofAction 的异常吞掉只打日志）。
	 *
	 * @param func 不能为空
	 */
	public static <R> @NotNull TaskSpec<R> ofFunc0(@NotNull Func0<R> func) {
		return new TaskSpec<>(null, null, null, Objects.requireNonNull(func));
	}

	/**
	 * 存储过程任务。日志名固定使用 procedure.getActionName()（{@link #name} 对它无效）。
	 *
	 * @param procedure 不能为空
	 */
	public static @NotNull TaskSpec<Long> ofProcedure(@NotNull Procedure procedure) {
		return new TaskSpec<>(null, null, Objects.requireNonNull(procedure), null);
	}

	/**
	 * @param name 任务名，用于日志与统计，默认使用载荷的类名；Procedure 载荷固定使用 getActionName()，此设置无效
	 */
	public @NotNull TaskSpec<R> name(@Nullable String name) {
		checkNotConsumed();
		this.name = name;
		return this;
	}

	/**
	 * @param dispatchMode 异步分发目标（Normal=默认池 / Critical=关键池 / Direct=调用线程），null 等同 Normal；
	 *                     call() 与 schedule 族不消费它（显式设置会抛 IllegalArgumentException）
	 */
	public @NotNull TaskSpec<R> dispatchMode(@Nullable DispatchMode dispatchMode) {
		checkNotConsumed();
		this.dispatchMode = dispatchMode;
		this.dispatchModeSet = true;
		return this;
	}

	/**
	 * @param timeout 任务超时(毫秒)，不设置或小于0时，终结方法执行时取 Task.defaultTimeout；
	 *                call() 与 executeOneByOne 族不消费它（显式设置会抛 IllegalArgumentException）
	 */
	public @NotNull TaskSpec<R> timeout(long timeout) {
		checkNotConsumed();
		this.timeout = timeout;
		this.timeoutSet = true;
		return this;
	}

	/**
	 * @param onCancel 队列 shutdown(true) 丢弃本任务时的补偿回调，可为空；
	 *                 仅 executeOneByOne 的 base 家族（含全局队列）消费它
	 */
	public @NotNull TaskSpec<R> onCancel(@Nullable Action0 onCancel) {
		checkNotConsumed();
		this.onCancel = onCancel;
		this.onCancelSet = true;
		return this;
	}

	private void checkNotConsumed() {
		if (consumed)
			throw new IllegalStateException("TaskSpec instance is single-use and has been consumed");
	}

	private void consume() {
		checkNotConsumed();
		consumed = true;
	}

	private long timeoutOrDefault() {
		return timeout < 0 ? Task.defaultTimeout : timeout;
	}

	private @NotNull DispatchMode dispatchModeOrDefault() {
		return dispatchMode != null ? dispatchMode : DispatchMode.Normal;
	}

	// ofAction/ofFunc/ofProcedure 的 R 归一为 Long
	@SuppressWarnings("unchecked")
	private R longResult(long result) {
		return (R)Long.valueOf(result);
	}

	@SuppressWarnings("unchecked")
	private static <R> @NotNull Future<R> castFuture(@NotNull Future<?> future) {
		return (Future<R>)future;
	}

	@SuppressWarnings("unchecked")
	private static <R> @NotNull ScheduledFuture<R> castScheduledFuture(@NotNull ScheduledFuture<?> future) {
		return (ScheduledFuture<R>)future;
	}

	@SuppressWarnings("unchecked")
	private static <R> @NotNull TimerFuture<R> castTimerFuture(@NotNull TimerFuture<?> future) {
		return (TimerFuture<R>)future;
	}

	/**
	 * 当前线程同步执行完（≡ dispatchMode=Direct 的退化形态：不进池、不包 hotGuard、不包 timeout）。
	 * ofAction 返回 0；ofFunc/ofProcedure 返回结果码；ofFunc0 返回值，异常直接抛给调用者。
	 * 显式设置过 dispatchMode/timeout/onCancel 时抛 IllegalArgumentException。
	 */
	public R call() {
		consume();
		if (dispatchModeSet || timeoutSet || onCancelSet)
			throw new IllegalArgumentException("call() does not consume dispatchMode/timeout/onCancel");
		if (action != null) {
			Task.callActionCore(action, name);
			return longResult(0);
		}
		if (procedure != null)
			return longResult(Task.callProcCore(procedure));
		if (func != null)
			return longResult(Task.callFuncCore(func, name));
		try {
			//noinspection DataFlowIssue
			return func0.call();
		} catch (Throwable e) {
			throw Task.forceThrow(e);
		}
	}

	/**
	 * 异步·事务感知：dispatchMode != Direct 且当前在运行中的事务内时延迟到事务提交后执行
	 * （rollback 不执行、redo 由新一轮重新注册），否则立即入池；dispatchMode=Direct 时跳过事务检查，
	 * 当前线程立即同步执行。
	 */
	public void run() {
		consume();
		Task.runTxnAware(dispatchMode, this::runNowInternal);
	}

	/**
	 * 异步·立即入池，不等事务提交（即使在事务中，之后该事务 redo 或 rollback 也无法撤销）。
	 */
	public void runNow() {
		consume();
		runNowInternal();
	}

	private void runNowInternal() {
		var timeout = timeoutOrDefault();
		if (action != null) {
			Task.executeActionCore(action, name, dispatchMode, timeout);
			return;
		}
		if (procedure != null) {
			Task.executeProcCore(procedure, dispatchMode, timeout);
			return;
		}
		if (func != null) {
			Task.executeFuncCore(func, name, dispatchMode, timeout);
			return;
		}
		//noinspection DataFlowIssue
		Task.submitFunc0Core(func0, name, dispatchMode, timeout); // 异常已记日志，Future 丢弃
	}

	/**
	 * 同 {@link #runNow}，但返回 Future：ofAction 的 Future 结果为 null（Direct 时为 0），
	 * ofFunc/ofProcedure 为结果码，ofFunc0 的返回值与异常经 Future 传播。
	 */
	public @NotNull Future<R> submitNow() {
		consume();
		var timeout = timeoutOrDefault();
		if (action != null)
			return castFuture(Task.submitActionCore(action, name, dispatchMode, timeout));
		if (procedure != null)
			return castFuture(Task.submitProcCore(procedure, dispatchMode, timeout));
		if (func != null)
			return castFuture(Task.submitFuncCore(func, name, dispatchMode, timeout));
		//noinspection DataFlowIssue
		return Task.submitFunc0Core(func0, name, dispatchMode, timeout);
	}

	private void checkScheduleOptions() {
		if (dispatchModeSet || onCancelSet)
			throw new IllegalArgumentException("schedule family does not consume dispatchMode/onCancel");
	}

	/**
	 * 事务感知注册延迟调度(毫秒)。忽略 dispatchMode/onCancel（显式设置抛错）。
	 */
	public void schedule(long delay) {
		consume();
		checkScheduleOptions();
		var timeout = timeoutOrDefault();
		if (action != null)
			Task.runTxnAware(null, () -> Task.scheduleActionCore(delay, action, name, timeout));
		else if (func != null)
			Task.runTxnAware(null, () -> Task.scheduleFuncCore(delay, func, name, timeout));
		else if (procedure != null)
			Task.runTxnAware(null, () -> Task.scheduleProcCore(delay, procedure, timeout));
		else
			//noinspection DataFlowIssue
			Task.runTxnAware(null, () -> Task.scheduleFunc0Core(delay, func0, name, timeout));
	}

	/**
	 * 事务感知注册固定延迟周期调度(毫秒)。忽略 dispatchMode/onCancel（显式设置抛错）。
	 * 周期任务无法携带返回值，ofFunc/ofProcedure/ofFunc0 的结果丢弃，日志与统计照常。
	 */
	public void schedule(long delay, long period) {
		consume();
		checkScheduleOptions();
		var timeout = timeoutOrDefault();
		if (action != null)
			Task.runTxnAware(null, () -> Task.schedulePeriodCore(delay, period, action, name, timeout));
		else if (func != null)
			Task.runTxnAware(null, () -> Task.scheduleFuncPeriodCore(delay, period, func, name, timeout));
		else if (procedure != null)
			Task.runTxnAware(null, () -> Task.scheduleProcPeriodCore(delay, period, procedure, timeout));
		else
			//noinspection DataFlowIssue
			Task.runTxnAware(null, () -> Task.scheduleFunc0PeriodCore(delay, period, func0, name, timeout));
	}

	/**
	 * 立即注册延迟调度(毫秒)，不等事务提交（"now" 指注册不等提交，delay 后触发照旧），返回句柄。
	 * 忽略 dispatchMode/onCancel（显式设置抛错）；ofFunc/ofProcedure 的结果码经 Future 传播。
	 */
	public @NotNull ScheduledFuture<R> scheduleNow(long delay) {
		consume();
		checkScheduleOptions();
		var timeout = timeoutOrDefault();
		if (action != null)
			return castScheduledFuture(Task.scheduleActionCore(delay, action, name, timeout));
		if (func != null)
			return castScheduledFuture(Task.scheduleFuncCore(delay, func, name, timeout));
		if (procedure != null)
			return castScheduledFuture(Task.scheduleProcCore(delay, procedure, timeout));
		//noinspection DataFlowIssue
		return Task.scheduleFunc0Core(delay, func0, name, timeout);
	}

	/**
	 * 立即注册固定延迟周期调度(毫秒)，不等事务提交，返回句柄。
	 * 忽略 dispatchMode/onCancel（显式设置抛错）。
	 * 周期任务无法携带返回值，ofFunc/ofProcedure/ofFunc0 的结果丢弃，日志与统计照常。
	 */
	public @NotNull TimerFuture<R> scheduleNow(long delay, long period) {
		consume();
		checkScheduleOptions();
		var timeout = timeoutOrDefault();
		if (action != null)
			return castTimerFuture(Task.schedulePeriodCore(delay, period, action, name, timeout));
		if (func != null)
			return castTimerFuture(Task.scheduleFuncPeriodCore(delay, period, func, name, timeout));
		if (procedure != null)
			return castTimerFuture(Task.scheduleProcPeriodCore(delay, period, procedure, timeout));
		//noinspection DataFlowIssue
		return Task.scheduleFunc0PeriodCore(delay, period, func0, name, timeout);
	}

	/**
	 * 事务感知注册每天 hour:minute 调度，默认只触发一次。
	 * 忽略 dispatchMode/onCancel（显式设置抛错）。
	 */
	public void scheduleAt(int hour, int minute) {
		scheduleAt(hour, minute, -1);
	}

	/**
	 * 事务感知注册每天 hour:minute 调度；period &gt; 0 时按该周期(毫秒)重复触发（结果丢弃，见
	 * {@link #schedule(long, long)}）。忽略 dispatchMode/onCancel（显式设置抛错）。
	 */
	public void scheduleAt(int hour, int minute, long period) {
		consume();
		checkScheduleOptions();
		var timeout = timeoutOrDefault();
		if (action != null)
			Task.runTxnAware(null, () -> Task.scheduleAtCore(hour, minute, period, action, name, timeout));
		else if (func != null)
			Task.runTxnAware(null, () -> Task.scheduleAtFuncCore(hour, minute, period, func, name, timeout));
		else if (procedure != null)
			Task.runTxnAware(null, () -> Task.scheduleAtProcCore(hour, minute, period, procedure, timeout));
		else
			//noinspection DataFlowIssue
			Task.runTxnAware(null, () -> Task.scheduleAtFunc0Core(hour, minute, period, func0, name, timeout));
	}

	/**
	 * 立即注册每天 hour:minute 调度，不等事务提交，返回句柄，默认只触发一次。
	 * 忽略 dispatchMode/onCancel（显式设置抛错）。
	 */
	public @NotNull ScheduledFuture<R> scheduleAtNow(int hour, int minute) {
		return scheduleAtNow(hour, minute, -1);
	}

	/**
	 * 立即注册每天 hour:minute 调度，不等事务提交，返回句柄；period &gt; 0 时按该周期(毫秒)重复触发
	 * （结果丢弃，见 {@link #schedule(long, long)}）。忽略 dispatchMode/onCancel（显式设置抛错）。
	 */
	public @NotNull ScheduledFuture<R> scheduleAtNow(int hour, int minute, long period) {
		consume();
		checkScheduleOptions();
		var timeout = timeoutOrDefault();
		if (action != null)
			return castScheduledFuture(Task.scheduleAtCore(hour, minute, period, action, name, timeout));
		if (func != null)
			return castScheduledFuture(Task.scheduleAtFuncCore(hour, minute, period, func, name, timeout));
		if (procedure != null)
			return castScheduledFuture(Task.scheduleAtProcCore(hour, minute, period, procedure, timeout));
		//noinspection DataFlowIssue
		return Task.scheduleAtFunc0Core(hour, minute, period, func0, name, timeout);
	}

	/**
	 * 提交到全局 {@link Task#getOneByOne()}，相同 key 的任务串行执行。
	 * 显式设置过 timeout 时抛 IllegalArgumentException（队列引擎无此概念）。
	 */
	public void executeOneByOne(@NotNull Object key) {
		executeOneByOne(key, Task.getOneByOne());
	}

	/**
	 * 提交到全局 {@link Task#getOneByOne()}，int key 不装箱。
	 */
	public void executeOneByOne(int key) {
		executeOneByOne(key, Task.getOneByOne());
	}

	/**
	 * 提交到全局 {@link Task#getOneByOne()}，long key 不装箱。
	 */
	public void executeOneByOne(long key) {
		executeOneByOne(key, Task.getOneByOne());
	}

	/**
	 * 提交到指定 {@link TaskOneByOneBase}（TaskOneByOneByKey / TaskOneByOneByKeyLru 等），相同 key 串行。
	 * 重载绑定与旧 Execute 一致：字面量 int/long 绑原生版本，显式包装类型走 Object 版（hashCode）。
	 * 显式设置过 timeout 时抛 IllegalArgumentException。ofFunc0 载荷的结果丢弃（队列语义无返回值消费者）。
	 */
	public void executeOneByOne(@NotNull Object key, @NotNull TaskOneByOneBase queue) {
		consumeBase();
		queue.execute(Objects.requireNonNull(key), newQueueTask());
	}

	/**
	 * 提交到指定 {@link TaskOneByOneBase}，int key 不装箱。
	 */
	public void executeOneByOne(int key, @NotNull TaskOneByOneBase queue) {
		consumeBase();
		queue.execute(key, newQueueTask());
	}

	/**
	 * 提交到指定 {@link TaskOneByOneBase}，long key 不装箱。
	 */
	public void executeOneByOne(long key, @NotNull TaskOneByOneBase queue) {
		consumeBase();
		queue.execute(key, newQueueTask());
	}

	/**
	 * 提交到指定 {@link TaskOneByOneByKey2}（Object key 取 hashCode 后委托 int 核心，与旧重载一致）。
	 * 显式设置过 timeout 时抛 IllegalArgumentException；onCancel 不支持 Key2。
	 */
	public void executeOneByOne(@NotNull Object key, @NotNull TaskOneByOneByKey2 queue) {
		consumeKey2();
		executeKey2(Objects.requireNonNull(key).hashCode(), queue);
	}

	/**
	 * 提交到指定 {@link TaskOneByOneByKey2}，int key 不装箱（直传 int 核心）。
	 */
	public void executeOneByOne(int key, @NotNull TaskOneByOneByKey2 queue) {
		consumeKey2();
		executeKey2(key, queue);
	}

	/**
	 * 提交到指定 {@link TaskOneByOneByKey2}，long key 取 Long.hashCode 后委托 int 核心（与旧重载一致）。
	 */
	public void executeOneByOne(long key, @NotNull TaskOneByOneByKey2 queue) {
		consumeKey2();
		executeKey2(Long.hashCode(key), queue);
	}

	private void consumeBase() {
		consume();
		if (timeoutSet)
			throw new IllegalArgumentException("executeOneByOne does not consume timeout");
	}

	private void consumeKey2() {
		consumeBase();
		if (onCancel != null)
			throw new IllegalArgumentException("onCancel is not supported by TaskOneByOneByKey2");
	}

	private @NotNull TaskOneByOneQueue.Task newQueueTask() {
		var mode = dispatchModeOrDefault();
		if (action != null)
			return new TaskOneByOneQueue.TaskAction(action, name, onCancel, mode);
		if (procedure != null)
			return new TaskOneByOneQueue.TaskFunc(procedure::call, procedure.getActionName(), onCancel, mode);
		if (func != null)
			return new TaskOneByOneQueue.TaskFunc(func, name, onCancel, mode);
		//noinspection DataFlowIssue
		return new TaskOneByOneQueue.TaskFunc0(func0, name, onCancel, mode);
	}

	private void executeKey2(int hashKey, @NotNull TaskOneByOneByKey2 queue) {
		var mode = dispatchModeOrDefault();
		if (action != null) {
			queue.executeActionCore(hashKey, action, name, mode);
			return;
		}
		if (procedure != null) {
			queue.executeProcedureCore(hashKey, procedure, mode);
			return;
		}
		if (func != null) {
			queue.executeFuncCore(hashKey, func, name, mode);
			return;
		}
		//noinspection DataFlowIssue
		queue.executeFunc0Core(hashKey, func0, name, mode);
	}
}
