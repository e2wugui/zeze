package Zeze.Arch;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.ByteBuffer;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 应用需要继承实现必要的方法，创建实例并保存。需要继承的可能性不大, 需要的话直接设置Application.Redirect
 */
public class RedirectBase {
	private static final Logger logger = LogManager.getLogger(RedirectBase.class);

	public final ConcurrentHashMap<String, RedirectHandle> handles = new ConcurrentHashMap<>();
	public final @NotNull ProviderApp providerApp;

	public RedirectBase(@NotNull ProviderApp app) {
		providerApp = app;
	}

	public @Nullable AsyncSocket choiceServer(@NotNull IModule module, int serverId) {
		return choiceServer(module, serverId, false);
	}

	public @Nullable AsyncSocket choiceServer(@NotNull IModule module, int serverId, boolean orOtherServer) {
		if (serverId == providerApp.zeze.getConfig().getServerId()) {
			if (!providerApp.isStartLast()) {
				logger.warn("", new IllegalStateException("choiceServer: not after startLast for module="
						+ module.getFullName() + ", serverId=" + serverId));
			}
			return null; // is Local
		}
		var ps = providerApp.providerDirectService.providerByServerId.get(serverId);
		if (ps == null) {
			if (!providerApp.isStartLast()) {
				throw new IllegalStateException("choiceServer: not after startLast for module="
						+ module.getFullName() + ", serverId=" + serverId);
			}
			providerApp.providerDirectService.waitDirectServerReady(serverId, 120_000);
			ps = providerApp.providerDirectService.providerByServerId.get(serverId);
			if (ps == null) {
				if (orOtherServer)
					return choiceHash(module, serverId, 1);
				throw new RedirectException(RedirectException.SERVER_NOT_FOUND,
						"choiceServer: not found session for serverId=" + serverId);
			}
		}
		var socket = providerApp.providerDirectService.GetSocket(ps.getSessionId());
		if (socket == null || socket.isClosed()) {
			if (!providerApp.isStartLast()) {
				throw new IllegalStateException("choiceServer: not after startLast for module="
						+ module.getFullName() + ", serverId=" + serverId);
			}
			providerApp.providerDirectService.waitDirectServerReady(serverId, 120_000);
			socket = providerApp.providerDirectService.GetSocket(ps.getSessionId());
			if (socket == null || socket.isClosed()) {
				if (orOtherServer)
					return choiceHash(module, serverId, 1);
				throw new RedirectException(RedirectException.SERVER_NOT_FOUND,
						"choiceServer: not found socket for serverId=" + serverId);
			}
		}
		return socket;
		/*
		var out = new OutLong();
		if (!ProviderApp.Distribute.choiceProviderByServerId(ProviderApp.ServerServiceNamePrefix, module.getId(), serverId, out))
			throw new ServerNotFoundException("choiceServer: not found server for serverId=" + serverId);
		var socket = ProviderApp.ProviderDirectService.GetSocket(out.Value);
		if (socket == null)
			throw new ServerNotFoundException("choiceServer: not found socket for serverId=" + serverId);
		return socket;
		*/
	}

	public @Nullable AsyncSocket choiceHash(@NotNull IModule module, int hash, int dataConcurrentLevel) {
		var subscribes = providerApp.zeze.getServiceManager().getSubscribeStates();
		var serviceName = ProviderDistribute.makeServiceName(providerApp.serverServiceNamePrefix, module.getId());

		var servers = subscribes.get(serviceName);
		if (servers == null) {
			throw new RedirectException(RedirectException.SERVER_NOT_FOUND,
					"choiceHash: not found service for serviceName=" + serviceName);
		}

		var serviceInfo = providerApp.distribute.choiceHash(servers, hash, dataConcurrentLevel);
		if (serviceInfo == null) {
			throw new RedirectException(RedirectException.SERVER_NOT_FOUND,
					"choiceHash: not found server for serviceName=" + serviceName + ", hash=" + hash
							+ ", conc=" + dataConcurrentLevel);
		}

		if (serviceInfo.getServiceIdentity().equals(String.valueOf(providerApp.zeze.getConfig().getServerId())))
			return null;

		AsyncSocket socket;
		var service = providerApp.providerDirectService;
		var ps = (ProviderModuleState)servers.getLocalStates().get(serviceInfo.getServiceIdentity());
		if (ps != null && (socket = service.GetSocket(ps.sessionId)) != null && !socket.isClosed())
			return socket;

		if (dataConcurrentLevel <= 1) {
			servers.lock();
			try {
				for (int i = 0, n = servers.getLocalStates().size(); i < n; i++) {
					var e = servers.getNextStateEntry();
					if (e == null)
						break;
					socket = service.GetSocket(((ProviderModuleState)e.getValue()).sessionId);
					if (socket != null && !socket.isClosed())
						return socket;
				}
			} finally {
				servers.unlock();
			}
		}

		throw new RedirectException(RedirectException.SERVER_NOT_FOUND,
				"choiceHash: not found socket for serviceName=" + serviceName + ", hash=" + hash
						+ ", conc=" + dataConcurrentLevel + ", serverId=" + serviceInfo.getServiceIdentity()
						+ ", count=" + servers.getLocalStates().size());
	}

