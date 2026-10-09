package Zeze.Transaction;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import Zeze.Application;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.History.History;
import Zeze.Onz.Onz;
import Zeze.Onz.OnzProcedure;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.Id128;
import Zeze.Util.Random;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Zeze 事务核心：管理保存点、修改日志与记录访问，配合记录锁驱动 redo 重试，直至最终提交或回滚。
 */
public final class Transaction {
	private static final @NotNull Logger logger = LogManager.getLogger(Transaction.class);

	// perform Abort限频+计数：环境异常（如GCM被停的Acquire In Releasing）
	// 时bench类负载每笔事务一条带整栈的Abort warn，百万级迭代打成日志洪水拖垮执行。
	// 窗口外只计数，窗口内首条仍打整栈（保留完整排查信息），计数随下一条一起输出。
	private static final LongAdder abortWarnCount = new LongAdder();
	private static volatile long lastAbortWarnTime;
	// redo 重试日志限频（对齐 abortWarnCount 惯例）：强冲突负载下每笔重试一条 info 是日志洪水，
	// 且默认 Root=DEBUG 配置下逐条格式化+落盘吞掉一个数量级的冲突吞吐。
	private static final LongAdder redoInfoCount = new LongAdder();
	private static volatile long lastRedoInfoTime;
	private static final ThreadLocal<Transaction> threadLocal = new ThreadLocal<>();
	private static final @NotNull Object NULL_VALUE = new Object(); // resolveOnce 已解析为 null 的哨兵

	public static @NotNull Transaction create(@NotNull Locks locks) {
		var t = threadLocal.get();
		if (t == null)
			threadLocal.set(t = new Transaction());
		t.locks = locks;
		t.created = true;
		return t;
	}

	public static void destroy() {
		var t = threadLocal.get();
		if (t != null)
			t.reuseTransaction();
	}

	public static @Nullable Transaction getCurrent() {
		var t = threadLocal.get();
		return t != null && t.created ? t : null;
	}

	public static @Nullable Transaction getCurrentVerifyRead(@NotNull Bean bean) {
		// 读操作全部允许，所以实际上不做验证，这个函数只是为了语义完备，定义在这里。
		return getCurrent();
	}

	public static @NotNull Transaction getCurrentVerifyWrite(@NotNull Bean bean) {
		var t = getCurrent();
		if (t == null)
			throw new IllegalStateException("not in transaction");
		t.verifyRecordForWrite(bean);
		return t;
	}

	private final ArrayList<Lockey> holdLocks = new ArrayList<>(); // 读写锁的话需要一个包装类，用来记录当前维持的是哪个锁。
	private final ArrayList<Procedure> procedureStack = new ArrayList<>(); // 嵌套存储过程栈。
	private @Nullable ArrayList<Runnable> logActions; // 延迟初始化
	private final ArrayList<Savepoint> savepoints = new ArrayList<>();
	private final ArrayList<Savepoint.Action> actions = new ArrayList<>();
	// 最近一个 redo 轮次的 whileRollback 回调，延迟初始化；重做会重新注册，仅重试耗尽（TooManyTry）终局回滚时触发。
	private @Nullable ArrayList<Savepoint.Action> redoRollbackActions;
	// 最近一个以异常形态进入 perform catch 的轮次原始异常（GoBackZeze/内部错误；返回码表达的重做无异常）。
	// 重试耗尽时随 error 全栈带出并可经 getLastRoundException() 读取；perform() 开始时清零。
	private @Nullable Throwable lastRoundException;
	private final TreeMap<TableKey, RecordAccessed> accessedRecords = new TreeMap<>();
	private Locks locks;
	private @NotNull TransactionState state = TransactionState.Running;
	private boolean created;
	private boolean alwaysReleaseLockWhenRedo;
	private final ArrayList<Bean> redoBeans = new ArrayList<>();
	private @Nullable HashMap<ResolveKey, Object> onceResolved; // resolveOnce 缓存，延迟初始化
	private @Nullable OnzProcedure onzProcedure;
	final Profiler profiler = new Profiler();
	private final AtomicLong totalTransaction = new AtomicLong();
	private volatile Long tid;

	private Transaction() {
	}

	public @NotNull ArrayList<Procedure> getProcedureStack() {
		return procedureStack;
	}

	public long getTransactionId() {
		if (null != tid)
			return tid;
		synchronized (this) {
			if (null != tid)
				return tid;
			var topProcedure = getTopProcedure();
			if (null != topProcedure) {
				tid = topProcedure.getZeze().getTransactionIdAutoKey().nextId();
				return tid;
			}
		}
		return 0;
	}

	public void addLogAction(Runnable action) {
		if (null == logActions)
			logActions = new ArrayList<>();
		logActions.add(action);
	}

	@NotNull TreeMap<TableKey, RecordAccessed> getAccessedRecords() {
		return accessedRecords;
	}

	public boolean isRunning() {
		return state == TransactionState.Running;
	}

	public boolean isCompleted() {
		return state == TransactionState.Completed;
	}

	public @Nullable Procedure getTopProcedure() {
		var stackSize = procedureStack.size();
		return stackSize > 0 ? procedureStack.get(stackSize - 1) : null;
	}

	/**
	 * 最近一个以异常形态进入重做 catch 的轮次原始异常（如驱动重做的 GoBackZeze）。
	 * 仅诊断用：末轮可能是无异常的返回码重做，此时保留的是最后一个异常轮；
	 * perform() 开始时清零，线程复用的事务对象上读到的是最近一笔的结果。
	 */
	public @Nullable Throwable getLastRoundException() {
		return lastRoundException;
	}

