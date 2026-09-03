package Zeze.Transaction;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
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

	// startRelease防重入期间排队的endAction（GlobalClient重连收尾等）：
	// 当前Releaser完成后由checkReleaseTimeout取出执行。仅在本对象ReentrantLock内访问。
	private final @NotNull ArrayDeque<Runnable> pendingEndActions = new ArrayDeque<>();

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
		@Nullable Runnable[] drained = null;
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
		// 排队的endAction在锁外执行（如GlobalClient的连接重启，不持锁等待网络相关操作）。
		if (drained != null) {
			for (var action : drained) {
				try {
					action.run();
				} catch (Throwable ex) { // logger.error
					logger.error("Global.Releaser pending endAction", ex);
				}
			}
		}
		// 每次成功Release，设置一次活动时间，阻止AchillesHeelDaemon马上再次触发Release。
		setActiveTime(System.currentTimeMillis());
		return CheckReleaseResult.NoRelease;
	}

	public static class Releaser extends Thread {
		public final @NotNull Application zeze;
		public final int globalIndex;
		public final long startTime = System.currentTimeMillis();
		public final @Nullable Runnable endAction;
		private volatile boolean done;
		// endAction 的一次性执行标记：isCompletedSuccessfully 会被多个守护线程并发/重复调用。
		private final AtomicBoolean endActionRan = new AtomicBoolean();

		public Releaser(@NotNull Application zeze, int index, @Nullable Runnable endAction) {
			super("Global.Releaser");
			setDaemon(true);
			this.endAction = endAction;
			this.zeze = zeze;
			this.globalIndex = index;
			logger.info("Global.Releaser Start...");
		}

		public final boolean isCompletedSuccessfully() {
			if (done) {
				// 保证 endAction 并发下只执行一次。
				if (endAction != null && endActionRan.compareAndSet(false, true))
					endAction.run();
				return true;
			}
			return false;
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
			if (releaser == null) {
				var r = new Releaser(zeze, globalCacheManagerHashIndex, endAction);
				releaser = r;
				r.start();
			} else if (endAction != null) {
				// 防重入：已有Releaser在运行（可能由守护线程的Release命令先启动）时不重复启动——
				// 双Releaser并发执行reduce/checkpoint无设计保证，且releaser字段被覆盖后先启动者的
				// 完成状态无人观察。AchillesHeelDaemon的调用点均有rr!=Releasing判断，GlobalClient
				// 的重连路径没有；调用方的endAction（连接重启等收尾）排队到当前Releaser完成后执行。
				pendingEndActions.add(endAction);
			}
		} finally {
			unlock();
		}
		cancelPending();
	}

	protected abstract void cancelPending();

	public abstract void keepAlive();
}
