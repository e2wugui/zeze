package Zeze.Transaction;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Services.AchillesHeelConfig;
import Zeze.Util.Task;

public abstract class GlobalAgentBase {
	private AchillesHeelConfig config;
	private volatile long activeTime = System.currentTimeMillis();

	public final long getActiveTime() {
		return activeTime;
	}

	public final synchronized void setActiveTime(long value) {
		activeTime = value;
	}

	public final AchillesHeelConfig getConfig() {
		return config;
	}

	public GlobalAgentBase() {
		config = new AchillesHeelConfig(1500, 1000, 10 * 1000);
	}

	public final void initialize(int maxNetPing, int serverProcessTime, int serverReleaseTimeout) {
		config = new AchillesHeelConfig(maxNetPing, serverProcessTime, serverReleaseTimeout);
	}

	public abstract void keepAlive();

	private final ConcurrentHashMap<Integer, Releaser> Releasers = new ConcurrentHashMap<>();

	public enum CheckReleaseResult {
		NoRelease,
		Releasing,
		Timeout,
	}

	public CheckReleaseResult checkReleaseTimeout(int index, long now, int timeout) {
		var r = Releasers.get(index);
		if (r == null)
			return CheckReleaseResult.NoRelease;

		if (r.isCompletedSuccessfully()) {
			Releasers.remove(index);
			// 每次成功Release，设置一次活动时间，阻止AchillesHeelDaemon马上再次触发Release。
			setActiveTime(System.currentTimeMillis());
			return CheckReleaseResult.NoRelease;
		}

		return now - r.StartTime > timeout ? CheckReleaseResult.Timeout : CheckReleaseResult.Releasing;
	}

	public static class Releaser {
		public int GlobalIndex;
		public long StartTime = System.currentTimeMillis();
		public ArrayList<Future<Boolean>> Tasks = new ArrayList<>();
		public Runnable EndAction;

		public final boolean isCompletedSuccessfully() {
			try {
				for (var task : Tasks) {
					if (!task.isDone() || !task.get())
						return false;
				}
				if (null != EndAction)
					EndAction.run();
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		public Releaser(Application zeze, int index, Runnable endAction) {
			EndAction = endAction;
			GlobalIndex = index;
			for (var database : zeze.getDatabases().values()) {
				for (var table : database.getTables()) {
					if (!table.isMemory()) {
						Tasks.add(Task.getThreadPool().submit(() -> {
							table.ReduceInvalidAllLocalOnly(index);
							return true;
						}));
					}
				}
			}
		}
	}
	// 开始释放本地锁。
	// 1.【要并发，要快】启动线程池来执行，释放锁除了需要和应用互斥，没有其他IO操作，基本上都是cpu。
	// 2. 超时没有释放完成，程序中止。see tryHalt。
	// 3. 每个Global服务一个Releaser.
	public void startRelease(Application zeze, int index, Runnable endAction) {
		Releasers.put(index, new Releaser(zeze, index, endAction));
	}
}