	void reuseTransaction() {
		// 防御：正常路径由perform的finally释放；手工create+begin后直接destroy/复用
		// （不走perform）时锁会静默泄漏——非空即记error并逐个释放。
		if (!holdLocks.isEmpty()) {
			logger.error("reuseTransaction with {} held lock(s), releasing defensively", holdLocks.size());
			holdLocks.forEach(Lockey::exitLock);
			holdLocks.clear();
		}
		tid = null;
		procedureStack.clear();
		if (null != logActions)
			logActions.clear();
		savepoints.clear();
		actions.clear();
		if (redoRollbackActions != null)
			redoRollbackActions.clear();
		accessedRecords.clear();
		locks = null;
		state = TransactionState.Running;
		created = false;
		alwaysReleaseLockWhenRedo = false;
		redoBeans.clear();
		if (null != onceResolved)
			onceResolved.clear();
		onzProcedure = null;
		profiler.reset();
	}

	void reuseTransactionForRedo(@NotNull CheckResult checkResult) {
		if (checkResult == CheckResult.RedoAndReleaseLock) {
			holdLocks.forEach(Lockey::exitLock);
			holdLocks.clear();
		}
		// retry 可能保持已有的锁，清除记录和保存点。
		if (null != logActions)
			logActions.clear(); // retry 中间的日志不记录。
		// redo 重做会重新注册本轮回调，但重试耗尽（TooManyTry）终局回滚时最近一轮的
		// whileRollback 回调仍需触发（Timer 恢复链的节点推进等依赖），先暂存最近一轮的再清空。
		if (redoRollbackActions == null)
			redoRollbackActions = new ArrayList<>();
		else
			redoRollbackActions.clear();
		redoRollbackActions.addAll(actions); // rollback() 已汇入事务级的本轮回调
		for (var sp : savepoints)
			sp.mergeRollbackActions(redoRollbackActions); // Begin/End 不配对时残留层中的回调
		savepoints.clear();
		actions.clear();
		accessedRecords.clear();
		state = TransactionState.Running; // prepare to retry
		redoBeans.clear();
		// onceResolved 不清除：它的生命周期是一次 perform（含全部redo重试），
		// 重试时必须继续复用首轮解析的结果，只在 perform 入口和 reuseTransaction 中清。
	}

	public void begin() {
		var saveSize = savepoints.size();
		savepoints.add(saveSize > 0 ? savepoints.get(saveSize - 1).beginSavepoint() : new Savepoint());
	}

	public void commit() {
		int saveSize = savepoints.size();
		if (saveSize > 1)
			savepoints.get(saveSize - 2).mergeCommitFrom(savepoints.remove(saveSize - 1)); // 嵌套事务，把日志合并到上一层。
		// 最外层存储过程提交在 Perform 中处理
	}

	public void rollback() {
		int lastIndex = savepoints.size() - 1;
		if (lastIndex < 0)
			// 空栈时savepoints.remove(-1)抛无消息的IndexOutOfBoundsException，掩盖配对错误
			// （如finalRollback清空savepoints后，回调内误调rollback）。显式报错，风格对齐verifyRunning。
			throw new IllegalStateException("rollback: savepoints is empty. begin/rollback not paired.");
		Savepoint last = savepoints.remove(lastIndex);
		if (lastIndex > 0)
			savepoints.get(lastIndex - 1).mergeRollbackFrom(last); // 嵌套事务，把日志合并到上一层。
		else
			last.mergeRollbackActions(actions);
	}

	public @NotNull Log logGetOrAdd(long logKey, @NotNull Supplier<@NotNull Log> logFactory) {
		var log = getLog(logKey);
		if (log == null)
			putLog(log = logFactory.get());
		return log;
	}

	public @Nullable Log getLog(long key) {
		verifyRunningOrCompleted();
		// 允许没有 savepoint 时返回 null. 就是说允许在保存点不存在时进行读取操作。
		var saveSize = savepoints.size();
		return saveSize > 0 ? savepoints.get(saveSize - 1).getLog(key) : null;
	}

	public void putLog(@NotNull Log log) {
		verifyRunning();
		savepoints.getLast().putLog(log);
	}

	private void triggerRedoActions() {
		profiler.onRedo();
		redoBeans.forEach(Bean::resetRootInfo);
		// redo 不跟 savepoint 打交道，总是事务级别的。
		// 不要在这里重新引入"为重试做准备"的动作机制：
		// 补偿点无法预知"是否还有下一次重试"，一次性资源请用 resolveOnce。
	}

	/**
	 * 在整个事务生命周期内（含 perform 的全部 redo 重试），同一个 (owner, key) 只向 resolver 求解一次。
	 * 供 handler 内消费"一次性"的外部资源（如 Rpc 会合：removeRpcContext 只能发生一次）：
	 * 首轮求解结果（包括 null，表示已输给并发消费者）缓存在事务上，重试直接复用，不再触碰外部资源。
	 * owner 是资源的属主（Rpc.handle 传 Service）：不同属主即使 key 相同也是不同资源，不会串；
	 * 同一属主内要求 key 唯一标识资源（默认 sessionId 生成器进程内唯一，满足此约束）。
	 */
	@SuppressWarnings("unchecked")
	public <T> T resolveOnce(@NotNull Object owner, long key, @NotNull LongFunction<T> resolver) {
		var cache = onceResolved;
		if (cache == null)
			onceResolved = cache = new HashMap<>();
		var cacheKey = new ResolveKey(owner, key);
		var cached = cache.get(cacheKey);
		if (cached != null)
			return cached != NULL_VALUE ? (T)cached : null;
		var value = resolver.apply(key);
		cache.put(cacheKey, value != null ? value : NULL_VALUE);
		return value;
	}

	/**
	 * 当前在事务内，执行事务的resolveOnce，否则直接返回resolver.apply(key)。
	 */
	public static <T> T resolveOnceOrApply(@NotNull Object owner, long key, @NotNull LongFunction<T> resolver) {
		var txn = getCurrent();
		if (null != txn)
			return txn.resolveOnce(owner, key, resolver);
		return resolver.apply(key);
	}

	private record ResolveKey(@NotNull Object owner, long key) {
	}

