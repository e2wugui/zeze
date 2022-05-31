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

	public void tryHalt(int timeout) {

	}

	public void startReleaseLocal() {
		/*
		for (var database : client.getZeze().getDatabases().values()) {
			for (var table : database.getTables())
				table.ReduceInvalidAllLocalOnly(getGlobalCacheManagerHashIndex());
		}
		client.getZeze().CheckpointRun();
		*/
	}
}
