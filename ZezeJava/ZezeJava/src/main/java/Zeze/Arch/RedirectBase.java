package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.Gen.GenModule;
import Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action0;
import Zeze.Util.Func0;
import Zeze.Util.LongHashMap;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 应用需要继承实现必要的方法，创建实例并保存。需要继承的可能性不大, 需要的话直接设置Application.Redirect
 */
public class RedirectBase {
	private static final Logger logger = LogManager.getLogger(RedirectBase.class);

	public final ConcurrentHashMap<String, RedirectHandle> Handles = new ConcurrentHashMap<>();
	public final ProviderApp ProviderApp;

	public RedirectBase(ProviderApp app) {
		ProviderApp = app;
	}

	public static <T extends IModule> T ReplaceModuleInstance(Zeze.AppBase userApp, T module) {
		return GenModule.Instance.ReplaceModuleInstance(userApp, module);
	}

	public AsyncSocket ChoiceServer(IModule module, int serverId) {
		if (serverId == ProviderApp.Zeze.getConfig().getServerId())
			return null; // is Local
		var ps = ProviderApp.ProviderDirectService.ProviderByServerId.get(serverId);
		if (null == ps)
			throw new RuntimeException("Server Session Not Found. ServerId=" + serverId);
		var socket = ProviderApp.ProviderDirectService.GetSocket(ps.getSessionId());
		if (null == socket)
			throw new RuntimeException("Server Socket Not Found. ServerId=" + serverId);
		return socket;
		/*
		var out = new OutLong();
		if (!ProviderApp.Distribute.ChoiceProviderByServerId(ProviderApp.ServerServiceNamePrefix, module.getId(), serverId, out))
			throw new RuntimeException("Server Not Found. ServerId=" + serverId);
		var socket = ProviderApp.ProviderDirectService.GetSocket(out.Value);
		if (null == socket)
			throw new RuntimeException("Server Socket Not Found. ServerId=" + serverId);
		return socket;
		 */
	}

	public AsyncSocket ChoiceHash(IModule module, int hash, int dataConcurrentLevel) {
		var subscribes = ProviderApp.Zeze.getServiceManagerAgent().getSubscribeStates();
		var serviceName = ProviderDistribute.MakeServiceName(ProviderApp.ServerServiceNamePrefix, module.getId());

		var servers = subscribes.get(serviceName);
		if (servers == null)
			return null;

		var serviceInfo = ProviderApp.Distribute.ChoiceHash(servers, hash, dataConcurrentLevel);
		if (serviceInfo == null || serviceInfo.getServiceIdentity().equals(String.valueOf(ProviderApp.Zeze.getConfig().getServerId())))
			return null;

		var providerModuleState = (ProviderModuleState)servers.LocalStates.get(serviceInfo.getServiceIdentity());
		if (providerModuleState == null)
			return null;

		return ProviderApp.ProviderDirectService.GetSocket(providerModuleState.SessionId);
	}

	private static void AddMiss(ModuleRedirectAllResult miss, int i, @SuppressWarnings("SameParameterValue") long rc) {
		var hashResult = new BModuleRedirectAllHash();
		hashResult.setReturnCode(rc);
		miss.Argument.getHashs().put(i, hashResult);
	}

	private static void AddTransmits(LongHashMap<ModuleRedirectAllRequest> transmits, long provider, int index, ModuleRedirectAllRequest req) {
		var exist = transmits.get(provider);
		if (exist == null) {
			exist = new ModuleRedirectAllRequest();
			exist.Argument.setModuleId(req.Argument.getModuleId());
			exist.Argument.setHashCodeConcurrentLevel(req.Argument.getHashCodeConcurrentLevel());
			exist.Argument.setMethodFullName(req.Argument.getMethodFullName());
			exist.Argument.setSourceProvider(req.Argument.getSourceProvider());
			exist.Argument.setSessionId(req.Argument.getSessionId());
			exist.Argument.setParams(req.Argument.getParams());
			transmits.put(provider, exist);
		}
		exist.Argument.getHashCodes().add(index);
	}

