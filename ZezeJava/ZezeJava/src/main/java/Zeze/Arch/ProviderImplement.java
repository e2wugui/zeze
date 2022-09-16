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

	protected ProviderApp ProviderApp;

	void ApplyOnChanged(Agent.SubscribeState subState) throws Throwable {
		if (subState.getServiceName().equals(ProviderApp.LinkdServiceName)) {
			// Linkd info
			ProviderApp.ProviderService.Apply(subState.getServiceInfos());
		} else if (subState.getServiceName().startsWith(ProviderApp.ServerServiceNamePrefix)) {
			// Provider info
			// 对于 SubscribeTypeSimple 是不需要 SetReady 的，为了能一致处理，就都设置上了。
			// 对于 SubscribeTypeReadyCommit 在 ApplyOnPrepare 中处理。
			if (subState.getSubscribeType() == BSubscribeInfo.SubscribeTypeSimple)
				ProviderApp.ProviderDirectService.TryConnectAndSetReady(subState, subState.getServiceInfos());
		}
	}

	void ApplyOnPrepare(Agent.SubscribeState subState) throws Throwable {
		var pending = subState.getServiceInfosPending();
		if (pending != null && pending.getServiceName().startsWith(ProviderApp.ServerServiceNamePrefix))
			ProviderApp.ProviderDirectService.TryConnectAndSetReady(subState, pending);
	}

	/**
	 * 注册所有支持的模块服务。
	 * 包括静态动态。
	 * 注册的模块时带上用于Provider之间连接的ip，port。
	 * <p>
	 * 订阅Linkd服务。
	 * Provider主动连接Linkd。
	 */
	public void RegisterModulesAndSubscribeLinkd() {
		var sm = ProviderApp.Zeze.getServiceManagerAgent();
		var identity = String.valueOf(ProviderApp.Zeze.getConfig().getServerId());
		// 注册本provider的静态服务
		for (var it = ProviderApp.StaticBinds.iterator(); it.moveToNext(); ) {
			sm.registerService(ProviderApp.ServerServiceNamePrefix + it.key(), identity,
					ProviderApp.DirectIp, ProviderApp.DirectPort);
		}
		// 注册本provider的动态服务
		for (var it = ProviderApp.DynamicModules.iterator(); it.moveToNext(); ) {
			sm.registerService(ProviderApp.ServerServiceNamePrefix + it.key(), identity,
					ProviderApp.DirectIp, ProviderApp.DirectPort);
		}

		// 订阅provider直连发现服务
		for (var it = ProviderApp.Modules.iterator(); it.moveToNext(); )
			sm.subscribeService(ProviderApp.ServerServiceNamePrefix + it.key(), it.value().getSubscribeType());

		// 订阅linkd发现服务。
		sm.subscribeService(ProviderApp.LinkdServiceName, BSubscribeInfo.SubscribeTypeSimple);
	}

	public static void SendKick(AsyncSocket sender, long linkSid, int code, String desc) {
		new Kick(new BKick(linkSid, code, desc)).Send(sender);
	}

	@Override
	protected long ProcessDispatch(Dispatch p) {
		var sender = p.getSender();
		var linkSid = p.Argument.getLinkSid();
		try {
			var factoryHandle = ProviderApp.ProviderService.findProtocolFactoryHandle(p.Argument.getProtocolType());
			if (factoryHandle == null) {
				SendKick(sender, linkSid, BKick.ErrorProtocolUnknown, "unknown protocol");
				return Procedure.LogicError;
			}
			var p2 = factoryHandle.Factory.create();
			p2.decode(ByteBuffer.Wrap(p.Argument.getProtocolData()));
			p2.setSender(sender);
			// 以下字段不再需要读了,避免ProviderUserSession引用太久,置空
			p.Argument.setProtocolData(Binary.Empty);
			p.Argument.setContextx(Binary.Empty);

			var session = new ProviderUserSession(p);
			p2.setUserState(session);

			if (AsyncSocket.ENABLE_PROTOCOL_LOG) {
				if (p2.isRequest()) {
					if (p2 instanceof Rpc)
						AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "DISP[{}] {}({}): {}", linkSid,
								p2.getClass().getSimpleName(), ((Rpc<?, ?>)p2).getSessionId(), p2.Argument);
					else
						AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "DISP[{}] {}: {}", linkSid,
								p2.getClass().getSimpleName(), p2.Argument);
				} else
					AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "DISP[{}] {}({})>{} {}", linkSid,
							p2.getClass().getSimpleName(), ((Rpc<?, ?>)p2).getSessionId(), p2.getResultCode(),
							p2.getResultBean());
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
		} catch (Throwable ex) {
			SendKick(sender, linkSid, BKick.ErrorProtocolException, ex.toString());
			logger.error("", ex);
			return Procedure.Success;
		}
	}

	@Override
	protected long ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol) {
		//var linkSession = (ProviderService.LinkSession)protocol.getSender().getUserState();
		return Procedure.Success;
	}
}