	static void whileRedo(@NotNull Bean b) {
		// 这个目前仅用来重置Bean.RootInfo。
		// 而RootInfo的设置可能在事务外使用，此时忽略action的执行。
		// 【bean所有权语义】登记进redoBeans的bean即被本事务占有：redo重试在
		// triggerRedoActions里resetRootInfo后随重放重新登记；最终回滚不解除占有
		// （redo-only，见Bean.initRootInfoWithRedo）——复用被占有bean
		// 将抛HasManagedException，请重建实例或copy()。
		var current = getCurrent();
		if (current != null)
			current.redoBeans.add(b);
	}

	public static void whileCommit(@NotNull Runnable action) {
		var current = getCurrent();
		if (current == null) // 事务外裸NPE难定位：显式ISE
			throw new IllegalStateException("no current transaction for whileCommit");
		current.runWhileCommit(action);
	}

	public static void whileRollback(@NotNull Runnable action) {
		var current = getCurrent();
		if (current == null)
			throw new IllegalStateException("no current transaction for whileRollback");
		current.runWhileRollback(action);
	}

	public void runWhileCommit(@NotNull Runnable action) {
		if (state == TransactionState.Completed) {
			actions.add(new Savepoint.Action(Savepoint.ActionType.COMMIT, action));
			return;
		}
		verifyRunning();
		savepoints.getLast().addCommitAction(action);
	}

	public void runWhileRollback(@NotNull Runnable action) {
		if (state == TransactionState.Completed) {
			actions.add(new Savepoint.Action(Savepoint.ActionType.ROLLBACK, action));
			return;
		}
		verifyRunning();
		savepoints.getLast().addRollbackAction(action);
	}

	void setAlwaysReleaseLockWhenRedo(int tableId) {
		alwaysReleaseLockWhenRedo = true;
		if (!holdLocks.isEmpty())
			throwRedo(tableId, "Redo: AlwaysReleaseLock");
	}