	private static void addMiss(@NotNull ModuleRedirectAllResult miss, int i,
								@SuppressWarnings("SameParameterValue") long rc) {
		miss.Argument.getHashs().put(i, new BModuleRedirectAllHash.Data(rc, null));
	}

	private static void addTransmits(@NotNull LongHashMap<ModuleRedirectAllRequest> transmits, long provider, int index,
									 @NotNull ModuleRedirectAllRequest req) {
		var exist = transmits.get(provider);
		if (exist == null) {
			exist = new ModuleRedirectAllRequest();
			exist.Argument.setModuleId(req.Argument.getModuleId());
			exist.Argument.setHashCodeConcurrentLevel(req.Argument.getHashCodeConcurrentLevel());
			exist.Argument.setMethodFullName(req.Argument.getMethodFullName());
			exist.Argument.setSourceProvider(req.Argument.getSourceProvider());
			exist.Argument.setSessionId(req.Argument.getSessionId());
			exist.Argument.setParams(req.Argument.getParams());
			exist.Argument.setVersion(req.Argument.getVersion());
			transmits.put(provider, exist);
		}
		exist.Argument.getHashCodes().add(index);
	}

	public <T extends RedirectResult> RedirectAllFuture<T> redirectAll(@NotNull IModule module,
																	   @NotNull ModuleRedirectAllRequest req,
																	   @NotNull RedirectAllContext<T> ctx) {
		var future = ctx.getFuture();
		var arg = req.Argument;
		if (arg.getHashCodeConcurrentLevel() <= 0) {
			providerApp.providerDirectService.tryRemoveManualContext(arg.getSessionId());
			return future;
		}

		var transmits = new LongHashMap<ModuleRedirectAllRequest>(); // <sessionId, request>
		var miss = new ModuleRedirectAllResult();
		var serviceName = ProviderDistribute.makeServiceName(arg.getServiceNamePrefix(), arg.getModuleId());
		var consistent = providerApp.distribute.getConsistentHash(serviceName);
		var providers = providerApp.zeze.getServiceManager().getSubscribeStates().get(serviceName);
		var localServiceIdentity = String.valueOf(providerApp.zeze.getConfig().getServerId());
		for (int i = 0; i < arg.getHashCodeConcurrentLevel(); ++i) {
			var target = providerApp.distribute.choiceDataIndex(providers, consistent, i,
					arg.getHashCodeConcurrentLevel());
			if (target == null) {
				addMiss(miss, i, Procedure.ProviderNotExist);
				continue;
			}
			if (target.getServiceIdentity().equals(localServiceIdentity)) {
				addTransmits(transmits, 0, i, req);
				continue; // loop-back
			}
			var localState = providers.getLocalStates().get(target.getServiceIdentity());
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
						// 为了完整支持事务重置传入的协议，这里需要编码一次。
						var bb = ByteBuffer.Allocate(32);
						request.encode(bb);
						service.dispatchProtocol(request.getTypeId(), bb,
								Objects.requireNonNull(service.findProtocolFactoryHandle(request.getTypeId())), null);
					} catch (Exception e) {
						logger.error("", e);
					}
				} else {
					for (var hashIndex : request.Argument.getHashCodes()) {
						miss.Argument.getHashs().put(hashIndex,
								new BModuleRedirectAllHash.Data(Procedure.ErrorSendFail, null));
					}
				}
			}
		}

		// 没有转发成功的provider的hash分组，马上报告结果。
		if (!miss.Argument.getHashs().isEmpty()) {
			miss.Argument.setModuleId(arg.getModuleId());
			miss.Argument.setMethodFullName(arg.getMethodFullName());
			miss.Argument.setSourceProvider(arg.getSourceProvider()); // not used
			miss.Argument.setSessionId(arg.getSessionId());
			miss.Argument.setServerId(0); // 在这里没法知道逻辑服务器id，错误报告就不提供这个了。
			miss.setResultCode(ModuleRedirect.ResultCodeLinkdNoProvider);
			try {
				var service = providerApp.providerDirectService;
				var bb = ByteBuffer.Allocate(32);
				miss.encode(bb);
				service.dispatchProtocol(miss.getTypeId(), bb,
						Objects.requireNonNull(service.findProtocolFactoryHandle(miss.getTypeId())), null);
			} catch (Exception e) {
				logger.error("", e);
			}
		}
		return future;
	}

	public <T> @NotNull RedirectFuture<T> runFuture(@Nullable TransactionLevel level,
													@NotNull Func0<RedirectFuture<T>> func) {
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
		Task.executeUnsafe(providerApp.zeze.newProcedure(() -> {
			try {
				func.call().onSuccess(future::setResult).onFail(future::setException);
			} catch (Exception e) {
				future.setException(e);
				throw e;
			}
			return Procedure.Success;
		}, "Redirect Loop Back", level, null), DispatchMode.Normal);
		return future;
	}

	public void runVoid(@Nullable TransactionLevel level, @NotNull Action0 action) {
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
		Task.executeUnsafe(providerApp.zeze.newProcedure(() -> {
			action.run();
			return Procedure.Success;
		}, "Redirect Loop Back", level, null), DispatchMode.Normal);
	}
}
