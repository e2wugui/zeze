package Zeze.Services;

public class AchillesHeelConfig {
	// reconnect 这个为静态常量，仅由Server端使用。
	// Zeze.Transaction.GlobalAgent明确使用了这个常量，会覆盖网络配置。
	// Zeze.Services.GlobalCacheManagerWithRaft由于使用Raft，网络由Raft管理。没有明确使用这个设置，但是默认也是1000ms。
	// 所以如果raft.agent的网络配置的重连时间太长，那相当于只要网络断开，最后都会引起Release操作，而没有尝试重连。不会造成致命问题。
	public static final int reconnectTimer = 1000;

	// keep alive
	public final int serverKeepAliveIdleTimeout;

	// [Important!]
	public final int serverDaemonTimeout;
	public final int serverReleaseTimeout;
	public final int globalDaemonTimeout;

	// rpc timeout
	public final int reduceTimeout;
	public final int acquireTimeout;
	public final int keepAliveTimeout;

	public final int serverFastErrorPeriod;
	public final int globalForbidPeriod;

	public final int loginTimeout;

	public AchillesHeelConfig(int maxNetPing, int serverProcessTime, int serverReleaseTimeout) {
		serverKeepAliveIdleTimeout = maxNetPing;
		serverDaemonTimeout = reconnectTimer * 8;
		this.serverReleaseTimeout = serverReleaseTimeout;
		globalDaemonTimeout = serverDaemonTimeout + this.serverReleaseTimeout + maxNetPing * 2 + 1000;

		reduceTimeout = maxNetPing + serverProcessTime;
		acquireTimeout = reduceTimeout + maxNetPing;
		keepAliveTimeout = maxNetPing;

		serverFastErrorPeriod = serverDaemonTimeout / 2;
		globalForbidPeriod = serverDaemonTimeout / 2;

		loginTimeout = acquireTimeout;
	}
}
