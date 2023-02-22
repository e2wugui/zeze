package Zeze.Arch;

import Zeze.Builtin.Provider.AnnounceLinkInfo;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.Kick;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ProviderImplement extends AbstractProviderImplement {
	private static final Logger logger = LogManager.getLogger(ProviderImplement.class);

	protected ProviderApp providerApp;

	void applyOnChanged(Agent.SubscribeState subState) {
		if (subState.getServiceName().equals(providerApp.linkdServiceName)) {
			// Linkd info
			providerApp.providerService.apply(subState.getServiceInfos());
		} else if (subState.getServiceName().startsWith(providerApp.serverServiceNamePrefix)) {
			// Provider info
			// 对于 SubscribeTypeSimple 是不需要 SetReady 的，为了能一致处理，就都设置上了。
			// 对于 SubscribeTypeReadyCommit 在 ApplyOnPrepare 中处理。
			if (subState.getSubscribeType() == BSubscribeInfo.SubscribeTypeSimple)
				providerApp.providerDirectService.tryConnectAndSetReady(subState, subState.getServiceInfos());
		}
	}

	void applyOnPrepare(Agent.SubscribeState subState) {
		var pending = subState.getServiceInfosPending();
		if (pending != null && pending.getServiceName().startsWith(providerApp.serverServiceNamePrefix))
			providerApp.providerDirectService.tryConnectAndSetReady(subState, pending);
	}

	/**
	 * 注册所有支持的模块服务。
	 * 包括静态动态。
	 * 注册的模块时带上用于Provider之间连接的ip，port。
	 * <p>
	 * 订阅Linkd服务。
	 * Provider主动连接Linkd。
	 */
	public void registerModulesAndSubscribeLinkd() {
		var sm = providerApp.zeze.getServiceManager();
		var identity = String.valueOf(providerApp.zeze.getConfig().getServerId());
		// 注册本provider的静态服务
		for (var it = providerApp.staticBinds.iterator(); it.moveToNext(); ) {
			sm.registerService(providerApp.serverServiceNamePrefix + it.key(), identity,
					providerApp.directIp, providerApp.directPort);
		}
		// 注册本provider的动态服务
		for (var it = providerApp.dynamicModules.iterator(); it.moveToNext(); ) {
			sm.registerService(providerApp.serverServiceNamePrefix + it.key(), identity,
					providerApp.directIp, providerApp.directPort);
		}

		// 订阅provider直连发现服务
		for (var it = providerApp.modules.iterator(); it.moveToNext(); )
			sm.subscribeService(providerApp.serverServiceNamePrefix + it.key(), it.value().getSubscribeType());

		// 订阅linkd发现服务。
		sm.subscribeService(providerApp.linkdServiceName, BSubscribeInfo.SubscribeTypeSimple);
	}

	public static void sendKick(AsyncSocket sender, long linkSid, int code, String desc) {
		new Kick(new BKick(linkSid, code, desc)).Send(sender);
	}

	@SuppressWarnings("MethodMayBeStatic")
	public ProviderUserSession newSession(Dispatch p) {
		return new ProviderUserSession(p);
	}

	@Override
	protected long ProcessDispatch(Dispatch p) {
		var sender = p.getSender();
		var linkSid = p.Argument.getLinkSid();
		try {
			var typeId = p.Argument.getProtocolType();
			var factoryHandle = providerApp.providerService.findProtocolFactoryHandle(typeId);
			if (factoryHandle == null) {
				sendKick(sender, linkSid, BKick.ErrorProtocolUnknown, "unknown protocol");
				return Procedure.LogicError;
			}
			var p2 = factoryHandle.Factory.create();
			p2.decode(ByteBuffer.Wrap(p.Argument.getProtocolData()));
			p2.setSender(sender);
			// 以下字段不再需要读了,避免ProviderUserSession引用太久,置空
			p.Argument.setProtocolData(Binary.Empty);

			var session = newSession(p);
			p2.setUserState(session);

			if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
				var log = AsyncSocket.logger;
				var level = AsyncSocket.PROTOCOL_LOG_LEVEL;
				var roleId = session.getRoleId();
				var className = p.getClass().getSimpleName();
				if (p2 instanceof Rpc) {
					var rpc = ((Rpc<?, ?>)p2);
					var rpcSessionId = rpc.getSessionId();
					if (p.isRequest())
						log.log(level, "Recv:{} {}:{} {}", roleId, className, rpcSessionId, p2.Argument);
					else {
						log.log(level, "Recv:{} {}:{}>{} {}", roleId, className, rpcSessionId,
								p2.getResultCode(), rpc.Result);
					}
				} else if (p2.getResultCode() == 0)
					log.log(level, "Recv:{} {} {}", roleId, className, p2.Argument);
				else
					log.log(level, "Recv:{} {}>{} {}", roleId, className, p2.getResultCode(), p2.Argument);
			}

			Transaction txn = Transaction.getCurrent();
			if (txn != null) {
				// 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
				Procedure proc = txn.getTopProcedure();
				//noinspection ConstantConditions
				proc.setActionName(p2.getClass().getName());
				proc.setUserState(p2.getUserState());
				return Task.call(() -> factoryHandle.Handle.handleProtocol(p2), p2, (p3, code) -> {
					p3.setResultCode(code);
					session.sendResponse(p3);
				});
			}

			if (p2.getSender().getService().getZeze() == null || factoryHandle.Level == TransactionLevel.None) {
				// 应用框架不支持事务或者协议配置了"不需要事务”
				return Task.call(() -> factoryHandle.Handle.handleProtocol(p2), p2, (p3, code) -> {
					p3.setResultCode(code);
					session.sendResponse(p3);
				});
			}

			// 创建存储过程并且在当前线程中调用。
			return Task.call(
					p2.getSender().getService().getZeze().newProcedure(() -> factoryHandle.Handle.handleProtocol(p2),
							p2.getClass().getName(), factoryHandle.Level, p2.getUserState()),
					p2, (p3, code) -> {
						p3.setResultCode(code);
						session.sendResponse(p3);
					});
		} catch (Exception ex) {
			logger.error("", ex);
			sendKick(sender, linkSid, BKick.ErrorProtocolException, ex.toString());
			return Procedure.Success;
		}
	}

	@Override
	protected long ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol) {
		//var linkSession = (ProviderService.LinkSession)protocol.getSender().getUserState();
		return Procedure.Success;
	}
}
