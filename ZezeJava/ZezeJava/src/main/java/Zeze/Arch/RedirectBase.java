package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.Gen.GenModule;
import Zeze.Beans.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Beans.ProviderDirect.ModuleRedirect;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action0;
import Zeze.Util.Func0;
import Zeze.Util.LongHashMap;
import Zeze.Util.OutLong;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 应用需要继承实现必要的方法，创建实例并保存。(Zeze.Application.setModuleRedirect)。
 */
public class RedirectBase {
	private static final Logger logger = LogManager.getLogger(RedirectBase.class);

	public final ConcurrentHashMap<String, RedirectHandle> Handles = new ConcurrentHashMap<>();
	public final ProviderApp ProviderApp;

	public RedirectBase(ProviderApp app) {
		ProviderApp = app;
	}

	public <T extends Zeze.AppBase> IModule ReplaceModuleInstance(T userApp, IModule module) {
		return GenModule.Instance.ReplaceModuleInstance(userApp, module);
	}

	public AsyncSocket ChoiceServer(IModule module, int serverId) {
		if (serverId == ProviderApp.Zeze.getConfig().getServerId())
			return null; // is Local
		var out = new OutLong();
		if (!ProviderApp.Distribute.ChoiceProviderByServerId(ProviderApp.ServerServiceNamePrefix, module.getId(), serverId, out))
			throw new RuntimeException("Server Not Found. ServerId=" + serverId);
		var socket = ProviderApp.ProviderDirectService.GetSocket(out.Value);
		if (null == socket)
			throw new RuntimeException("Server Socket Not Found. ServerId=" + serverId);
		return socket;
	}

	public AsyncSocket ChoiceHash(IModule module, int hash) {
		var subscribes = ProviderApp.Zeze.getServiceManagerAgent().getSubscribeStates();
		var serviceName = ProviderApp.Distribute.MakeServiceName(ProviderApp.ServerServiceNamePrefix, module.getId());

		var servers = subscribes.get(serviceName);
		if (servers == null)
			return null;

		var serviceInfo = ProviderApp.Distribute.ChoiceHash(servers, hash);
		if (serviceInfo == null || serviceInfo.getServiceIdentity().equals(String.valueOf(ProviderApp.Zeze.getConfig().getServerId())))
			return null;

		var providerModuleState = (ProviderModuleState)serviceInfo.getLocalState();
		if (providerModuleState == null)
			return null;

		return ProviderApp.ProviderDirectService.GetSocket(providerModuleState.SessionId);
	}

	public <T extends RedirectResult> RedirectAllFuture<T> RedirectAll(IModule module, ModuleRedirectAllRequest req,
																	   ModuleRedirectAllContext<T> ctx) {
		var future = ctx.getFuture();
		if (req.Argument.getHashCodeConcurrentLevel() <= 0) {
			ProviderApp.ProviderDirectService.TryRemoveManualContext(req.Argument.getSessionId());
			return future;
		}

		LongHashMap<ModuleRedirectAllRequest> transmits = new LongHashMap<>(); // <sessionId, request>
		var miss = new ModuleRedirectAllResult();
		var provider = new OutLong();
		for (int i = 0; i < req.Argument.getHashCodeConcurrentLevel(); ++i) {
			if (ProviderApp.Distribute.ChoiceProvider(req.Argument.getServiceNamePrefix(),
					req.Argument.getModuleId(), i, provider)) {
				var exist = transmits.get(provider.Value);
				if (exist == null) {
					exist = new ModuleRedirectAllRequest();
					exist.Argument.setModuleId(req.Argument.getModuleId());
					exist.Argument.setHashCodeConcurrentLevel(req.Argument.getHashCodeConcurrentLevel());
					exist.Argument.setMethodFullName(req.Argument.getMethodFullName());
					exist.Argument.setSourceProvider(req.Argument.getSourceProvider());
					exist.Argument.setSessionId(req.Argument.getSessionId());
					exist.Argument.setParams(req.Argument.getParams());
					transmits.put(provider.Value, exist);
				}
				exist.Argument.getHashCodes().add(i);
			} else {
				var hashResult = new BModuleRedirectAllHash();
				hashResult.setReturnCode(Zeze.Transaction.Procedure.ProviderNotExist);
				miss.Argument.getHashs().put(i, hashResult);
			}
		}

		// 转发给provider
		for (var it = transmits.iterator(); it.moveToNext(); ) {
			var sessionId = it.key();
			var request = it.value();
			var socket = ProviderApp.ProviderDirectService.GetSocket(sessionId);
			if (socket != null)
				request.Send(socket);
			else if (sessionId == 0) { // loop-back. sessionId=0应该不可能是有效的socket session,代表自己
				try {
					var service = ProviderApp.ProviderDirectService;
					request.Dispatch(service, service.FindProtocolFactoryHandle(request.getTypeId()));
				} catch (Throwable e) {
					logger.error("", e);
				}
			} else {
				for (var hashIndex : request.Argument.getHashCodes()) {
					BModuleRedirectAllHash hashResult = new BModuleRedirectAllHash();
					hashResult.setReturnCode(Zeze.Transaction.Procedure.ProviderNotExist);
					miss.Argument.getHashs().put(hashIndex, hashResult);
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
		}, "Redirect Loop Back", level, null));
		return future;
	}

	public void RunVoid(TransactionLevel level, Action0 action) {
		if (level == TransactionLevel.None) {
			try {
				action.run();
			} catch (Throwable e) {
				logger.error("", e);
			}
		}

		Task.run(ProviderApp.Zeze.NewProcedure(() -> {
			action.run();
			return Procedure.Success;
		}, "Redirect Loop Back", level, null));
	}
}