	/**
	 * Procedure 第一层入口，总的处理流程，包括重做和所有错误处理。
	 *
	 * @param procedure first procedure
	 */
	public long perform(@NotNull Procedure procedure) {
		if (null != onceResolved)
			onceResolved.clear(); // 一次性解析缓存的生命周期 = 一次 perform（含redo重试），不跨事务
		if (redoRollbackActions != null)
			redoRollbackActions.clear(); // 同上，直接调用 perform 时防止读到上个事务的残留
		lastRoundException = null; // 同上
		try {
			var checkpoint = procedure.getZeze().getCheckpoint();
			if (checkpoint == null)
				return Procedure.Closed;
			for (int tryCount = 0; tryCount < 256; ++tryCount) { // 最多尝试次数
				// 默认在锁内重复尝试，除非CheckResult.RedoAndReleaseLock，否则由于CheckResult.Redo保持锁会导致死锁。
				for (; tryCount < 256; ++tryCount) { // 最多尝试次数
					// 重试轮次间重新检查停机状态：终检点已过（checkpoint==null）时，
					// 在途事务显式失败（Closed）退出，不再重执行业务逻辑后在提交点被静默丢弃。
					if (procedure.getZeze().getCheckpoint() == null) {
						if (redoRollbackActions != null)
							actions.addAll(redoRollbackActions);
						finalRollback(procedure);
						return Procedure.Closed;
					}
					CheckResult checkResult = CheckResult.Redo; // 用来决定是否释放锁，除非 lockAndCheck 明确返回需要释放锁，否则都不释放。
					try {
						var result = procedure.call();
						switch (state) {
						case Running:
							var saveSize = savepoints.size();
							if ((result == Procedure.Success && saveSize != 1)
									|| (result != Procedure.Success && saveSize > 0)) {
								// 这个错误不应该重做
								logger.error("perform({}): savepoints.size != 1", procedure);
								finalRollback(procedure);
								return Procedure.ErrorSavepoint;
							}
							// lockAndCheck 单独 try 归类：业务成功后根savepoint必在（size==1），
							// 其意外异常（如acquire链路的unchecked）若走外层catch会被
							// "!savepoints.isEmpty()"误判为ErrorSavepoint——不重试且丢失
							// 真实异常语义；显式失败并保留原始栈。
							CheckResult lockResult;
							try {
								lockResult = lockAndCheck(procedure);
							} catch (Throwable lcEx) {
								logger.error("perform({}) lockAndCheck exception. run count:{}", procedure, tryCount, lcEx);
								finalRollback(procedure);
								return Procedure.Exception;
							}
							checkResult = lockResult;
							if (checkResult == CheckResult.Success) {
								if (result == Procedure.Success) {
									// onz事务执行阶段的2段式同步等待。
									OnzProcedure flushMode = null; // 即使当前是Onz事务，也要根据flushMode决定是否继续传递参数给flush过程。
									if (onzProcedure != null) {
										onzProcedure.sendReadyAndWait();
										if (onzProcedure.getFlushMode() != Onz.eFlushAsync)
											flushMode = onzProcedure;
									}
									try {
										finalCommit(procedure, flushMode);
									} catch (RejectWhileStopping e) {
										// 终检点已过：tryUpdateAndCheckpoint在应用修改前（或落库前）显式拒绝，
										// 转为finalRollback+Closed显式失败，不得静默跳过落库后返回Success
										// （否则已应答的提交丢失）。
										// Onz参与方"结果已发、Commit决策已送达"（能走到这里说明
										// sendReadyAndWait已按Commit决策返回，而协调者侧saveCommitPoint
										// 严格先行已持久化）而本地因停机回滚——跨集群分歧（协调者按提交推进、
										// 本地写入未发生）。回滚时刻进程存活、信息完备，是唯一确定性的暴露点，
										// 必须记error；回填哨兵让迟到的redo Commit取到时二次确认。
										if (onzProcedure != null) {
											logger.error("onz participant({}) tid={}: coordinator commit decision"
													+ " delivered and persisted, but local transaction rolled back"
													+ " while stopping -- cross-cluster divergence, manual check required",
													procedure.getActionName(), onzProcedure.getOnzTid(), e);
											onzProcedure.markRolledBackAfterReady();
										}
									finalRollback(procedure);
									return Procedure.Closed;
								} catch (AssertionError ae) {
									// whileCommit等终局回调的断言失败：数据已成功落库（回调在提交点之后），
									// 不进halt路径；向上抛出让测试框架看到失败——终局吞掉会造成单测假绿
									// （perform静默返回Success）。
									throw ae;
								} catch (Throwable ex) { // logger.fatal & halt
										logger.fatal("finalCommit exception:", ex);
										// final Commit 不能抛出异常。否则就halt。

										// 首先释放锁
										holdLocks.forEach(Lockey::exitLock);
										holdLocks.clear();

										// halt process.
										// checkpointRun裸调用时双读NPE会吞掉halt（带伤运行，
										// 外层catch还可能重跑已炸事务）——统一收口到Application.
										// haltAfterCheckpoint：checkpoint尽力保存失败也不拦下halt。
										Application.haltAfterCheckpoint(procedure.getZeze(), 543543);
										return 0;
									}
									return Procedure.Success;
								}
								finalRollback(procedure);
								return result;
							}
							break; // retry

						case Abort:
							logger.warn("perform({}): Abort", procedure);
							finalRollback(procedure);
							return Procedure.AbortException;

						case Redo:
							break; // retry

						case RedoAndReleaseLock:
							checkResult = CheckResult.RedoAndReleaseLock;
							break; // retry
						}
						// retry clear in finally
						// 限频+计数（对齐下方Abort日志的1秒窗口）：窗口内首条带上窗口计数。
						redoInfoCount.increment();
						var redoNowMs = System.currentTimeMillis();
						if (redoNowMs - lastRedoInfoTime >= 1000) {
							lastRedoInfoTime = redoNowMs;
							logger.info("perform({}): {}, count={} (rate-limited 1/s)",
									procedure, checkResult, redoInfoCount.sumThenReset());
						}
						triggerRedoActions();
					} catch (Throwable e) { // logger.error, logger.warn, rethrow AssertionError, ignored
						// Procedure.Call 里面已经处理了异常。只有 unit test 或者重做或者内部错误会到达这里。
						// 在 unit test 下，异常日志会被记录两次。
						lastRoundException = e; // 重试耗尽终局诊断：保留最近一轮异常（末轮为返回码重做时保留的是最后一个异常轮）
						switch (state) {
						case Running:
							logger.error("perform({}) exception. run count:{}", procedure, tryCount, e);
							if (!savepoints.isEmpty()) {
								// 这个错误不应该重做
								logger.error("perform({}) exception. !savepoints.isEmpty", procedure, e);
								finalRollback(procedure);
								return Procedure.ErrorSavepoint;
							}
							// 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
							if (e instanceof AssertionError) {
								finalRollback(procedure);
								throw (AssertionError)e;
							}
							// 同上：lockAndCheck在此路径再抛会顶掉原始异常e——单独归类，
							// 原始异常已在上方记录，这里按内部错误显式失败。
							try {
								checkResult = lockAndCheck(procedure);
							} catch (Throwable lcEx) {
								logger.error("perform({}) lockAndCheck exception (in exception path). run count:{}",
										procedure, tryCount, lcEx);
								finalRollback(procedure);
								return Procedure.Exception;
							}
							if (checkResult == CheckResult.Success) {
								finalRollback(procedure);
								return Procedure.Exception;
							}
							// retry
							break;

						case Abort:
							// 限频+计数（Acquire Failed等环境性Abort在bench下每笔一条整栈warn是日志洪水）；
							// 1秒窗口，窗口内首条打整栈并带上窗口计数。
							abortWarnCount.increment();
							var nowMs = System.currentTimeMillis();
							if (nowMs - lastAbortWarnTime >= 1000) {
								lastAbortWarnTime = nowMs;
								logger.warn("perform({}): Abort, count={} (rate-limited 1/s)",
										procedure, abortWarnCount.sumThenReset(), e);
							}
							finalRollback(procedure);
							return Procedure.AbortException;

						case Redo:
							logger.trace("perform({}): Redo", procedure, e);
							checkResult = CheckResult.Redo;
							break;

						case RedoAndReleaseLock:
							logger.trace("perform({}): RedoAndReleaseLock", procedure, e);
							checkResult = CheckResult.RedoAndReleaseLock;
							break;

						default: // case Completed:
							if (e instanceof AssertionError)
								throw (AssertionError)e;
							logger.error("perform({}) {} exception. run count:{}", procedure, state, tryCount, e);
						}
						triggerRedoActions();
						// retry
					} finally {
					// alwaysReleaseLockWhenRedo 的升级统一放在这里：try 正常返回与异常路径
					// （throwRedo→GoBackZeze 走 catch）共用，且必须先于 reuseTransactionForRedo——
					// 它按 checkResult 决定是否释放锁。
						if (alwaysReleaseLockWhenRedo && checkResult == CheckResult.Redo)
							checkResult = CheckResult.RedoAndReleaseLock;
						reuseTransactionForRedo(checkResult);
					}

					if (checkResult == CheckResult.RedoAndReleaseLock) {
						procedure.procedureCounter().redoAndReleaseLock();
						break;
					}
					procedure.procedureCounter().redo();
				}
				// 实现Fresh队列以后删除Sleep。
				try {
					Thread.sleep(Random.getInstance().nextInt(80) + 20);
				} catch (InterruptedException e) {
					// 恢复中断标志并按取消语义退出重试：perform跑在业务线程上，
					// shutdownNow/任务取消依赖interrupt让事务及时让位。不得吞掉标志继续重试：
					// 停机期间每轮sleep立即再抛形成日志洪水、取消被拖延到255次耗尽。
					// 中断视为"本事务未执行"：回滚最近一轮回调后按异常码返回。
					logger.error("perform({}): interrupted, cancel retry", procedure);
					Thread.currentThread().interrupt();
					if (redoRollbackActions != null)
						actions.addAll(redoRollbackActions);
					finalRollback(procedure);
					return Procedure.Exception;
				}
			}
			// 重试耗尽是严重异常场景：最后异常轮的原始异常全栈带出，调用方无需翻日志定位冲突根因。
			if (lastRoundException != null)
				logger.error("perform({}): too many try, last exception:", procedure, lastRoundException);
			else
				logger.error("perform({}): too many try", procedure);
			// 最后一轮回调已被 reuseTransactionForRedo 清空，用暂存的最近一轮回调终局回滚。
			if (redoRollbackActions != null)
				actions.addAll(redoRollbackActions);
			finalRollback(procedure);
			return Procedure.TooManyTry;
		} finally {
			state = TransactionState.Completed; // 异常到这里时，可能state没有设置。
			holdLocks.forEach(Lockey::exitLock);
			holdLocks.clear();
			// 锁之后计数并尝试checkpoint。
			var totalCount = totalTransaction.incrementAndGet();
			var config = procedure.getZeze().getConfig().getCheckpointTransactionPeriod();
			if (config > 0 && (totalCount % config == 0)) {
				try {
					procedure.getZeze().checkpointRunThread();
				} catch (Throwable e) { // logger.error
					// 停机窗口守卫：Task.shutdownNow 后 submitNow 链路抛 IllegalStateException，
					// finally 抛异常会吞掉 perform 的正常返回值（已提交事务向客户端报错→重试→重复执行）。
					// 周期触发丢失无害：后台 checkpoint 线程与 stop 序列仍有兜底。
					logger.error("perform({}): checkpointRunThread fail", procedure, e);
				}
			}
		}
	}