	public <T extends RedirectResult> RedirectAllFuture<T> RedirectAll(IModule module, ModuleRedirectAllRequest req,
																	   RedirectAllContext<T> ctx) {
		var future = ctx.getFuture();
		if (req.Argument.getHashCodeConcurrentLevel() <= 0) {
			ProviderApp.ProviderDirectService.TryRemoveManualContext(req.Argument.getSessionId());
			return future;
		}

		var transmits = new LongHashMap<ModuleRedirectAllRequest>(); // <sessionId, request>
		var miss = new ModuleRedirectAllResult();
		var serviceName = ProviderDistribute.MakeServiceName(req.Argument.getServiceNamePrefix(), req.Argument.getModuleId());
		var consistent = ProviderApp.Distribute.getConsistentHash(serviceName);
		var providers = ProviderApp.Zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		var localServiceIdentity = String.valueOf(ProviderApp.Zeze.getConfig().getServerId());
		for (int i = 0; i < req.Argument.getHashCodeConcurrentLevel(); ++i) {
			var target = ProviderDistribute.ChoiceDataIndex(consistent, i, req.Argument.getHashCodeConcurrentLevel());
			if (null == target) {
				AddMiss(miss, i, Procedure.ProviderNotExist);
				continue;
			}
			if (target.getServiceIdentity().equals(localServiceIdentity)) {
				AddTransmits(transmits, 0, i, req);
				continue; // loop-back
			}
			var localState = providers.LocalStates.get(target.getServiceIdentity());
			if (localState == null) {
				AddMiss(miss, i, Procedure.ProviderNotExist);
				continue; // not ready
			}
			AddTransmits(transmits, ((ProviderModuleState)localState).SessionId, i, req);
		}

		// 转发给provider
		for (var it = transmits.iterator(); it.moveToNext(); ) {
			var sessionId = it.key();
			var request = it.value();
			var socket = ProviderApp.ProviderDirectService.GetSocket(sessionId);
			if (socket == null || !request.Send(socket)) {
				if (sessionId == 0) { // loop-back. sessionId=0应该不可能是有效的socket session,代表自己
					try {
						var service = ProviderApp.ProviderDirectService;
						request.Dispatch(service, service.FindProtocolFactoryHandle(request.getTypeId()));
					} catch (Throwable e) {
						logger.error("", e);
					}
				} else {
					for (var hashIndex : request.Argument.getHashCodes()) {
						BModuleRedirectAllHash hashResult = new BModuleRedirectAllHash();
						hashResult.setReturnCode(Procedure.ProviderNotExist);
						miss.Argument.getHashs().put(hashIndex, hashResult);
					}
				}
			}
		}

		// 没有转发成功的provider的hash分组，马上报告结果。
		if (miss.Argument.getHashs().size() > 0) {
			miss.Argument.setModuleId(req.Argument.getModuleId());
			miss.Argument.setMethodFullName(req.Argument.getMethodFullName());
			miss.Argument.setSourceProvider(req.Argument.getSourceProvider()); // not used
			miss.Argument.setSessionId(req.Argument.getSessionId());
			miss.Argument.setServerId(0); // 在这里没法知道逻辑服务器id，错误报告就不提供这个了。
			miss.setResultCode(ModuleRedirect.ResultCodeLinkdNoProvider);
			try {
				var service = ProviderApp.ProviderDirectService;
				miss.Dispatch(service, service.FindProtocolFactoryHandle(miss.getTypeId()));
			} catch (Throwable e) {
				logger.error("", e);
			}
		}
		return future;
	}

	public <T> RedirectFuture<T> RunFuture(TransactionLevel level, Func0<RedirectFuture<T>> func) {
		if (level == TransactionLevel.None) {
			try {
				return func.call();
			} catch (Throwable e) {
				var f = new RedirectFuture<T>();
				f.SetException(e);
				return f;
			}
		}

		var future = new RedirectFuture<T>();
		Task.run(ProviderApp.Zeze.NewProcedure(() -> {
			func.call().then(future::SetResult);
			return Procedure.Success;
		}, "Redirect Loop Back", level, null), null, null, DispatchMode.Normal);
		return future;
	}

	public void RunVoid(TransactionLevel level, Action0 action) {
		if (level == TransactionLevel.None) {
			try {
				action.run();
			} catch (Throwable e) {
				logger.error("", e);
			}
			return;
		}

		Task.run(ProviderApp.Zeze.NewProcedure(() -> {
			action.run();
			return Procedure.Success;
		}, "Redirect Loop Back", level, null), null, null, DispatchMode.Normal);
	}
}
