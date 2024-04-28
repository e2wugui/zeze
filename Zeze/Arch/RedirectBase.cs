using System.Collections.Concurrent;
using Zeze.Net;
using System;
using Zeze.Builtin.ProviderDirect;
using static Zeze.Net.Service;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Threading;
using Zeze.Util;

namespace Zeze.Arch
{
	/**
	 * 应用需要继承实现必要的方法，创建实例并保存。(Zeze.Application.setModuleRedirect)。
	 */
	public class RedirectBase
	{
		// private static readonly ILogger logger = LogManager.GetLogger(typeof(RedirectBase));

		public ConcurrentDictionary<string, RedirectHandle> Handles { get; } = new();
		public ProviderApp ProviderApp { get; }

		public RedirectBase(ProviderApp app)
		{
			ProviderApp = app;
		}

		public AsyncSocket ChoiceServer(IModule module, int serverId)
		{
			if (serverId == ProviderApp.Zeze.Config.ServerId)
				return null; // is Local

			// 直接使用ProviderDirectService.ProviderByServerId查找。
			if (!ProviderApp.ProviderDirectService.ProviderByServerId.TryGetValue(serverId, out var ps))
				throw new Exception("Server Session Not Found. ServerId=" + serverId);
			var socket = ProviderApp.ProviderDirectService.GetSocket(ps.SessionId);
			if (null == socket)
				throw new Exception("Server Socket Not Found. ServerId=" + serverId);
			return socket;

			/*
			// 使用模块支持的服务查找。
			if (!ProviderApp.Distribute.ChoiceProviderByServerId(ProviderApp.ServerServiceNamePrefix, module.Id, serverId, out var provider))
				throw new Exception("Server Not Found. ServerId=" + serverId);
			var socket = ProviderApp.ProviderDirectService.GetSocket(provider);
			if (null == socket)
				throw new Exception("Server Socket Not Found. ServerId=" + serverId);
			return socket;
			*/
		}

		public AsyncSocket ChoiceHash(IModule module, int hash, int dataConcurrentLevel = 1)
		{
			var subscribes = ProviderApp.Zeze.ServiceManager.SubscribeStates;
			var serviceName = ProviderDistribute.MakeServiceName(ProviderApp.ServerServiceNamePrefix, module.Id);

			if (!subscribes.TryGetValue(serviceName, out var providers))
				return null;

			var serviceInfo = ProviderApp.Distribute.ChoiceHash(providers, hash, dataConcurrentLevel);
			if (serviceInfo == null || serviceInfo.ServiceIdentity.Equals(ProviderApp.Zeze.Config.ServerId.ToString()))
				return null;

			if (false == providers.LocalStates.TryGetValue(serviceInfo.ServiceIdentity, out var localState))
				return null;

			if (localState is not ProviderModuleState providerModuleState)
				return null;

			return ProviderApp.ProviderDirectService.GetSocket(providerModuleState.SessionId);
		}

		private static void AddMiss(ModuleRedirectAllResult miss, int hash, long code)
        {
            var tempVar = new BModuleRedirectAllHash
            {
                ReturnCode = code
            };
            miss.Argument.Hashs.Add(hash, tempVar);
		}

		private static void AddTransmits(Dictionary<long, ModuleRedirectAllRequest> transmits,
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
            
			if (false == ProviderApp.Distribute.TryGetProviders(req.Argument.ServiceNamePrefix, req.Argument.ModuleId, out var providers)
				|| providers.ServiceInfosVersion.IsServiceEmpty(ProviderApp.Distribute.Version))
			{
				// 全部miss。
				for (int i = 0; i < req.Argument.HashCodeConcurrentLevel; ++i)
					AddMiss(miss, i, ResultCode.ProviderNotExist);
			}
			else
            {
				for (int i = 0; i < req.Argument.HashCodeConcurrentLevel; ++i)
				{
					var target = ProviderApp.Distribute.ChoiceDataIndex(providers, i, req.Argument.HashCodeConcurrentLevel);
					if (null == target)
					{
						AddMiss(miss, i, ResultCode.ProviderNotExist);
						continue;
					}

					if (target.ServiceIdentity.Equals(ProviderApp.Zeze.Config.ServerId.ToString()))
					{
						AddTransmits(transmits, 0, i, req);
						continue; // loop-back
					}
					if (false == providers.LocalStates.TryGetValue(target.ServiceIdentity, out var localState))
					{
						AddMiss(miss, i, ResultCode.ProviderNotExist);
						continue; // not ready
					}
					if (localState is not ProviderModuleState state)
					{
						AddMiss(miss, i, ResultCode.ProviderNotExist);
						continue; // invalid state
					}
					AddTransmits(transmits, state.SessionId, i, req);
				}
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
					// 可以用来获取一些配置什么的。
					request.Service = service;
					// 本地转发的请求Sender是null：ProviderDirect.ProcessModuleRedirectAllRequest 会处理这种情况。
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
                        var tempVar2 = new BModuleRedirectAllHash
                        {
                            ReturnCode = ResultCode.ProviderNotExist
                        };
                        miss.Argument.Hashs.Add(hashIndex, tempVar2);
					}
				}
			}

			// 没有转发成功的provider的hash分组，马上报告结果。
			if (miss.Argument.Hashs.Count > 0) {
				var service = ProviderApp.ProviderDirectService;
				// 可以用来获取一些配置什么的。
				miss.Service = service;
				// Sender Is Null. miss 是结果协议，不会访问这个了。
				miss.Dispatch(service, service.FindProtocolFactoryHandle(miss.TypeId));
			}
		}

		public void RunVoid(Action action) {
			ExecutionContext.SuppressFlow();
			Task.Run(action);
			ExecutionContext.RestoreFlow();
		}

		// 下面这两种方式可能使用独立上下文更好。
		public async Task RunAsync(Func<Task> action)
		{
			await action();
		}

		public async Task<T> RunResultAsync<T>(Func<Task<T>> action)
		{
			return await action();
		}
	}
}
