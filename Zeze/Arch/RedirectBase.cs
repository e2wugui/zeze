using System.Collections.Concurrent;
using Zeze.Net;
using System;
using Zeze.Beans.ProviderDirect;
using static Zeze.Net.Service;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Threading;

namespace Zeze.Arch
{
	/**
	 * 应用需要继承实现必要的方法，创建实例并保存。(Zeze.Application.setModuleRedirect)。
	 */
	public class RedirectBase
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
		public ConcurrentDictionary<string, RedirectHandle> Handles { get; } = new();
		public ProviderApp ProviderApp { get; }

		public RedirectBase(ProviderApp app)
		{
			ProviderApp = app;
		}

		public IModule ReplaceModuleInstance<T>(T userApp, IModule module)
			where T : AppBase
		{
			return Gen.GenModule.Instance.ReplaceModuleInstance(userApp, module);
		}

		public AsyncSocket ChoiceServer(IModule module, int serverId)
		{
			if (serverId == ProviderApp.Zeze.Config.ServerId)
				return null; // is Local

			if (!ProviderApp.Distribute.ChoiceProviderByServerId(ProviderApp.ServerServiceNamePrefix, module.Id, serverId, out var provider))
				throw new Exception("Server Not Found. ServerId=" + serverId);
			var socket = ProviderApp.ProviderDirectService.GetSocket(provider);
			if (null == socket)
				throw new Exception("Server Socket Not Found. ServerId=" + serverId);
			return socket;
		}

		public AsyncSocket ChoiceHash(IModule module, int hash)
		{
			var subscribes = ProviderApp.Zeze.ServiceManagerAgent.SubscribeStates;
			var serviceName = ProviderApp.Distribute.MakeServiceName(ProviderApp.ServerServiceNamePrefix, module.Id);

			if (!subscribes.TryGetValue(serviceName, out var servers))
				return null;

			var serviceInfo = ProviderApp.Distribute.ChoiceHash(servers, hash);
			if (serviceInfo == null || serviceInfo.ServiceIdentity.Equals(ProviderApp.Zeze.Config.ServerId.ToString()))
				return null;

			var providerModuleState = (ProviderModuleState)serviceInfo.LocalState;
			if (providerModuleState == null)
				return null;

			return ProviderApp.ProviderDirectService.GetSocket(providerModuleState.SessionId);
		}

		private void AddMiss(ModuleRedirectAllResult miss, int hash, long code)
        {
			var tempVar = new BModuleRedirectAllHash();
			tempVar.ReturnCode = code;
			miss.Argument.Hashs.Add(hash, tempVar);
		}

		private void AddTransmits(Dictionary<long, ModuleRedirectAllRequest> transmits,
			long provider, int hash, ModuleRedirectAllRequest req)
        {
			if (false == transmits.TryGetValue(provider, out var exist))
			{
				exist = new ModuleRedirectAllRequest();
				exist.Argument.ModuleId = req.Argument.ModuleId;
				exist.Argument.HashCodeConcurrentLevel = req.Argument.HashCodeConcurrentLevel;
				exist.Argument.MethodFullName = req.Argument.MethodFullName;
				exist.Argument.SourceProvider = req.Argument.SourceProvider;
				exist.Argument.SessionId = req.Argument.SessionId;
				exist.Argument.Params = req.Argument.Params;
				transmits.Add(provider, exist);
			}
			exist.Argument.HashCodes.Add(hash);
		}

		public void RedirectAll(IModule module, ModuleRedirectAllRequest req)
		{
			if (req.Argument.HashCodeConcurrentLevel <= 0)
			{
				ProviderApp.ProviderDirectService.TryRemoveManualContext<ManualContext>(req.Argument.SessionId);
				return;
			}

			Dictionary<long, ModuleRedirectAllRequest> transmits = new(); // <sessionId, request>

			var miss = new ModuleRedirectAllResult();
			miss.Argument.ModuleId = req.Argument.ModuleId;
			miss.Argument.MethodFullName = req.Argument.MethodFullName;
			miss.Argument.SourceProvider = req.Argument.SourceProvider; // not used
			miss.Argument.SessionId = req.Argument.SessionId;
			miss.Argument.ServerId = 0; // 在这里没法知道逻辑服务器id，错误报告就不提供这个了。
			miss.ResultCode = ModuleRedirect.ResultCodeLinkdNoProvider;

			for (int i = 0; i < req.Argument.HashCodeConcurrentLevel; ++i)
			{
				var target = ProviderApp.Distribute.ChoiceProvider(req.Argument.ServiceNamePrefix, req.Argument.ModuleId, i);
				if (null == target)
                {
					AddMiss(miss, i, Zeze.Transaction.Procedure.ProviderNotExist);
					continue; // miss
				}
				if (target.ServiceIdentity.Equals(ProviderApp.Zeze.Config.ServerId.ToString()))
				{
					AddTransmits(transmits, 0, i, req);
					continue; // loop-back
				}
				var state = target.LocalState as ProviderModuleState;
				if (null == state)
                {
					AddMiss(miss, i, Zeze.Transaction.Procedure.ProviderNotExist);
					continue; // not ready
				}
				AddTransmits(transmits, state.SessionId, i, req);
			}

			// 转发给provider
			foreach (var it in transmits)
			{
				long sessionId = it.Key;
				var request = it.Value;
				if (sessionId == 0)
				{
					// loop-back. see above!
					var service = ProviderApp.ProviderDirectService;
					request.Dispatch(service, service.FindProtocolFactoryHandle(request.TypeId));
					continue;
				}

				var socket = ProviderApp.ProviderDirectService.GetSocket(sessionId);
				if (socket != null)
				{
					request.Send(socket);
				}
				else
				{
					foreach (var hashIndex in request.Argument.HashCodes) {
						var tempVar2 = new BModuleRedirectAllHash();
						tempVar2.ReturnCode = Zeze.Transaction.Procedure.ProviderNotExist;
						miss.Argument.Hashs.Add(hashIndex, tempVar2);
					}
				}
			}

			// 没有转发成功的provider的hash分组，马上报告结果。
			if (miss.Argument.Hashs.Count > 0) {
				var service = ProviderApp.ProviderDirectService;
				miss.Dispatch(service, service.FindProtocolFactoryHandle(miss.TypeId));
			}
		}

		public TaskCompletionSource<long> RunFuture(Action action) {
			var future = new TaskCompletionSource<long>();
			ExecutionContext.SuppressFlow();
			_ = Task.Run(() =>
			{
				try
				{
					action.Invoke();
					future.SetResult(0L);
				}
				catch (Exception ex)
				{
					future.SetException(ex);
				}
			});
			ExecutionContext.RestoreFlow();
			return future;
		}

		public void RunVoid(Action action) {
			ExecutionContext.SuppressFlow();
			Task.Run(action);
			ExecutionContext.RestoreFlow();
		}

		public async Task RunAsync(Func<Task> action)
		{
			var future = new TaskCompletionSource<long>();
			ExecutionContext.SuppressFlow();
			_ = Task.Run(async () =>
			{
				try
				{
					await action.Invoke();
					future.SetResult(0L);
				}
				catch (Exception ex)
				{
					future.SetException(ex);
				}
			});
			ExecutionContext.RestoreFlow();
			await future.Task;
		}
	}
}