	private void triggerActions(@NotNull Procedure procedure) {
		//noinspection ForLoopReplaceableByForEach
		for (var i = 0; i < actions.size(); ++i) { // size可能会在循环中增加
			var action = actions.get(i);
			try {
				action.action.run();
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) { // logger.error
				String typeStr;
				if (action.actionType == Savepoint.ActionType.COMMIT) {
					typeStr = "commit";
				} else if (action.actionType == Savepoint.ActionType.NESTED_ROLLBACK) {
					typeStr = "nestedRollback";
				} else {
					typeStr = "rollback";
				}
				logger.error("{} Procedure={} Action={} exception:",
						typeStr, procedure.getActionName(), action.action.getClass().getName(), e);
				// action是Savepoint$Action包装，目标Runnable在action.action字段，直接getClass()只会得到包装类名。
			}
		}
	}

	private void triggerCommitActions(@NotNull Procedure procedure) {
		triggerActions(procedure);
	}

	private void triggerRollbackActions(@NotNull Procedure procedure) {
		triggerActions(procedure);
	}

	public void setOnzProcedure(@Nullable OnzProcedure onzProcedure) {
		this.onzProcedure = onzProcedure;
	}

	private void finalCommit(@NotNull Procedure proc, @Nullable OnzProcedure flushMode) throws Exception {
		// 下面不允许失败了，因为最终提交失败，数据可能不一致，而且没法恢复。
		proc.getZeze().getProcedureLockWatcher().doWatch(proc, accessedRecords);
		var lastSp = savepoints.getLast();

		// collect logs and notify listeners
		var cc = new Changes(this, proc);
		RelativeRecordSet.tryUpdateAndCheckpoint(this, proc, () -> {
			try {
				lastSp.mergeCommitActions(actions);
				lastSp.commit();
				for (var v : accessedRecords.values()) {
					v.atomicTupleRecord.record().setNotFresh();
					if (v.dirty) {
						v.atomicTupleRecord.record().commit(v);
						var newValue = v.atomicTupleRecord.record().getSoftValue();
						if (newValue != null) {
							// 如果newValue为null，表示记录被删除，以后再次PutValue，version从0重新开始。
							var oldValue = v.atomicTupleRecord.strongRef();
							var oldVersion = oldValue != null ? oldValue.version() : 0;
							newValue.version(oldVersion + 1);
						}
					}
				}
			} catch (Throwable e) { // halt
				logger.fatal("finalCommit({}) exception:", proc, e);
				LogManager.shutdown();
				Runtime.getRuntime().halt(54321);
			}
		}, flushMode, new RelativeRecordSet.HistoryChangesCollector() {
			// 历史 gid 的整个取号（cache.next()，含段耗尽的续段分配）都在日志应用之前：
			// 取号若在应用后失败，数据已生效而 tHistory 永久缺失（gid 未消费，键空间连
			// 空洞都没有，回放端无从感知），Immediately 模式更是补刷后吞异常报假成功。
			// 任何取号失败=数据未应用即干净失败（RejectHistoryAllocFailed）；成功取号
			// 立即入账，此后任何失败（应用/收集/落库）都留下账本痕迹（超龄未核销=
			// 确定性缺口告警）。count=1 的顺序保证不变：取号仍在 rrs 锁内（Table 模式）/
			// 记录锁持有期（Immediately），SM 按请求到达序发号。
			private Id128 historyGid;

			@Override
			public void beforeApply() {
				// 门控与走查分离：走查/编码必须留在应用后——Record.collect 对 Put/Remove 的
				// 分类读 ar.committedPutLog，它由应用期（PutLog.commit）填充，前置走查会把
				// put 误分类为 edit（监听者拿到 null LogBean、History 编码错误）。取号门控用
				// anyDirty：isHistory 下 collectRecord 对每个 dirty 记录必登记，与走查后的
				// records 非空精确等价。
				var zeze = proc.getZeze();
				if (!zeze.getConfig().isHistory())
					return;
				for (var ar : accessedRecords.values()) {
					if (!ar.dirty)
						continue;
					// 上一次分配可能已异常完成（Udp超时毒化），直接get()会抛异常——毒化时
					// 兜底发起新分配替换。get() 阻塞等待批次，此刻数据尚未应用：任何失败转
					// RejectHistoryAllocFailed（RejectWhileStopping 同款干净失败路径：
					// finalRollback+Closed），不把"已应用数据+历史缺失"的静默分歧留给应用后。
					// 段耗尽的续段分配同样在此（cache.next() 内同步换段）——失败同款干净失败。
					try {
						@SuppressWarnings("DataFlowIssue")
						var future = zeze.getServiceManager().getUsableTid128CacheFuture(zeze.getConfig().getHistory());
						historyGid = future.get().next();
						// 取号即入账：gid 消费先于数据应用，此后任何失败（应用/收集/落库）
						// 都在账本留下"已发号未核销"痕迹（超龄=sweep的确定性缺口告警）。
						zeze.getPendingGidLedger().register(historyGid, System.nanoTime());
					} catch (Throwable ex) {
						logger.error("finalCommit({}) history gid alloc fail before apply:", proc.getActionName(), ex);
						throw new RejectHistoryAllocFailed("history gid alloc fail before apply: " + proc.getActionName());
					}
					break;
				}
			}

			@Override
			public @Nullable BLogChanges.Data afterApply() {
				var it = lastSp.logIterator();
				if (it != null) {
					while (it.moveToNext()) {
						var log = it.value();
						if (log.category() != Log.Category.eHistory)
							continue;
						var logBelong = log.getBelong();
						// 这里都是修改操作的日志，没有Owner的日志是特殊测试目的加入的，简单忽略即可。
						if (logBelong != null && logBelong.isManaged()) {
							// 第一个参数Owner为null，表示bean属于record，到达root了。
							cc.collect(logBelong, log);
						}
					}
				}

				for (var ar : accessedRecords.values()) {
					if (ar.dirty)
						cc.collectRecord(ar);
				}

				// 门控与原callable同形（isHistory&&records非空）：records 在 !isHistory 时也会
				// 因监听者登记而非空（collectRecord 对有 listener 的表无早退），不能单看 records。
				if (proc.getZeze().getConfig().isHistory() && !cc.getRecords().isEmpty()) {
					var gid = historyGid;
					// 不变量守卫：isHistory 下门控（anyDirty）与本走查的 records 非空精确等价
					//（collectRecord 无早退必登记），gid 缺席属不可达形态——宁可响亮失败也
					// 不静默产出无历史的数据。
					if (null == gid)
						throw new IllegalStateException("history changes without gid: " + proc.getActionName());
					if (proc instanceof ProtocolProcedure pp) {
						return History.buildLogChanges(gid, cc,
								pp.getProtocolClassName(), pp.getProtocolRawArgument());
					}
					return History.buildLogChanges(gid, cc, null, null);
				}
				return null;
			}
		});

		// 禁止在listener回调中访问表格的操作。除了回调参数中给定的记录可以访问。
		// 不支持在回调中再次执行事务。
		// 在Notify之前设置的。
		state = TransactionState.Completed;

		try {
			if (null != logActions) {
				// logActions逐项隔离（与triggerActions同型）。过程日志动作、监听通知、
				// 提交回调三步语义独立，任一logAction抛错（如用户替换的Procedure.logAction）
				// 不得吞掉notifyListener与whileCommit回调（典型是Rpc应答，丢失即静默丢语义）。
				for (var act : logActions) {
					try {
						act.run();
					} catch (Throwable e) { // logger.error
						logger.error("finalCommit({}) logAction exception:", proc, e);
					}
				}
			}
			cc.notifyListener();
			triggerCommitActions(proc);
		} catch (AssertionError ex) {
			// 测试断言失败必须向上传播（与perform对业务断言的重抛语义一致），终局吞掉会造成假绿。
			throw ex;
		} catch (Throwable ex) { // logger.error
			logger.error("finalCommit({}) exception:", proc, ex);
		}
	}

