package Zeze.Transaction;

import Zeze.Net.AsyncSocket;
import Zeze.Services.AchillesHeelConfig;

public abstract class GlobalAgentBase {
	private AchillesHeelConfig config;
	private long activeTime;

	public final long getActiveTime() {
		return activeTime;
	}

	public final void setActiveTime(long value) {
		activeTime = value;
	}

	public final AchillesHeelConfig getConfig() {
		return config;
	}

	public final void initialize(int maxNetPing, int serverProcessTime, int serverReleaseTimeout) {
		config = new AchillesHeelConfig(maxNetPing, serverProcessTime, serverReleaseTimeout);
	}

	public abstract void keepAlive();

	// 如果正在释放本地锁，并且超时了，立即杀死程序。
	public void tryHalt(int index, int timeout) {

	}

	// 开始释放本地锁。
	// 1.【要并发，要快】启动线程池来执行，释放锁除了需要和应用互斥，没有其他IO操作，基本上都是cpu。
	// 2. 超时没有释放完成，程序中止。see tryHalt。
	// 3. Per Global Instance.
	public void startReleaseLocal(int index) {
		/*
		for (var database : client.getZeze().getDatabases().values()) {
			for (var table : database.getTables())
				table.ReduceInvalidAllLocalOnly(getGlobalCacheManagerHashIndex());
		}
		client.getZeze().CheckpointRun();
		*/
	}
}
