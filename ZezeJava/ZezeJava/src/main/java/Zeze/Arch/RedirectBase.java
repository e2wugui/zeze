package Zeze.Arch;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.Gen.GenModule;
import Zeze.Beans.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Beans.ProviderDirect.ModuleRedirect;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Util.Action0;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 应用需要继承实现必要的方法，创建实例并保存。(Zeze.Application.setModuleRedirect)。
 */
public abstract class RedirectBase {
	private static final Logger logger = LogManager.getLogger(RedirectBase.class);
	public ConcurrentHashMap<String, RedirectHandle> Handles = new ConcurrentHashMap <>();

	public <T extends Zeze.AppBase> IModule ReplaceModuleInstance(T userApp, IModule module) {
		return GenModule.Instance.ReplaceModuleInstance(userApp, module);
	}

	public int GetChoiceHashCode() {
		throw new UnsupportedOperationException();
	}

	public ProviderApp ProviderApp;

	public RedirectBase(ProviderApp app) {
		ProviderApp = app;
	}

	public AsyncSocket ChoiceServer(IModule module, int serverId) {
		if (serverId == ProviderApp.Zeze.getConfig().getServerId())
			return null; // is Local
		var out = new OutObject<Long>();
		if (ProviderApp.Distribute.ChoiceProviderByServerId(ProviderApp.ServerServiceNamePrefix, module.getId(), serverId, out))
			return ProviderApp.ProviderService.GetSocket(out.Value);
		return null;
	}

	public AsyncSocket ChoiceHash(IModule module, int hash) {
		var subscribes = ProviderApp.Zeze.getServiceManagerAgent().getSubscribeStates();
		var serviceName = ProviderApp.Distribute.MakeServiceName(ProviderApp.ServerServiceNamePrefix, module.getId());

		var servers = subscribes.get(serviceName);
		if (null == servers)
			return null;

		var serviceInfo = ProviderApp.Distribute.ChoiceHash(servers, hash);
		if (null == serviceInfo || serviceInfo.getServiceIdentity().equals(String.valueOf(ProviderApp.Zeze.getConfig().getServerId())))
			return null;

		var providerModuleState = (ProviderModuleState)serviceInfo.getLocalState();
		if (null == providerModuleState) {
			return null;
		}

		return ProviderApp.ProviderService.GetSocket(providerModuleState.SessionId);
	}

	public void RedirectAll(IModule module, ModuleRedirectAllRequest req) {
		HashMap<Long, ModuleRedirectAllRequest> transmits = new HashMap<>();

		var miss = new ModuleRedirectAllResult();
		miss.Argument.setModuleId(req.Argument.getModuleId());
		miss.Argument.setMethodFullName(req.Argument.getMethodFullName());
		miss.Argument.setSourceProvider(req.getSender().getSessionId()); // not used
		miss.Argument.setSessionId(req.Argument.getSessionId());
		miss.Argument.setServerId(0); // 在这里没法知道逻辑服务器id，错误报告就不提供这个了。
		miss.setResultCode(ModuleRedirect.ResultCodeLinkdNoProvider);

		for (int i = 0; i < req.Argument.getHashCodeConcurrentLevel(); ++i) {
			var provider = new Zeze.Util.OutObject<Long>();
			if (ProviderApp.Distribute.ChoiceProvider(req.Argument.getServiceNamePrefix(),
					req.Argument.getModuleId(), i, provider)) {
				var exist = transmits.get(provider.Value);
				if (null == exist) {
					exist = new ModuleRedirectAllRequest();
					exist.Argument.setModuleId(req.Argument.getModuleId());
					exist.Argument.setHashCodeConcurrentLevel(req.Argument.getHashCodeConcurrentLevel());
					exist.Argument.setMethodFullName(req.Argument.getMethodFullName());
					exist.Argument.setSourceProvider(req.getSender().getSessionId());
					exist.Argument.setSessionId(req.Argument.getSessionId());
					exist.Argument.setParams(req.Argument.getParams());
					transmits.put(provider.Value, exist);
				}
				exist.Argument.getHashCodes().add(i);
			} else {
				var tempVar = new BModuleRedirectAllHash();
				tempVar.setReturnCode(Zeze.Transaction.Procedure.ProviderNotExist);
				miss.Argument.getHashs().put(i, tempVar);
			}
		}

		// 转发给provider
		for (var transmit : transmits.entrySet()) {
			var socket = ProviderApp.ProviderDirectService.GetSocket(transmit.getKey());
			if (null != socket) {
				transmit.getValue().Send(socket);
			} else {
				for (var hashindex : transmit.getValue().Argument.getHashCodes()) {
					BModuleRedirectAllHash tempVar2 = new BModuleRedirectAllHash();
					tempVar2.setReturnCode(Zeze.Transaction.Procedure.ProviderNotExist);
					miss.Argument.getHashs().put(hashindex, tempVar2);
				}
			}
		}

		// 没有转发成功的provider的hash分组，马上报告结果。
		if (miss.Argument.getHashs().size() > 0) {
			try {
				var service = ProviderApp.ProviderDirectService;
				miss.Dispatch(service, service.FindProtocolFactoryHandle(miss.getTypeId()));
			} catch (Throwable e) {
				logger.error(e);
			}
		}
	}

	public TaskCompletionSource<Long> RunFuture(Action0 action) {
		var future = new TaskCompletionSource<Long>();
		Task.run(() -> {
			try {
				action.run();
				future.SetResult(0L);
			} catch (Throwable ex) {
				future.SetException(ex);
			}
		}, "Redirect Loop Back Future");
		return future;
	}

	public void RunVoid(Action0 action) {
		Task.run(action, "Redirect Loop Back Async");
	}
}