	private void finalRollback(@NotNull Procedure procedure) {
		for (var ra : accessedRecords.values())
			ra.atomicTupleRecord.record().setNotFresh();
		// Begin/End 不配对（ErrorSavepoint 路径）时，savepoints 中可能残留未汇入的 whileRollback 回调，一并触发。
		for (var sp : savepoints)
			sp.mergeRollbackActions(actions);
		savepoints.clear(); // 这里可以安全的清除日志，这样如果 rollback_action 需要读取数据，将读到原始的。
		state = TransactionState.Completed;
		try {
			if (null != logActions) {
				// 同finalCommit，logActions逐项隔离，任一抛错不得吞掉后续项与whileRollback回调。
				for (var act : logActions) {
					try {
						act.run();
					} catch (Throwable e) { // logger.error
						logger.error("finalRollback({}) logAction exception:", procedure, e);
					}
				}
			}
			triggerRollbackActions(procedure);
		} catch (AssertionError ex) {
			// 同finalCommit：测试断言失败必须向上传播，终局吞掉会造成假绿。
			throw ex;
		} catch (Throwable ex) { // logger.error
			logger.error("finalRollback({}) exception:", procedure, ex);
		}
	}

	/**
	 * 只能添加一次。
	 */
	void addRecordAccessed(@NotNull Record.RootInfo root, @NotNull RecordAccessed ra,
						   @SuppressWarnings("unused") boolean removeWhileRollback) {
		verifyRunning();
		ra.initRootInfo(root, null);
		accessedRecords.put(root.tableKey(), ra);
	}

	public @Nullable RecordAccessed getRecordAccessed(@NotNull TableKey key) {
		// 允许读取事务内访问过的记录。
		verifyRunningOrCompleted();
		return accessedRecords.get(key);
	}

	public void verifyRecordForWrite(@NotNull Bean bean) {
		var ri = bean.rootInfo;
		//noinspection DataFlowIssue
		if (ri.record().getState() == GlobalCacheManagerConst.StateRemoved)
			throwRedo(ri.tableKey().getId(), "Redo: StateRemoved: " + bean.tableKey()); // 这个错误需要redo。不是逻辑错误。
		//noinspection DataFlowIssue
		var ra = getRecordAccessed(bean.tableKey());
		if (ra == null)
			throw new IllegalStateException("VerifyRecordAccessed: Record Not Control Under Current Transaction: " + bean.tableKey());
		var atr = ra.atomicTupleRecord;
		var r = atr.record();
		if (ri.record() != r)
			throw new IllegalStateException("VerifyRecordAccessed: Record Reloaded: " + bean.tableKey());
		// 事务结束后可能会触发Listener，此时Commit已经完成，Timestamp已经改变，
		// 这种情况下不做RedoCheck，当然Listener的访问数据是只读的。
		var t = r.getTable();
		if (t.getZeze().getConfig().getFastRedoWhenConflict()
				&& state != TransactionState.Completed
				&& r.getTimestamp() != atr.timestamp())
			throwRedo(ri.tableKey().getId(), "Redo: FastRedoWhenConflict(" + t.getName() + ')');
	}

