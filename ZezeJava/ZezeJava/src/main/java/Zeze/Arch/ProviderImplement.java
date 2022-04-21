package Zeze.Arch;

import java.util.HashMap;
import Zeze.Builtin.Provider.AnnounceLinkInfo;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.Kick;
import Zeze.Net.AsyncSocket;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.SubscribeInfo;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Str;

public abstract class ProviderImplement extends AbstractProviderImplement {
	//private static final Logger logger = LogManager.getLogger(ProviderImplement.class);

	public ProviderApp ProviderApp;

	void ApplyOnChanged(Agent.SubscribeState subState) {
		if (subState.getServiceName().equals(ProviderApp.LinkdServiceName)) {
			ProviderApp.ProviderService.Apply(subState.getServiceInfos());
		}
		/*
		else if (subState.getServiceName().startsWith(ProviderApp.ServerServiceNamePrefix)){
			System.out.println("ServerId=" + ProviderApp.Zeze.getConfig().getServerId()
			+ " OnChanged=" + subState.getServiceInfos());
			//this.ProviderApp.ProviderDirectService.TryConnectAndSetReady(subState, subState.getServiceInfos());
		}
		*/
	}

	void ApplyOnPrepare(Agent.SubscribeState subState) {
		var pending = subState.getServiceInfosPending();
		if (pending == null)
			return;

		if (pending.getServiceName().startsWith(ProviderApp.ServerServiceNamePrefix)) {
			this.ProviderApp.ProviderDirectService.TryConnectAndSetReady(subState, pending);
		}
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
		var services = new HashMap<String, BModule>();
		// 注册本provider的静态服务
		for (var it = ProviderApp.StaticBinds.iterator(); it.moveToNext(); ) {
			var name = Str.format("{}{}", ProviderApp.ServerServiceNamePrefix, it.key());
			var identity = String.valueOf(ProviderApp.Zeze.getConfig().getServerId());
			sm.RegisterService(name, identity, ProviderApp.DirectIp, ProviderApp.DirectPort);
			services.put(name, it.value());
		}
		// 注册本provider的动态服务
		for (var it = ProviderApp.DynamicModules.iterator(); it.moveToNext(); ) {
			var name = Str.format("{}{}", ProviderApp.ServerServiceNamePrefix, it.key());
			var identity = String.valueOf(ProviderApp.Zeze.getConfig().getServerId());
			sm.RegisterService(name, identity, ProviderApp.DirectIp, ProviderApp.DirectPort);
			services.put(name, it.value());
		}

		// 订阅provider直连发现服务
		for (var e : services.entrySet()) {
			sm.SubscribeService(e.getKey(), e.getValue().getSubscribeType());
		}

		// 订阅linkd发现服务。
		sm.SubscribeService(ProviderApp.LinkdServiceName, SubscribeInfo.SubscribeTypeSimple);
	}

	private void SendKick(AsyncSocket sender, long linkSid, int code, String desc) {
		var p = new Kick();
		p.Argument.setLinksid(linkSid);
		p.Argument.setCode(code);
		p.Argument.setDesc(desc);
		p.Send(sender);
	}

	@Override
	protected long ProcessDispatch(Dispatch p) {
		try {
			var factoryHandle = ProviderApp.ProviderService.FindProtocolFactoryHandle(p.Argument.getProtocolType());
			if (factoryHandle == null) {
				SendKick(p.getSender(), p.Argument.getLinkSid(), BKick.ErrorProtocolUnkown, "unknown protocol");
				return Procedure.LogicError;
			}
			var p2 = factoryHandle.Factory.create();
			p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.getProtocolData()));
			p2.setSender(p.getSender());

			var session = new ProviderUserSession(ProviderApp.ProviderService, p.Argument.getAccount(),
					p.Argument.getStates(), p.getSender(), p.Argument.getLinkSid());

			p2.setUserState(session);
			Transaction txn = Transaction.getCurrent();
			if (txn != null) {
				// 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
				Procedure proc = txn.getTopProcedure();
				assert proc != null;
				proc.setActionName(p2.getClass().getName());
				proc.setUserState(p2.getUserState());
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle.handleProtocol(p2), p2, (p3, code) -> {
					p3.setResultCode(code);
					session.SendResponse(p3);
				});
			}

			if (p2.getSender().getService().getZeze() == null || factoryHandle.Level == TransactionLevel.None) {
				// 应用框架不支持事务或者协议配置了"不需要事务”
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle.handleProtocol(p2), p2, (p3, code) -> {
					p3.setResultCode(code);
					session.SendResponse(p3);
				});
			}

			// 创建存储过程并且在当前线程中调用。
			return Zeze.Util.Task.Call(
					p2.getSender().getService().getZeze().NewProcedure(() -> factoryHandle.Handle.handleProtocol(p2),
							p2.getClass().getName(), factoryHandle.Level, p2.getUserState()),
					p2, (p3, code) -> {
						p3.setResultCode(code);
						session.SendResponse(p3);
					});
		} catch (Throwable ex) {
			SendKick(p.getSender(), p.Argument.getLinkSid(), BKick.ErrorProtocolException, ex.toString());
			throw ex;
		}
	}

	@Override
	protected long ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol) {
		var linkSession = (ProviderService.LinkSession)protocol.getSender().getUserState();
		linkSession.Setup(protocol.Argument.getLinkId(), protocol.Argument.getProviderSessionId());
		return Procedure.Success;
	}
}
