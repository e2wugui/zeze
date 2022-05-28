using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.ProviderDirect;
using Zeze.Net;
using Zeze.Services.ServiceManager;
using Zeze.Util;

namespace Zeze.Arch
{
	public class ProviderDirectService : Zeze.Services.HandshakeBoth
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
		public ProviderApp ProviderApp;
		public readonly ConcurrentDictionary<string, ProviderSession> ProviderByLoadName = new();
		public readonly ConcurrentDictionary<int, ProviderSession> ProviderByServerId = new();

		public ProviderDirectService(string name, Zeze.Application zeze)
			: base(name, zeze)
		{
		}

		public void TryConnectAndSetReady(Agent.SubscribeState ss, ServiceInfos infos)
		{
			lock (this)
			{
				foreach (var pm in infos.SortedIdentity)
				{
					var connName = pm.PassiveIp + ":" + pm.PassivePort;
					if (ProviderByLoadName.TryGetValue(connName, out var ps))
					{
						// connection has ready.
						var mid = int.Parse(infos.ServiceName.Split('#')[1]);
						if (false == ProviderApp.Modules.TryGetValue(mid, out var m))
							throw new Exception($"Module Not Found {mid}");
						SetReady(ss, pm, ps, mid, m);
						continue;
					}
					var serverId = int.Parse(pm.Identity);
					if (serverId < Zeze.Config.ServerId)
						continue;
					if (serverId == Zeze.Config.ServerId)
					{
						var psLocal = new ProviderSession();
						psLocal.ServerId = serverId;
						SetRelativeServiceReady(psLocal, ProviderApp.DirectIp, ProviderApp.DirectPort);
						continue;
					}
					if (Config.TryGetOrAddConnector(pm.PassiveIp, pm.PassivePort, true, out var newAdd))
					{
						// 新建的Connector。开始连接。
						var psPeer = new ProviderSession();
						psPeer.ServerId = serverId;
						newAdd.UserState = psPeer;
						newAdd.Start();
					}
				}
			}
		}

        public override void OnSocketAccept(AsyncSocket socket)
        {
			if (socket.Connector == null)
            {
				// 被动连接等待对方报告信息时再处理。
				var ps = new ProviderSession();
				ps.SessionId = socket.SessionId;
				socket.UserState = ps;
			}
			base.OnSocketAccept(socket);
        }

        public override void OnHandshakeDone(AsyncSocket socket)
		{
			// call base
			base.OnHandshakeDone(socket);

			var c = socket.Connector;
			if (c != null) {
				// 主动连接。
				var ps = (ProviderSession)socket.UserState;
				ps.SessionId = socket.SessionId;
				SetRelativeServiceReady(ps, c.HostNameOrAddress, c.Port);
				var r = new AnnounceProviderInfo();
				r.Argument.Ip = ProviderApp.DirectIp;
				r.Argument.Port = ProviderApp.DirectPort;
				r.Send(socket, (_r) => Task.FromResult(0L)); // skip result
			}
		}

		internal void SetRelativeServiceReady(ProviderSession ps, String ip, int port)
		{
			lock (this)
			{
				ps.ServerLoadIp = ip;
				ps.ServerLoadPort = port;
				// 本机的连接可能设置多次。此时使用已经存在的，忽略后面的。
				if (false == ProviderByLoadName.TryAdd(ps.ServerLoadName, ps))
					return;
				ProviderByServerId[ps.ServerId] = ps;

				// 需要把所有符合当前连接目标的Provider相关的服务信息都更新到当前连接的状态。
				foreach (var ss in Zeze.ServiceManagerAgent.SubscribeStates.Values)
				{
					if (ss.ServiceName.StartsWith(ProviderApp.ServerServiceNamePrefix))
					{
						var infos = ss.ServiceInfosPending;
						if (null == infos)
							continue;
						var mid = int.Parse(ss.ServiceName.Split('#')[1]);
						if (false == ProviderApp.Modules.TryGetValue(mid, out var m))
							throw new Exception($"Module Not Found {mid}");
						foreach (var server in infos.SortedIdentity)
						{
							// 符合当前连接目标。每个Identity标识的服务的(ip,port)必须不一样。
							if (server.PassiveIp.Equals(ip) && server.PassivePort == port)
							{
								SetReady(ss, server, ps, mid, m);
							}
						}
					}
				}
			}
		}

		private void SetReady(Agent.SubscribeState ss, ServiceInfo server, ProviderSession ps,
			int mid, Zeze.Builtin.Provider.BModule m)
		{
			Console.WriteLine($"SetReady Server={Zeze.Config.ServerId} {ss.ServiceName} {server.Identity}");
			var pms = new ProviderModuleState(ps.SessionId, mid, m.ChoiceType, m.ConfigType);
			ps.GetOrAddServiceReadyState(ss.ServiceName).TryAdd(server.Identity, pms);
			ss.SetServiceIdentityReadyState(server.Identity, pms);
		}

		public override void OnSocketClose(AsyncSocket socket, Exception ex)
		{
			var ps = (ProviderSession)socket.UserState;
			if (ps != null)
			{
				foreach (var service in ps.ServiceReadyStates)
				{
					if (Zeze.ServiceManagerAgent.SubscribeStates.TryGetValue(service.Key, out var subs))
                    {
						foreach (var identity in service.Value.Keys)
						{
							subs.SetServiceIdentityReadyState(identity, null);
						}
					}
				}
				ProviderByLoadName.TryRemove(ps.ServerLoadName, out _);
				ProviderByServerId.TryRemove(ps.ServerId, out _);
			}
			base.OnSocketClose(socket, ex);
		}

		public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
		{
			if (p.TypeId == ModuleRedirect.TypeId_)
			{
				var r = (ModuleRedirect)p;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				Zeze.TaskOneByOneByKey.Execute(
					r.Argument.HashCode, factoryHandle.Handle, p, r.Argument.MethodFullName,
					(p, code) => p.SendResultCode(code));

				return;
			}

			if (p.TypeId == ModuleRedirectAllResult.TypeId_)
			{
				var r = (ModuleRedirectAllResult)p;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				_ = Mission.CallAsync(factoryHandle.Handle, p, (p, code) => p.SendResultCode(code), r.Argument.MethodFullName);

				return;
			}
			// 所有的ProviderDirectService都不启用存储过程。
			// 按收到顺序处理，不并发。这样也避免线程切换。
			_ = Mission.CallAsync(factoryHandle.Handle, p, (p, code) => p.SendResultCode(code));
		}

		public override void DispatchRpcResponse(Protocol rpc, Func<Protocol, Task<long>> responseHandle, ProtocolFactoryHandle factoryHandle)
		{
			if (rpc.TypeId == ModuleRedirect.TypeId_)
			{
				var redirect = (ModuleRedirect)rpc;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				Zeze.TaskOneByOneByKey.Execute(redirect.Argument.HashCode, responseHandle, rpc);
				return;
			}

			// no procedure.
			// 按收到顺序处理，不并发。这样也避免线程切换。
			_ = Mission.CallAsync(responseHandle, rpc);
		}
	}
}