	private enum CheckResult {
		Success,
		Redo,
		RedoAndReleaseLock
	}

	private static @NotNull CheckResult _check_(boolean writeLock,
												@NotNull RecordAccessed e) {
		e.atomicTupleRecord.record().enterFairLock();
		try {
			if (writeLock) {
				switch (e.atomicTupleRecord.record().getState()) {
				case GlobalCacheManagerConst.StateRemoved:
					// 被从cache中清除，不持有该记录的Global锁，简单重做即可。
					return CheckResult.Redo;

				case GlobalCacheManagerConst.StateInvalid:
					return CheckResult.RedoAndReleaseLock; // 写锁发现Invalid，可能有Reduce请求。

				case GlobalCacheManagerConst.StateModify:
					return e.atomicTupleRecord.timestamp() != e.atomicTupleRecord.record().getTimestamp()
							? CheckResult.Redo : CheckResult.Success;

				case GlobalCacheManagerConst.StateShare:
					// 这里可能死锁：另一个先获得提升的请求要求本机Reduce，但是本机Checkpoint无法进行下去，被当前事务挡住了。
					// 通过 GlobalCacheManager 检查死锁，返回失败;需要重做并释放锁。
					var acquire = e.atomicTupleRecord.record().acquire(GlobalCacheManagerConst.StateModify,
							e.atomicTupleRecord.record().isFresh(), false);
					//noinspection DataFlowIssue
					if (acquire.resultState() != GlobalCacheManagerConst.StateModify) {
						e.atomicTupleRecord.record().setNotFresh(); // 抢失败不再新鲜。
						logger.debug("Acquire Failed. Maybe DeadLock Found: record={}, time={}, resultCode={}",
								e.atomicTupleRecord.record(), e.atomicTupleRecord.timestamp(), acquire.resultCode());
						e.atomicTupleRecord.record().setState(GlobalCacheManagerConst.StateInvalid);
						return CheckResult.RedoAndReleaseLock;
					}
					e.atomicTupleRecord.record().setState(GlobalCacheManagerConst.StateModify);
					return e.atomicTupleRecord.timestamp() != e.atomicTupleRecord.record().getTimestamp()
							? CheckResult.Redo : CheckResult.Success;
				}
				return e.atomicTupleRecord.timestamp() != e.atomicTupleRecord.record().getTimestamp()
						? CheckResult.Redo : CheckResult.Success; // impossible
			}

			// read lock
			return switch (e.atomicTupleRecord.record().getState()) {
				case GlobalCacheManagerConst.StateRemoved ->
					// 被从cache中清除，不持有该记录的Global锁，简单重做即可。
						CheckResult.Redo;
				case GlobalCacheManagerConst.StateInvalid ->
						CheckResult.RedoAndReleaseLock; // 发现Invalid，可能有Reduce请求或者被Cache清理，此时保险起见释放锁。
				default -> e.atomicTupleRecord.timestamp() != e.atomicTupleRecord.record().getTimestamp()
						? CheckResult.Redo : CheckResult.Success;
			};
		} finally {
			e.atomicTupleRecord.record().exitFairLock();
		}
	}

	@NotNull Lockey getLockey(@NotNull TableKey key) {
		return locks.get(key);
	}

	private @NotNull CheckResult lockAndCheck(@NotNull Map.Entry<TableKey, RecordAccessed> e) {
		Lockey lockey = getLockey(e.getKey());
		boolean writeLock = e.getValue().dirty;
		lockey.enterLock(writeLock);
		holdLocks.add(lockey);
		return _check_(writeLock, e.getValue());
	}

