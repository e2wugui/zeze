using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;

namespace Zeze.Arch
{
    public class ProviderSession
    {
		public long SessionId { get; }
		private volatile BLoad Load_ = new BLoad();
		public BLoad Load { get { return Load_; } set { Load_ = value; } }
		public string ServerLoadIp { get; set; } = "";
		public int ServerLoadPort { get; set; }
		public string ServerLoadName => ServerLoadIp + ":" + ServerLoadPort;

		public ProviderSession(long sid)
        {
			SessionId = sid;
        }

		/**
		 * 下面维护和本Session相关的订阅Ready状态。在Session关闭时需要取消Ready状态。
		 * 【仅用于ProviderApp】
		 */
		public ConcurrentDictionary<string, ConcurrentDictionary<string, ProviderModuleState>> ServiceReadyStates = new();

		public ConcurrentDictionary<string, ProviderModuleState> GetOrAddServiceReadyState(string serviceName)
		{
			return ServiceReadyStates.GetOrAdd(serviceName, (key) => new());
		}
	}
}
