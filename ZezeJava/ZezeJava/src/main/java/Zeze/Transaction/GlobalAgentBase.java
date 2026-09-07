package Zeze.Transaction;

import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Services.AchillesHeelConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GlobalAgentBase extends ReentrantLock {
	private static final @NotNull Logger logger = LogManager.getLogger(GlobalAgentBase.class);

	public final @NotNull Application zeze;
	private @NotNull AchillesHeelConfig config = new AchillesHeelConfig(1500, 10000, 60 * 1000);
	private volatile long activeTime = System.currentTimeMillis();
	protected int globalCacheManagerHashIndex;
	private volatile @Nullable Releaser releaser;

	public GlobalAgentBase(@NotNull Application zeze) {
		this.zeze = zeze;
	}

	public final @NotNull AchillesHeelConfig getConfig() {
		return config;
	}

	// release完成回调队列（GlobalClient重连收尾等）。startRelease统一入队（含首个Releaser的发起者），
	// Releaser完成后由checkReleaseTimeout取出在锁外执行。仅在本对象ReentrantLock内访问。
	private final @NotNull ArrayDeque<Runnable> pendingEndActions = new ArrayDeque<>();

	// checkReleaseTimeout的drained哨兵初值：避免可空局部变量（@Nullable Runnable[]会让元素
	// 被TYPE_USE语义推断为可空，action.run()报警告），空数组循环为no-op，无需null分支。
	private static final @NotNull Runnable[] emptyEndActions = new Runnable[0];

	public final long getActiveTime() {
		return activeTime;
	}

	public final void setActiveTime(long value) {
		activeTime = value;
		zeze.getAchillesHeelDaemon().setProcessDaemonActiveTime(this, value);
	}

	public boolean isReleasing() {
		return releaser != null;
	}

	public final void initialize(int maxNetPing, int serverProcessTime, int serverReleaseTimeout) {
		config = new AchillesHeelConfig(maxNetPing, serverProcessTime, serverReleaseTimeout);
		zeze.getAchillesHeelDaemon().onInitialize(this);
	}

	public enum CheckReleaseResult {
		NoRelease,
		Releasing,
		Timeout,
	}

	public @NotNull CheckReleaseResult checkReleaseTimeout(long now, int timeout) {
		var drained = emptyEndActions;
		lock();
		try {
			var r = releaser;
			if (r == null)
				return CheckReleaseResult.NoRelease;

			if (r.isCompletedSuccessfully()) {
				logger.info("Global.Releaser End.");
				releaser = null;
				if (!pendingEndActions.isEmpty()) {
					drained = pendingEndActions.toArray(new Runnable[0]);
					pendingEndActions.clear();
				}
			} else if (now - r.startTime > timeout)
				return CheckReleaseResult.Timeout;
			else
				return CheckReleaseResult.Releasing;
		} finally {
			unlock();
		}
		// 每次成功Release，设置一次活动时间，阻止AchillesHeelDaemon马上再次触发Release。
		// 必须先于endAction执行：endAction里connector.start()的同步DNS（可达5-30秒）若拖延
		// 活动时间上报，外部Daemon.Monitor会把DNS耗时计入release预算，idle超serverReleaseTimeout
		// 即destroySubprocess杀进程重启（Daemon.java Monitor.run）。
		setActiveTime(System.currentTimeMillis());
		// 排队的endAction在锁外执行（如GlobalClient的连接重启，不持锁等待网络相关操作）。
		for (var action : drained) {
			try {
				action.run();
			} catch (Throwable ex) { // logger.error
				logger.error("Global.Releaser pending endAction", ex);
			}
		}
		return CheckReleaseResult.NoRelease;
	}

	public static class Releaser extends Thread {
		public final @NotNull Application zeze;
		public final int globalIndex;
		public final long startTime = System.currentTimeMillis();
		private volatile boolean done;

		public Releaser(@NotNull Application zeze, int index) {
			super("Global.Releaser");
			setDaemon(true);
			this.zeze = zeze;
			this.globalIndex = index;
			logger.info("Global.Releaser Start...");
		}

		// 纯状态读，无副作用：完成回调统一走pendingEndActions（checkReleaseTimeout锁外执行），
		// 不再挂在Releaser上——曾由挂载式回调产生锁内执行、回调裸异常逃逸、并发重复执行三类问题。
		public final boolean isCompletedSuccessfully() {
			return done;
		}

		@Override
		public void run() {
			zeze.getDatabases().values().parallelStream().forEach(database ->
					database.getTables().parallelStream().forEach(table -> {
						if (!table.isMemory())
							table.reduceInvalidAllLocalOnly(globalIndex);
					}));
			logger.warn("Global.Releaser Checkpoint Start ...");
			zeze.checkpointRun();
			logger.warn("Global.Releaser Checkpoint End .");
			done = true;
		}
	}

	// 开始释放本地锁。
	// 1.【要并发，要快】启动线程池来执行，释放锁除了需要和应用互斥，没有其他IO操作，基本上都是cpu。
	// 2. 超时没有释放完成，程序中止。see tryHalt。
	// 3. 每个Global服务一个Releaser.
	public void startRelease(@NotNull Application zeze, @Nullable Runnable endAction) {
		lock();
		try {
			// 防重入：已有Releaser在运行（可能由守护线程的Release命令先启动）时不重复启动——
			// 双Releaser并发执行reduce/checkpoint无设计保证，且releaser字段被覆盖后先启动者的
			// 完成状态无人观察。AchillesHeelDaemon的调用点均有rr!=Releasing判断，GlobalClient
			// 的重连路径没有。
			if (releaser == null) {
				var r = new Releaser(zeze, globalCacheManagerHashIndex);
				releaser = r;
				r.start();
			}
			// endAction统一排队，不挂在Releaser上：完成回调与由哪个Releaser兑现无关，由
			// checkReleaseTimeout在完成后锁外执行——新建/重叠两条路径合流，回调无锁内执行路径。
			if (endAction != null)
				pendingEndActions.add(endAction);
		} finally {
			unlock();
		}
		cancelPending();
	}

	protected abstract void cancelPending();

	public abstract void keepAlive();
}