	private @NotNull CheckResult lockAndCheck(@NotNull Procedure procedure) {
		var level = procedure.getTransactionLevel();
		boolean allRead = true;
		var saveSize = savepoints.size();
		if (saveSize > 0) {
			// 全部 Rollback 时 Count 为 0；最后提交时 Count 必须为 1；
			// 其他情况属于Begin,Commit,Rollback不匹配。外面检查。
			var it = savepoints.get(saveSize - 1).logIterator();
			if (it != null) {
				while (it.moveToNext()) {
					// 特殊日志。不是 bean 的修改日志，当然也不会修改 Record。
					// 现在不会有这种情况，保留给未来扩展需要。
					var belong = it.value().getBelong();
					if (belong == null)
						continue;

					var tkey = belong.tableKey();
					if (tkey == null) {
						// 非受管bean没有tableKey。
						// 必须跳过：继续走下去TreeMap.get(null)必然NPE，防御分支自己先崩，掩盖真实诊断信息。
						logger.error("impossible! log bean({}): {}", belong.getClass().getName(), belong);
						continue;
					}
					var record = accessedRecords.get(tkey);
					if (record != null) {
						record.dirty = true;
						allRead = false;
					} else // 只有测试代码会把非 Managed 的 Bean 的日志加进来。
						logger.error("impossible! record not found.");
				}
			}
		}

		if (allRead && level == TransactionLevel.AllowDirtyWhenAllRead)
			return CheckResult.Success;

		boolean conflict = false; // 冲突了，也继续加锁，为重做做准备！！！
		if (holdLocks.isEmpty()) {
			for (var e : accessedRecords.entrySet()) {
				var r = lockAndCheck(e);
				switch (r) {
				case Success:
					break;
				case Redo:
					conflict = true;
					ZezeCounter.instance.tableCounter(e.getKey().getId(), ZezeCounter.TableMetric.REDO).increment();
					break; // continue lock
				default:
					return r;
				}
			}
			return conflict ? CheckResult.Redo : CheckResult.Success;
		}

		int index = 0;
		int n = holdLocks.size();
		final var ite = accessedRecords.entrySet().iterator();
		for (var e = ite.hasNext() ? ite.next() : null; e != null; ) {
			// 如果 holdLocks 全部被对比完毕，直接锁定它
			if (index >= n) {
				var r = lockAndCheck(e);
				switch (r) {
				case Success:
					break;
				case Redo:
					conflict = true;
					ZezeCounter.instance.tableCounter(e.getKey().getId(), ZezeCounter.TableMetric.REDO).increment();
					break; // continue lock
				default:
					return r;
				}
				e = ite.hasNext() ? ite.next() : null;
				continue;
			}

			Lockey curLock = holdLocks.get(index);
			int c = curLock.getTableKey().compareTo(e.getKey());

			// holdLocks a  b  ...
			// needLocks a  b  ...
			if (c == 0) {
				// 这里可能发生读写锁提升
				if (e.getValue().dirty && !curLock.isWriteLockHeld()) {
					// 必须先全部释放，再升级当前记录锁，再锁后面的记录。
					// 直接 unlockRead，lockWrite会死锁。
					n = _unlock_start_(index, n);
					// 从当前index之后都是新加锁，并且index和n都不会再发生变化。
					// 重新从当前 e 继续锁。
					continue;
				}
					// 即使锁内，Record.Global.State 也可能没有提升到需要的水平，需要重新_check_。
				var r = _check_(e.getValue().dirty, e.getValue());
				switch (r) {
				case Success:
					// 已经锁内，所以肯定不会冲突，多数情况是这个。
					break;
				case Redo:
					// Impossible!
					conflict = true;
					ZezeCounter.instance.tableCounter(e.getKey().getId(), ZezeCounter.TableMetric.REDO).increment();
					break; // continue lock
				default:
					// _check_可能需要到Global提升状态，这里可能发生GLOBAL-DEAD-LOCK。
					return r;
				}
				index++;
				e = ite.hasNext() ? ite.next() : null;
				continue;
			}
			// holdLocks a  b  ...
			// needLocks a  c  ...
			if (c < 0) {
				// 释放掉 比当前锁序小的锁，因为当前事务中不再需要这些锁
				int unlockEndIndex = index;
				while (unlockEndIndex < n && holdLocks.get(unlockEndIndex).getTableKey().compareTo(e.getKey()) < 0)
					holdLocks.get(unlockEndIndex++).exitLock();
				holdLocks.subList(index, unlockEndIndex).clear();
				n = holdLocks.size();
				// 重新从当前 e 继续锁。
				continue;
			}

			// holdLocks a  c  ...
			// needLocks a  b  ...
			// 为了不违背锁序，释放从当前锁开始的所有锁
			n = _unlock_start_(index, n);
			// 重新从当前 e 继续锁。
		}
		return conflict ? CheckResult.Redo : CheckResult.Success;
	}

	private int _unlock_start_(int index, int nLast) {
		for (int i = index; i < nLast; ++i)
			holdLocks.get(i).exitLock();
		holdLocks.subList(index, nLast).clear();
		return holdLocks.size();
	}

	@Contract("_, _ -> fail")
	public void throwAbort(@NotNull String msg, @Nullable Throwable cause) {
		if (state != TransactionState.Running)
			throw new IllegalStateException("Abort: State Is Not Running: " + state + ", msg: " + msg, cause);
		state = TransactionState.Abort;
		throw cause != null ? new GoBackZeze(msg, cause) : new GoBackZeze(msg);
	}

	@Contract("_, _, _ -> fail")
	public void throwRedoAndReleaseLock(int tableId, @NotNull String msg, @Nullable Throwable cause) {
		ZezeCounter.instance.tableCounter(tableId, ZezeCounter.TableMetric.REDO).increment();
		if (state != TransactionState.Running)
			throw new IllegalStateException("RedoAndReleaseLock: State Is Not Running: " + state + ", msg: " + msg, cause);
		state = TransactionState.RedoAndReleaseLock;
		throw cause != null ? new GoBackZeze(msg, cause) : new GoBackZeze(msg);
	}

	@Contract("_, _ -> fail")
	public void throwRedo(int tableId, @NotNull String msg) {
		ZezeCounter.instance.tableCounter(tableId, ZezeCounter.TableMetric.REDO).increment();
		if (state != TransactionState.Running)
			throw new IllegalStateException("Redo: State Is Not Running: " + state);
		state = TransactionState.Redo;
		throw new GoBackZeze(msg);
	}

	public void verifyRunning() {
		if (state != TransactionState.Running)
			throw new IllegalStateException("State Is Not Running: " + state);
	}

	public void verifyRunningOrCompleted() {
		if (state != TransactionState.Running && state != TransactionState.Completed)
			throw new IllegalStateException("State Is Not Running or Completed: " + state);
	}

	/**
	 * 停机窗口拒绝提交——终检点已过（checkpoint==null），修改无法保证落库。
	 * RelativeRecordSet.tryUpdateAndCheckpoint在应用修改前（或落库/注册脏集前）抛出，
	 * perform捕获后转为finalRollback+Procedure.Closed显式失败，
	 * 不得静默跳过落库后返回Success（否则已应答的提交丢失）。
	 */
	static class RejectWhileStopping extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 0L;

		RejectWhileStopping(@NotNull String msg) {
			super(msg);
		}
	}

	/**
	 * 历史 gid 分配失败（日志应用前拒绝，history-01）：继承 RejectWhileStopping 的 perform
	 * 处理路径（finalRollback+Closed）。抛出点在 commit.run 之前，数据未应用，干净失败回滚，
	 * 历史与数据同生共死；onz 参与方已收 Commit 决策时的跨集群分歧由既有停机分歧告警覆盖
	 * （同为"决策已持久化而本地回滚"的暴露点）。
	 */
	static final class RejectHistoryAllocFailed extends RejectWhileStopping {
		@Serial
		private static final long serialVersionUID = 0L;

		RejectHistoryAllocFailed(@NotNull String msg) {
			super(msg);
		}
	}
}
