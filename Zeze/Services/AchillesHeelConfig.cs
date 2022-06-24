using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Services
{
	public class AchillesHeelConfig
	{
		// reconnect 这个为静态常量，仅由Server端使用。
		// Zeze.Transaction.GlobalAgent明确使用了这个常量，会覆盖网络配置。
		// Zeze.Services.GlobalCacheManagerWithRaft由于使用Raft，网络由Raft管理。没有明确使用这个设置，但是默认也是1000ms。
		// 所以如果raft.agent的网络配置的重连时间太长，那相当于只要网络断开，最后都会引起Release操作，而没有尝试重连。不会造成致命问题。
		public const int ReconnectTimer = 1000;

		// keep alive
		public int ServerKeepAliveIdleTimeout { get; }

		// [Important!]
		public int ServerDaemonTimeout { get; }
		public int ServerReleaseTimeout { get; }
		public int GlobalDaemonTimeout { get; }

		// rpc timeout
		public int ReduceTimeout { get; }
		public int AcquireTimeout { get; }
		public int KeepAliveTimeout { get; }

		public int ServerFastErrorPeriod { get; }
		public int GlobalForbidPeriod { get; }

		public int LoginTimeout { get; }

		public AchillesHeelConfig(int maxNetPing, int serverProcessTime, int serverReleaseTimeout)
		{
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
}
