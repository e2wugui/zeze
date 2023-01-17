package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
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

	public final ConcurrentHashMap<String, RedirectHandle> handles = new ConcurrentHashMap<>();
	public final ProviderApp providerApp;

	public RedirectBase(ProviderApp app) {
		providerApp = app;
	}

	public AsyncSocket choiceServer(IModule module, int serverId) {
		if (serverId == providerApp.zeze.getConfig().getServerId())
			return null; // is Local
		var ps = providerApp.providerDirectService.providerByServerId.get(serverId);
		if (ps == null)
			throw new IllegalStateException("choiceServer: not found session for serverId=" + serverId);
		var socket = providerApp.providerDirectService.GetSocket(ps.getSessionId());
		if (socket == null)
			throw new IllegalStateException("choiceServer: not found socket for serverId=" + serverId);
		return socket;
		/*
		var out = new OutLong();
		if (!ProviderApp.Distribute.choiceProviderByServerId(ProviderApp.ServerServiceNamePrefix, module.getId(), serverId, out))
			throw new IllegalStateException("choiceServer: not found server for serverId=" + serverId);
		var socket = ProviderApp.ProviderDirectService.GetSocket(out.Value);
		if (socket == null)
			throw new IllegalStateException("choiceServer: not found socket for serverId=" + serverId);
		return socket;
		*/
	}

	public AsyncSocket choiceHash(IModule module, int hash, int dataConcurrentLevel) {
		var subscribes = providerApp.zeze.getServiceManager().getSubscribeStates();
		var serviceName = ProviderDistribute.makeServiceName(providerApp.serverServiceNamePrefix, module.getId());

		var servers = subscribes.get(serviceName);
		if (servers == null)
			throw new IllegalStateException("choiceHash: not found service for serviceName=" + serviceName);

		var serviceInfo = providerApp.distribute.choiceHash(servers, hash, dataConcurrentLevel);
		if (serviceInfo == null)
			throw new IllegalStateException("choiceHash: not found server for serviceName=" + serviceName
					+ ", hash=" + hash + ", conc=" + dataConcurrentLevel);

		if (serviceInfo.getServiceIdentity().equals(String.valueOf(providerApp.zeze.getConfig().getServerId())))
			return null;

		var ps = (ProviderModuleState)servers.localStates.get(serviceInfo.getServiceIdentity());
		if (ps == null)
			throw new IllegalStateException("choiceHash: not found server for serviceIdentity="
					+ serviceInfo.getServiceIdentity());

		var socket = providerApp.providerDirectService.GetSocket(ps.sessionId);
		if (socket == null)
			throw new IllegalStateException("choiceHash: not found socket for serviceIdentity="
					+ serviceInfo.getServiceIdentity());
		return socket;
	}

	private static void addMiss(ModuleRedirectAllResult miss, int i, @SuppressWarnings("SameParameterValue") long rc) {
		var hashResult = new BModuleRedirectAllHash();
		hashResult.setReturnCode(rc);
		miss.Argument.getHashs().put(i, hashResult);
	}

	private static void addTransmits(LongHashMap<ModuleRedirectAllRequest> transmits, long provider, int index,
									 ModuleRedirectAllRequest req) {
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

	public <T extends RedirectResult> RedirectAllFuture<T> redirectAll(IModule module, ModuleRedirectAllRequest req,
																	   RedirectAllContext<T> ctx) {
		var future = ctx.getFuture();
		if (req.Argument.getHashCodeConcurrentLevel() <= 0) {
			providerApp.providerDirectService.tryRemoveManualContext(req.Argument.getSessionId());
			return future;
		}

		var transmits = new LongHashMap<ModuleRedirectAllRequest>(); // <sessionId, request>
		var miss = new ModuleRedirectAllResult();
		var serviceName = ProviderDistribute.makeServiceName(req.Argument.getServiceNamePrefix(), req.Argument.getModuleId());
		var consistent = providerApp.distribute.getConsistentHash(serviceName);
		var providers = providerApp.zeze.getServiceManager().getSubscribeStates().get(serviceName);
		var localServiceIdentity = String.valueOf(providerApp.zeze.getConfig().getServerId());
		for (int i = 0; i < req.Argument.getHashCodeConcurrentLevel(); ++i) {
			var target = providerApp.distribute.choiceDataIndex(providers, consistent, i, req.Argument.getHashCodeConcurrentLevel());
			if (target == null) {
				addMiss(miss, i, Procedure.ProviderNotExist);
				continue;
			}
			if (target.getServiceIdentity().equals(localServiceIdentity)) {
				addTransmits(transmits, 0, i, req);
				continue; // loop-back
			}
			var localState = providers.localStates.get(target.getServiceIdentity());
			if (localState == null) {
				addMiss(miss, i, Procedure.ProviderNotExist);
				continue; // not ready
			}
			addTransmits(transmits, ((ProviderModuleState)localState).sessionId, i, req);
		}

		// 转发给provider
		for (var it = transmits.iterator(); it.moveToNext(); ) {
			var sessionId = it.key();
			var request = it.value();
			var socket = providerApp.providerDirectService.GetSocket(sessionId);
			if (socket == null || !request.Send(socket)) {
				if (sessionId == 0) { // loop-back. sessionId=0应该不可能是有效的socket session,代表自己
					try {
						var service = providerApp.providerDirectService;
						request.dispatch(service, service.findProtocolFactoryHandle(request.getTypeId()));
					} catch (Exception e) {
						logger.error("", e);
					}
				} else {
					for (var hashIndex : request.Argument.getHashCodes()) {
						BModuleRedirectAllHash hashResult = new BModuleRedirectAllHash();
						hashResult.setReturnCode(Procedure.ErrorSendFail);
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
				var service = providerApp.providerDirectService;
				miss.dispatch(service, service.findProtocolFactoryHandle(miss.getTypeId()));
			} catch (Exception e) {
				logger.error("", e);
			}
		}
		return future;
	}

	public <T> RedirectFuture<T> runFuture(TransactionLevel level, Func0<RedirectFuture<T>> func) {
		Transaction t;
		if (level == TransactionLevel.None || (t = Transaction.getCurrent()) != null && t.isRunning()) {
			try {
				return func.call();
			} catch (Exception e) {
				var f = new RedirectFuture<T>();
				f.setException(e);
				return f;
			}
		}

		var future = new RedirectFuture<T>();
		// 由于返回的future暴露出来,很可能await同步等待,所以这里不能whileCommit时执行,否则会死锁等待
		Task.runUnsafe(providerApp.zeze.newProcedure(() -> {
			func.call().then(future::setResult);
			return Procedure.Success;
		}, "Redirect Loop Back", level, null), null, null, DispatchMode.Normal);
		return future;
	}

	public void runVoid(TransactionLevel level, Action0 action) {
		Transaction t;
		if (level == TransactionLevel.None || (t = Transaction.getCurrent()) != null && t.isRunning()) {
			try {
				action.run();
			} catch (Exception e) {
				logger.error("", e);
			}
			return;
		}

		// 由于此方法用于loop-back的redirect,所以这里不能whileCommit时执行,否则会死锁等待
		Task.runUnsafe(providerApp.zeze.newProcedure(() -> {
			action.run();
			return Procedure.Success;
		}, "Redirect Loop Back", level, null), null, null, DispatchMode.Normal);
	}
}
