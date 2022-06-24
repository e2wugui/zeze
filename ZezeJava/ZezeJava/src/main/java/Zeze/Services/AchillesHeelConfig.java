package Zeze.Services;

public class AchillesHeelConfig {
	// reconnect 这个为静态常量，仅由Server端使用。
	// Zeze.Transaction.GlobalAgent明确使用了这个常量，会覆盖网络配置。
	// Zeze.Services.GlobalCacheManagerWithRaft由于使用Raft，网络由Raft管理。没有明确使用这个设置，但是默认也是1000ms。
	// 所以如果raft.agent的网络配置的重连时间太长，那相当于只要网络断开，最后都会引起Release操作，而没有尝试重连。不会造成致命问题。
	public static final int ReconnectTimer = 1000;

	// keep alive
	public final int ServerKeepAliveIdleTimeout;

	// [Important!]
	public final int ServerDaemonTimeout;
	public final int ServerReleaseTimeout;
	public final int GlobalDaemonTimeout;

	// rpc timeout
	public final int ReduceTimeout;
	public final int AcquireTimeout;
	public final int KeepAliveTimeout;

	public final int ServerFastErrorPeriod;
	public final int GlobalForbidPeriod;

	public final int LoginTimeout;

	public AchillesHeelConfig(int maxNetPing, int serverProcessTime, int serverReleaseTimeout) {
		ServerKeepAliveIdleTimeout = maxNetPing;
		ServerDaemonTimeout = ReconnectTimer * 8;
		ServerReleaseTimeout = serverReleaseTimeout;
		GlobalDaemonTimeout = ServerDaemonTimeout + ServerReleaseTimeout + maxNetPing * 2 + 1000;

		ReduceTimeout = maxNetPing + serverProcessTime;
		AcquireTimeout = ReduceTimeout + maxNetPing;
		KeepAliveTimeout = maxNetPing;

		ServerFastErrorPeriod = ServerDaemonTimeout / 2;
		GlobalForbidPeriod = ServerDaemonTimeout / 2;

		LoginTimeout = AcquireTimeout;
	}
}
