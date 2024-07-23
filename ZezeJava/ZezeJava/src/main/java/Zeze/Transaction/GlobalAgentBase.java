package Zeze.Transaction;

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
		var r = releaser;
		if (r == null)
			return CheckReleaseResult.NoRelease;

		if (r.isCompletedSuccessfully()) {
			logger.info("Global.Releaser End.");
			releaser = null;
			// 每次成功Release，设置一次活动时间，阻止AchillesHeelDaemon马上再次触发Release。
			setActiveTime(System.currentTimeMillis());
			return CheckReleaseResult.NoRelease;
		}

		if (now - r.startTime > timeout)
			return CheckReleaseResult.Timeout;

		return CheckReleaseResult.Releasing;
	}

	public static class Releaser extends Thread {
		public final @NotNull Application zeze;
		public final int globalIndex;
		public final long startTime = System.currentTimeMillis();
		public final @Nullable Runnable endAction;
		private volatile boolean done;

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
				if (endAction != null)
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
			var r = new Releaser(zeze, globalCacheManagerHashIndex, endAction);
			releaser = r;
			r.start();
		} finally {
			unlock();
		}
		cancelPending();
	}

	protected abstract void cancelPending();

	public abstract void keepAlive();
}
