package Zeze.Arch;

import Zeze.Arch.Beans.BSend;
import Zeze.Builtin.Provider.AnnounceLinkInfo;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.Kick;
import Zeze.Builtin.Provider.Send;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutObject;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ProviderImplement extends AbstractProviderImplement {
	protected static final @NotNull Logger logger = LogManager.getLogger(ProviderImplement.class);
	private static final ThreadLocal<Dispatch> localDispatch = new ThreadLocal<>();

	protected ProviderApp providerApp;
	private volatile int controlKick = BKick.eControlClose;

	public void setControlKick(int control) {
		controlKick = control;
	}

	public abstract @Nullable ProviderLoadBase getLoad();

	public static @Nullable Dispatch localDispatch() {
		return localDispatch.get();
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
		var edit = new BEditService();
		var appVersion = providerApp.zeze.getConfig().getAppVersion();
		// 注册本provider的静态服务
		for (var it = providerApp.staticBinds.iterator(); it.moveToNext(); ) {
			edit.getAdd().add(new BServiceInfo(providerApp.serverServiceNamePrefix + it.key(), identity, appVersion,
					providerApp.directIp, providerApp.directPort));
		}
		// 注册本provider的动态服务
		for (var it = providerApp.dynamicModules.iterator(); it.moveToNext(); ) {
			edit.getAdd().add(new BServiceInfo(providerApp.serverServiceNamePrefix + it.key(), identity, appVersion,
					providerApp.directIp, providerApp.directPort));
		}
		sm.editService(edit);

		// 订阅服务
		var sub = new BSubscribeArgument();
		// 订阅provider直连发现服务
		for (var it = providerApp.modules.iterator(); it.moveToNext(); )
			sub.subs.add(new BSubscribeInfo(providerApp.serverServiceNamePrefix + it.key(), appVersion));
		// 订阅linkd发现服务。
		sub.subs.add(new BSubscribeInfo(providerApp.linkdServiceName, 0)); // link 服务没有使用版本号。
		sm.subscribeServices(sub);
	}

	public static void sendKick(@Nullable AsyncSocket sender, long linkSid, int code, @NotNull String desc) {
		sendKick(sender, linkSid, code, desc, BKick.eControlClose);
	}

	public static void sendKick(@Nullable AsyncSocket sender, long linkSid, int code, @NotNull String desc,
								int control) {
		if (!AsyncSocket.ENABLE_PROTOCOL_LOG) {
			logger.info("sendKick[{}]: linkSid={}, code={}, desc={}",
					sender != null ? sender.getSessionId() : null, linkSid, code, desc);
		}
		new Kick(new BKick.Data(linkSid, code, desc, control)).Send(sender);
	}

	@SuppressWarnings("MethodMayBeStatic")
	public @NotNull ProviderUserSession newSession(@NotNull Dispatch p) {
		return new ProviderUserSession(p);
	}

	private @Nullable String getAuthFlags(@NotNull String account, long typeId) {
		var auth = providerApp.zeze.getAuth();
		if (null == auth)
			return "";
		return auth.getAccountAuth(account, typeId);
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@Override
	protected long ProcessDispatch(@NotNull Dispatch p) {
		var sender = p.getSender();
		var arg = p.Argument;
		var linkSid = arg.getLinkSid();
		var typeId = arg.getProtocolType();
		Protocol<?> p2 = null;

		// 先检查是否未知协议
		var factoryHandle = providerApp.providerService.findProtocolFactoryHandle(typeId);
		if (factoryHandle == null) {
			sendKick(sender, linkSid, BKick.ErrorProtocolUnknown, "unknown protocol: " + typeId, controlKick);
			return Procedure.LogicError;
		}

		// 验证协议权限
		var authFlags = getAuthFlags(arg.getAccount(), typeId);
		if (authFlags == null) {
			sendKick(sender, linkSid, BKick.ErrorAuth, "auth fail " + typeId, controlKick);
			return Procedure.AuthFail;
		}

		// 根据负载和协议级别处理熔断
		var load = getLoad();
		if (load != null) {
			var overload = load.getOverload().getOverload();
			if (overload != BLoad.eWorkFine) {
				if (overload == BLoad.eThreshold && factoryHandle.CriticalLevel == Protocol.eSheddable ||
						overload == BLoad.eOverload && factoryHandle.CriticalLevel != Protocol.eCriticalPlus) {
					var pdata = arg.getProtocolData();
					if (pdata.size() > 0 && (pdata.get(0) & FamilyClass.FamilyClassMask) == FamilyClass.Request) {
						// 简单构造并回复该RPC
						var bb = pdata.Wrap();
						var header = bb.ReadInt();
						if ((header & FamilyClass.BitResultCode) != 0)
							bb.SkipLong(); // resultCode
						var sessionId = bb.ReadLong();
						bb = ByteBuffer.Allocate(24);
						bb.WriteInt4(Protocol.getModuleId(typeId));
						bb.WriteInt4(Protocol.getProtocolId(typeId));
						int saveSize = bb.BeginWriteWithSize4();
						bb.WriteInt(FamilyClass.Response | FamilyClass.BitResultCode);
						bb.WriteLong(Procedure.Busy);
						bb.WriteLong(sessionId);
						EmptyBean.instance.encode(bb);
						bb.EndWriteWithSize4(saveSize);
						var pSend = new Send(new BSend(typeId, new Binary(bb)));
						pSend.Argument.getLinkSids().add(linkSid);
						pSend.Send(sender);
					}
					return 0;
				}
			}
		}

		try {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			localDispatch.set(p);
			int psize = arg.getProtocolData().size();
			var session = newSession(p);
			session.setAuthFlags(authFlags);
			var zeze = sender.getService().getZeze();
			var txn = Transaction.getCurrent();
			var outRpcContext = new OutObject<Rpc<?, ?>>();
			if (txn == null && zeze != null && factoryHandle.Level != TransactionLevel.None) {
				var outProtocol = new OutObject<Protocol<?>>();
				var r = Task.call(zeze.newProcedure(() -> { // 创建存储过程并且在当前线程中调用。
					var p3 = factoryHandle.Factory.create();
					var t = Transaction.getCurrent();
					var proc = t.getTopProcedure();
					//noinspection DataFlowIssue
					proc.setActionName(p3.getClass().getName());
					p3.decode(ByteBuffer.Wrap(arg.getProtocolData()));
					p3.setSender(sender);
					p3.setUserState(session);
					var isRpcResponse = !p3.isRequest(); // && p3 instanceof Rpc
					if (isRpcResponse)
						proc.setActionName(proc.getActionName() + ":Response");
					if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(p3.getTypeId())
							&& outProtocol.value == null) { // redo后不再输出日志
						var roleId = session.getRoleId();
						if (roleId == null)
							roleId = -arg.getLinkSid();
						AsyncSocket.log("Recv", roleId, arg.getOnlineSetName(), p3);
					}
					outProtocol.value = p3;
					t.runWhileCommit(() -> arg.setProtocolData(Binary.Empty)); // 这个字段不再需要读了,避免ProviderUserSession引用太久,置空
					if (isRpcResponse)
						return processRpcResponse(outRpcContext, p3);
					// protocol or rpc request
					@SuppressWarnings("unchecked")
					var handler = (ProtocolHandle<Protocol<?>>)factoryHandle.Handle;
					return handler != null ? handler.handle(p3) : Procedure.NotImplement;
				}, null, factoryHandle.Level, session), outProtocol, session::trySendResponse);
				if (PerfCounter.ENABLE_PERF) {
					PerfCounter.instance.addRecvInfo(typeId, factoryHandle.Class,
							Protocol.HEADER_SIZE + psize, System.nanoTime() - timeBegin);
				}
				return r;
			}

			p2 = factoryHandle.Factory.create();
			p2.decode(ByteBuffer.Wrap(arg.getProtocolData()));
			p2.setSender(sender);
			p2.setUserState(session);
			if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
				var roleId = session.getRoleId();
				if (roleId == null)
					roleId = -linkSid;
				AsyncSocket.log("Recv", roleId, arg.getOnlineSetName(), p2);
			}
			var isRpcResponse = !p2.isRequest(); // && p2 instanceof Rpc
			if (txn != null) { // 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
				//noinspection ConstantConditions
				txn.getTopProcedure().setActionName(p2.getClass().getName() + (isRpcResponse ? ":Response" : ""));
				txn.setUserState(session);
				txn.runWhileCommit(() -> arg.setProtocolData(Binary.Empty)); // 这个字段不再需要读了,避免ProviderUserSession引用太久,置空
			} else // 应用框架不支持事务或者协议配置了"不需要事务”
				arg.setProtocolData(Binary.Empty); // 这个字段不再需要读了,避免ProviderUserSession引用太久,置空
			var p3 = p2;
			var r = Task.call(() -> {
				if (isRpcResponse)
					return processRpcResponse(outRpcContext, p3);
				// protocol or rpc request
				@SuppressWarnings("unchecked")
				var handler = (ProtocolHandle<Protocol<?>>)factoryHandle.Handle;
				return handler != null ? handler.handle(p3) : Procedure.NotImplement;
			}, p3, session::trySendResponse);
			if (PerfCounter.ENABLE_PERF) {
				PerfCounter.instance.addRecvInfo(typeId, factoryHandle.Class,
						Protocol.HEADER_SIZE + psize, System.nanoTime() - timeBegin);
			}
			return r;
		} catch (Exception ex) {
			var desc = "ProcessDispatch(" + (p2 != null ? p2.getClass().getName() : typeId) + ") exception:";
			logger.error(desc, ex);
			sendKick(sender, linkSid, BKick.ErrorProtocolException, desc + ' ' + ex);
			return Procedure.Success;
		} finally {
			localDispatch.remove();
		}
	}

	private long processRpcResponse(@NotNull OutObject<Rpc<?, ?>> outRpcContext, @NotNull Protocol<?> p3)
			throws Exception {
		var res = (Rpc<?, ?>)p3;
		// 获取context并保存下来，redo的时候继续使用。
		if (outRpcContext.value == null) {
			outRpcContext.value = providerApp.providerService.removeRpcContext(res.getSessionId());
			// 再次检查，因为context可能丢失
			if (outRpcContext.value == null) {
				logger.warn("rpc response: lost context, maybe timeout. {}", p3);
				return Procedure.Unknown;
			}
		}

		return res.setupRpcResponseContext(outRpcContext.value).setFutureResultOrCallHandle();
	}

	@Override
	protected long ProcessAnnounceLinkInfo(@NotNull AnnounceLinkInfo protocol) {
		if (!AsyncSocket.ENABLE_PROTOCOL_LOG) {
			logger.info("AnnounceLinkInfo[{}]: {}",
					protocol.getSender().getSessionId(), AsyncSocket.toStr(protocol.Argument));
		}
		// var linkSession = (ProviderService.LinkSession)protocol.getSender().getUserState();
		return Procedure.Success;
	}
}
