using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Beans.ProviderDirect;
using Zeze.Net;
using Zeze.Services.ServiceManager;
using Zeze.Util;

namespace Zeze.Arch
{
	public class ProviderDirectService : Zeze.Services.HandshakeBoth
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
		public ProviderApp ProviderApp;
		public readonly ConcurrentDictionary<String, ProviderSession> ProviderSessions = new();

		public ProviderDirectService(string name, Zeze.Application zeze)
			: base(name, zeze)
		{
		}

		public void TryConnectAndSetReady(Agent.SubscribeState ss, ServiceInfos infos)
		{
			lock (this)
			{
				foreach (var pm in infos.ServiceInfoListSortedByIdentity)
				{
					var connName = pm.PassiveIp + ":" + pm.PassivePort;
					if (ProviderSessions.TryGetValue(connName, out var ps))
					{
						// connection has ready.
						var mid = int.Parse(infos.ServiceName.Split('#')[1]);
						if (false == ProviderApp.Modules.TryGetValue(mid, out var m))
							throw new Exception($"Module Not Found {mid}");
						SetReady(ss, pm, ps, mid, m);
						continue;
					}
					var serverId = int.Parse(pm.ServiceIdentity);
					if (serverId < Zeze.Config.ServerId)
						continue;
					if (serverId == Zeze.Config.ServerId)
					{
						SetRelativeServiceReady(new ProviderSession(0), ProviderApp.DirectIp, ProviderApp.DirectPort);
						continue;
					}
					if (Config.TryGetOrAddConnector(pm.PassiveIp, pm.PassivePort, true, out var newAdd))
					{
						// 新建的Connector。开始连接。
						newAdd.Start();
					}
				}
			}
		}

		public override void OnHandshakeDone(AsyncSocket socket)
		{
			base.OnHandshakeDone(socket);

			var ps = new ProviderSession(socket.SessionId);
			socket.UserState = ps;
			var c = socket.Connector;
			if (c != null) {
				// 主动连接。
				SetRelativeServiceReady(ps, c.HostNameOrAddress, c.Port);
				var r = new AnnounceProviderInfo();
				r.Argument.Ip = ProviderApp.DirectIp;
				r.Argument.Port = ProviderApp.DirectPort;
				r.Send(socket, async (_r) => 0); // skip result
			}
			// 被动连接等待对方报告信息时再处理。
			// call base
		}

		internal void SetRelativeServiceReady(ProviderSession ps, String ip, int port)
		{
			lock (this)
			{
				ps.ServerLoadIp = ip;
				ps.ServerLoadPort = port;
				// 本机的连接可能设置多次。此时使用已经存在的，忽略后面的。
				if (false == ProviderSessions.TryAdd(ps.ServerLoadName, ps))
					return;

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
						foreach (var server in infos.ServiceInfoListSortedByIdentity)
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
			int mid, Zeze.Beans.Provider.BModule m)
		{
			var pms = new ProviderModuleState(ps.SessionId, mid, m.ChoiceType, m.ConfigType);
			ps.GetOrAddServiceReadyState(ss.ServiceName).TryAdd(server.ServiceIdentity, pms);
			ss.SetServiceIdentityReadyState(server.ServiceIdentity, pms);
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
				ProviderSessions.TryRemove(ps.ServerLoadName, out _);
			}
			base.OnSocketClose(socket, ex);
		}

		public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
		{
			// 防止Client不进入加密，直接发送用户协议。
			if (!IsHandshakeProtocol(p.TypeId)) {
				p.Sender?.VerifySecurity();
			}

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
			_= Mission.CallAsync(responseHandle, rpc);
		}
	}
}
