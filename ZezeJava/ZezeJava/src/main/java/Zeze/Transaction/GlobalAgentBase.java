package Zeze.Transaction;

import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Services.AchillesHeelConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * GlobalCacheManager 代理基类：维护 activeTime 保活时间、AchillesHeel 配置，
 * 以及 Releaser 本地锁释放流程与其完成回调队列。
 */
public abstract class GlobalAgentBase extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger(GlobalAgentBase.class);

	public final Application zeze;
	// IO 线程 initialize() 写入、daemon/业务线程无锁读取，须 volatile 保证可见性。
	private volatile AchillesHeelConfig config = new AchillesHeelConfig(1500, 10000, 60 * 1000);
	private volatile long activeTime = System.currentTimeMillis();
	protected int globalCacheManagerHashIndex;
	private volatile @Nullable Releaser releaser;

	public GlobalAgentBase(Application zeze) {
		this.zeze = zeze;
	}

	public final AchillesHeelConfig getConfig() {
		return config;
	}

	// Releaser 完成回调队列（在锁内入队，由 checkReleaseTimeout 在锁外执行）。
	private final ArrayDeque<Runnable> pendingEndActions = new ArrayDeque<>();

	private static final Runnable[] emptyEndActions = new Runnable[0];

	public final long getActiveTime() {
		return activeTime;
	}

	public final void setActiveTime(long value) {
		activeTime = value;
		// 停机窗口内 daemon 可能已置空，跳过上报。
		var daemon = zeze.getAchillesHeelDaemon();
		if (daemon != null)
			daemon.setProcessDaemonActiveTime(this, value);
	}

	public boolean isReleasing() {
		return releaser != null;
	}

	public final void initialize(int maxNetPing, int serverProcessTime, int serverReleaseTimeout) {
		config = new AchillesHeelConfig(maxNetPing, serverProcessTime, serverReleaseTimeout);
		// Login 应答可能到达于停机卸载 daemon 之后。
		var daemon = zeze.getAchillesHeelDaemon();
		if (daemon != null)
			daemon.onInitialize(this);
	}

	/** 计算最终保存截止时间（预留 2s 余量）；迟到心跳不得延长已捕获的 Releaser 截止时间。 */
	public final long getFatalSaveDeadline() {
		long deadline = activeTime + config.serverKillTimeout - 1000;
		var r = releaser;
		return r != null ? Math.min(deadline, r.saveDeadline) : deadline;
	}

	public enum CheckReleaseResult {
		NoRelease,
		Releasing,
		Timeout,
	}

	public CheckReleaseResult checkReleaseTimeout(long now, int timeout) {
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
			} else if (now >= Math.min(r.saveDeadline, getFatalSaveDeadline())
					- Math.min(Application.HALT_DEADLINE_MS, timeout / 2L))
				return CheckReleaseResult.Timeout;
			else
				return CheckReleaseResult.Releasing;
		} finally {
			unlock();
		}
		// 刷新活动时间须先于 endAction，防止守护线程立刻重触发 Release；回调在锁外执行避免持锁阻塞。
		setActiveTime(System.currentTimeMillis());
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
		public final Application zeze;
		public final int globalIndex;
		public final long startTime = System.currentTimeMillis();
		public final long saveDeadline;
		private volatile boolean done;

		public Releaser(Application zeze, int index) {
			this(zeze, index, System.currentTimeMillis() + 60_000);
		}

		private Releaser(Application zeze, int index, long saveDeadline) {
			super("Global.Releaser");
			setDaemon(true);
			this.zeze = zeze;
			this.globalIndex = index;
			this.saveDeadline = saveDeadline;
			logger.info("Global.Releaser Start...");
		}

		public final boolean isCompletedSuccessfully() {
			return done;
		}

		@Override
		public void run() {
			// 专用 dbIo 池：reduce 含阻塞 DB IO，避免占用 commonPool。
			Zeze.Util.Task.runInDbIoPool(() -> zeze.getDatabases().values().parallelStream().forEach(database ->
					database.getTables().parallelStream().forEach(table -> {
						if (!table.isMemory())
							table.reduceInvalidAllLocalOnly(globalIndex);
					})));
			logger.warn("Global.Releaser Checkpoint Start ...");
			zeze.checkpointRun();
			logger.warn("Global.Releaser Checkpoint End .");
			done = true;
		}
	}

	// 启动本地锁释放（每个 Global 服务同一时刻仅允许一个 Releaser，超时由守护线程中止进程）。
	public void startRelease(Application zeze, @Nullable Runnable endAction) {
		lock();
		try {
			if (releaser == null) {
				var r = new Releaser(zeze, globalCacheManagerHashIndex, getFatalSaveDeadline());
				releaser = r;
				r.start();
			}
			// 完成回调统一入队，由 checkReleaseTimeout 在锁外执行。
			if (endAction != null)
				pendingEndActions.add(endAction);
		} finally {
			unlock();
		}
		cancelPending();
	}

	protected abstract void cancelPending();

	// 停机关库前锁外有界等待活跃 Releaser 退出，避免其 checkpoint 与关库并发。
	public void awaitReleaser(long timeoutMillis) {
		Releaser r;
		lock();
		try {
			r = releaser;
		} finally {
			unlock();
		}
		if (r == null)
			return;
		try {
			r.join(timeoutMillis);
			if (r.isAlive())
				logger.warn("Global.Releaser still running after {}ms, continue to close databases "
						+ "(risk of close racing in-flight flush)", timeoutMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public abstract void keepAlive();
}
