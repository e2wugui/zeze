using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Builtin.ProviderDirect;
using Zeze.Net;
using Zeze.Services.ServiceManager;
using Zeze.Util;

namespace Zeze.Arch
{
	public class ProviderDirectService : Zeze.Services.HandshakeBoth
	{
		private static readonly new ILogger logger = LogManager.GetLogger(typeof(ProviderDirectService));

		public ProviderApp ProviderApp;
		public readonly ConcurrentDictionary<string, ProviderSession> ProviderByLoadName = new();
		public readonly ConcurrentDictionary<int, ProviderSession> ProviderByServerId = new();
		public readonly ConcurrentDictionary<int, IdentityHashSet<Action>> ServerReadyEvents = new();

        public ProviderDirectService(string name, Zeze.Application zeze)
			: base(name, zeze)
		{
		}

		public void RemoveServer(ServiceInfo pm)
		{
            if (false == ProviderApp.Zeze.ServiceManager.SubscribeStates.TryGetValue(pm.ServiceIdentity, out var ss))
                return;

            lock (this)
			{
				var connName = pm.PassiveIp + ":" + pm.PassivePort;
				var conn = Config.FindConnector(connName);
				if (null != conn)
				{
					conn.Stop();
					ProviderByLoadName.TryRemove(connName, out _);
					ProviderByServerId.TryRemove(int.Parse(pm.ServiceIdentity), out _);
					ss.SetIdentityLocalState(pm.ServiceIdentity, null);
					Config.RemoveConnector(conn);
				}
			}
		}

		public void AddServer(ServiceInfo pm)
		{
			if (false == ProviderApp.Zeze.ServiceManager.SubscribeStates.TryGetValue(pm.ServiceIdentity, out var ss))
				return;

			lock (this)
			{
				var connName = pm.PassiveIp + ":" + pm.PassivePort;
				if (ProviderByLoadName.TryGetValue(connName, out var ps))
				{
					// connection has ready.
					var mid = int.Parse(pm.ServiceName.Split('#')[1]);
					if (false == ProviderApp.Modules.TryGetValue(mid, out var m))
						throw new Exception($"Module Not Found {mid}");
					SetReady(ss, pm, ps, mid, m);
					return;
				}
				var serverId = int.Parse(pm.ServiceIdentity);
				if (serverId < Zeze.Config.ServerId)
					return;
				if (serverId == Zeze.Config.ServerId)
				{
					var psLocal = new ProviderSession
					{
						ServerId = serverId
					};
					SetRelativeServiceReady(psLocal, ProviderApp.DirectIp, ProviderApp.DirectPort);
					return;
				}
				if (Config.TryGetOrAddConnector(pm.PassiveIp, pm.PassivePort, true, out var newAdd))
				{
					// 新建的Connector。开始连接。
					var psPeer = new ProviderSession
					{
						ServerId = serverId
					};
					newAdd.UserState = psPeer;
					newAdd.Start();
				}
			}
		}

        public override void OnSocketAccept(AsyncSocket socket)
        {
			if (socket.Connector == null)
            {
                // 被动连接等待对方报告信息时再处理。
                var ps = new ProviderSession
                {
                    SessionId = socket.SessionId
                };
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

        public void WaitDirectServerReady(int serverId, int timeout = 3000)
        {
            var future = new TaskCompletionSource<int>();
            Action callback = () => future.SetResult(0);
            try
            {
                WaitDirectServerReady(serverId, callback);
                future.Task.Wait(timeout);
            }
            finally
            {
                ServerReadyEvents.GetOrAdd(serverId, _ => new IdentityHashSet<Action>()).Remove(callback);
            }
        }

        public void WaitDirectServerReady(int serverId, Action callback)
        {
            lock(this)
			{
                if (!ProviderByServerId.ContainsKey(serverId))
                {
                    ServerReadyEvents.GetOrAdd(serverId, _ => new IdentityHashSet<Action>()).Add(callback);
                    return;
                }
            }
            callback(); // 锁外回调，避免死锁风险。
        }

        // under lock
        private void NotifyServerReady(int serverId)
        {
            var watchers = ServerReadyEvents.GetOrAdd(serverId, _ => new IdentityHashSet<Action>());
            foreach (var w in watchers)
            {
                try
                {
                    w();
                }
                catch (Exception ex)
				{
                    logger.Error(ex);
                }
            }
            watchers.Clear();
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
				foreach (var ss in Zeze.ServiceManager.SubscribeStates.Values)
				{
					if (ss.ServiceName.StartsWith(ProviderApp.ServerServiceNamePrefix))
					{
						if (false == ss.ServiceInfosVersion.InfosVersion.TryGetValue(0, out var infos))
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
				NotifyServerReady(ps.ServerId);
            }
		}

		private void SetReady(Agent.SubscribeState ss, ServiceInfo server, ProviderSession ps,
			int mid, Zeze.Builtin.Provider.BModule m)
		{
			Console.WriteLine($"SetReady Server={Zeze.Config.ServerId} {ss.ServiceName} {server.ServiceIdentity}");
			var pms = new ProviderModuleState(ps.SessionId, mid, m.ChoiceType, m.ConfigType);
			ps.GetOrAddServiceReadyState(ss.ServiceName).TryAdd(server.ServiceIdentity, pms);
			ss.SetIdentityLocalState(server.ServiceIdentity, pms);
		}

		public override void OnSocketClose(AsyncSocket socket, Exception ex)
		{
			var ps = (ProviderSession)socket.UserState;
			if (ps != null)
			{
				foreach (var service in ps.ServiceReadyStates)
				{
					if (Zeze.ServiceManager.SubscribeStates.TryGetValue(service.Key, out var subs))
                    {
						foreach (var identity in service.Value.Keys)
						{
							subs.SetIdentityLocalState(identity, null);
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
					(p, code) => p.TrySendResultCode(code));

				return;
			}

			if (p.TypeId == ModuleRedirectAllResult.TypeId_)
			{
				var r = (ModuleRedirectAllResult)p;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				_ = Mission.CallAsync(factoryHandle.Handle, p, (p, code) => p.TrySendResultCode(code), r.Argument.MethodFullName);

				return;
			}
			// 所有的ProviderDirectService都不启用存储过程。
			// 按收到顺序处理，不并发。这样也避免线程切换。
			_ = Mission.CallAsync(factoryHandle.Handle, p, (p, code) => p.TrySendResultCode(code));
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
