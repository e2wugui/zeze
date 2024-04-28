package Zeze.Arch;

import java.net.ServerSocket;
import Zeze.Application;
import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Builtin.LinkdBase.ReportError;
import Zeze.Builtin.Provider.BDispatch;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.HandshakeServer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutLong;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinkdService extends HandshakeServer {
	private static final @NotNull Logger logger = LogManager.getLogger(LinkdService.class);
	protected LinkdApp linkdApp;
	protected long curSendSpeed; // bytes/sec

	public LinkdService(@NotNull String name, Application zeze) {
		super(name, zeze);

		if (getSocketOptions().getOverBandwidth() != null) {
			var lastSendSize = new OutLong();
			Task.scheduleUnsafe(1000, 1000, () -> {
				updateRecvSendSize();
				long sendSize = getSendSize();
				curSendSpeed = sendSize - lastSendSize.value;
				lastSendSize.value = sendSize;
			});
		}
	}

	@Override
	public void start() throws Exception {
		super.start();
	}

	private void reportError(@NotNull Dispatch dispatch) {
		// 如果是 rpc.request 直接返回Procedure.Busy错误。
		// see Zeze.Net.Rpc.decode/encode
		var bb = ByteBuffer.Wrap(dispatch.Argument.getProtocolData());
		var header = bb.ReadInt();
		AsyncSocket so;
		if ((header & FamilyClass.FamilyClassMask) == FamilyClass.Request
				&& (so = GetSocket(dispatch.Argument.getLinkSid())) != null) {
			if ((header & FamilyClass.BitResultCode) != 0)
				bb.SkipLong();
			var sessionId = bb.ReadLong();
			// argument 忽略，必须要解析出来，也不知道是什么。

			// 开始响应rpc.response.
			// 【注意】复用了上面的变量 bb，compress。
			bb = ByteBuffer.Allocate(12);
			bb.WriteInt(FamilyClass.Response | FamilyClass.BitResultCode);
			bb.WriteLong(Procedure.Busy);
			bb.WriteLong(sessionId);
			EmptyBean.instance.encode(bb); // emptyBean对应任意bean的默认值状态。
			so.Send(bb);
		}
		// 报告服务器繁忙，但不关闭连接。
		reportError(dispatch.Argument.getLinkSid(), BReportError.FromLink, BReportError.CodeProviderBusy,
				"provider is busy.", false);
	}

	public void reportError(long linkSid, int from, int code, @Nullable String desc) {
		reportError(linkSid, from, code, desc, true);
	}

	public void reportError(long linkSid, int from, int code, @Nullable String desc, boolean closeLink) {
		var link = GetSocket(linkSid);
		if (link != null) {
			new ReportError(new BReportError.Data(from, code, desc)).Send(link);

			switch (from) {
			case BReportError.FromLink:
				//noinspection SwitchStatementWithTooFewBranches
				switch (code) {
				case BReportError.CodeNoProvider:
					// 没有服务时，不断开连接，允许客户端重试。
					return;
				}
				break;

			case BReportError.FromProvider:
				break;
			}
			// 延迟关闭。等待客户端收到错误以后主动关闭，或者超时。
			// 虽然使用了写完关闭(CloseGracefully)方法，但是等待一下，尽量让客户端主动关闭，有利于减少 TCP_TIME_WAIT?
			if (closeLink) {
				Task.schedule(2000, () -> {
					var so = GetSocket(linkSid);
					if (so != null)
						so.closeGracefully();
				});
			}
		}
	}

	public @Nullable LinkdUserSession getAuthedSession(@NotNull AsyncSocket socket) {
		var linkSession = (LinkdUserSession)socket.getUserState();
		if (linkSession == null || !linkSession.isAuthed()) {
			reportError(socket.getSessionId(), BReportError.FromLink, BReportError.CodeNotAuthed, "not authed.");
			return null;
		}
		return linkSession;
	}

	// 注意这里为了优化拷贝开销,返回的Dispatch引用了参数data中的byte数组,调用者要确保Dispatch用完之前不能修改data数据,否则应该传入data.Copy()
	public static @NotNull Dispatch createDispatch(@NotNull LinkdUserSession linkSession, @NotNull AsyncSocket so,
												   int moduleId, int protocolId, @NotNull ByteBuffer data) {
		var userState = linkSession.getUserState();
		return new Dispatch(new BDispatch.Data(so.getSessionId(), linkSession.getAccount(),
				Protocol.makeTypeId(moduleId, protocolId), new Binary(data),
				userState.getContext(), userState.getContextx(), userState.getOnlineSetName()));
	}

	private boolean tryReportError(@NotNull LinkdUserSession linkSession, int moduleId, @NotNull Dispatch dispatch) {
		var pms = linkdApp.linkdProvider.getProviderModuleState(moduleId);
		if (null == pms)
			return false;
		if (pms.configType == BModule.ConfigTypeDynamic) {
			reportError(linkSession.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider,
					"no provider: " + moduleId + ", " + dispatch.getProtocolId());
			// 此后断开连接，不再继续搜索，返回true
			return true;
		}
		return false;
	}

	public boolean findSend(@NotNull LinkdUserSession linkSession, int moduleId, @NotNull Dispatch dispatch) {
		var providerSessionId = linkSession.tryGetProvider(moduleId);
		if (providerSessionId != null) {
			var socket = linkdApp.linkdProviderService.GetSocket(providerSessionId);
			if (socket == null)
				return tryReportError(linkSession, moduleId, dispatch);

			var ps = (LinkdProviderSession)socket.getUserState();
			if (ps.load.getOverload() == BLoad.eOverload) {
				// 过载时会直接拒绝请求以及报告错误。
				reportError(dispatch);
				// 但是不能继续派发了。所以这里返回true，表示处理完成。
				return true;
			}

			if (socket.Send(dispatch)) {
				ps.timeCounter.increment();
				return true;
			}

			return tryReportError(linkSession, moduleId, dispatch);
		}
		return false;
	}

	public boolean choiceBindSend(@NotNull LinkdUserSession linkSession, @NotNull AsyncSocket so, int moduleId,
								  @NotNull Dispatch dispatch) {
		var provider = new OutLong();

		var pms = linkdApp.linkdProvider.getProviderModuleState(moduleId);
		if (null != pms && pms.configType == BModule.ConfigTypeDynamic) {
			logger.warn("dynamic module do not need choice. moduleId={}, protocolType={}",
					moduleId, dispatch.Argument.getProtocolType());
			var curTime = System.currentTimeMillis();
			if (curTime - linkSession.lastReportUnbindDynamicModuleTime >= 1000) {
				linkSession.lastReportUnbindDynamicModuleTime = curTime;
				new ReportError(new BReportError.Data(BReportError.FromDynamicModule, moduleId, null)).Send(so);
			}
			return true; // skip ...
		}

		if (linkdApp.linkdProvider.choiceProviderAndBind(moduleId, linkSession.clientAppVersion, so, provider)) {
			var providerSocket = linkdApp.linkdProviderService.GetSocket(provider.value);
			if (providerSocket == null)
				return false;

			var ps = (LinkdProviderSession)providerSocket.getUserState();
			if (ps.load.getOverload() == BLoad.eOverload) {
				// 过载时会直接拒绝请求以及报告错误。
				reportError(dispatch);
				// 但是不能继续派发了。所以这里返回true，表示处理完成。
				return true;
			}

			// ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
			if (providerSocket.Send(dispatch)) {
				ps.timeCounter.increment();
				return true;
			}
			// 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
		}
		return false;
	}

	@Override
	public void dispatchUnknownProtocol(@NotNull AsyncSocket so, int moduleId, int protocolId,
										@NotNull ByteBuffer data) {
		var linkSession = getAuthedSession(so);
		if (linkSession == null)
			return;
		linkSession.keepAlive(this);
		var dispatch = createDispatch(linkSession, so, moduleId, protocolId, data);
		if (findSend(linkSession, moduleId, dispatch))
			return;
		if (choiceBindSend(linkSession, so, moduleId, dispatch))
			return;
		reportError(so.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider,
				"no provider: " + moduleId + ", " + protocolId);
	}

	@Override
	public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb, @NotNull ProtocolFactoryHandle<?> factoryHandle,
								 @Nullable AsyncSocket so) throws Exception {
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		p.dispatch(this, factoryHandle);
	}

	@Override
	public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle)
			throws Exception {
		try {
			var isRequestSaved = p.isRequest();
			var result = p.handle(this, factoryHandle); // 不启用新的Task，直接在io-thread里面执行。
			Task.logAndStatistics(null, result, p, isRequestSaved);
		} catch (Exception ex) {
			p.getSender().close(ex); // link 在异常时关闭连接。
		}
	}

	@Override
	public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc, @NotNull ProtocolHandle<P> responseHandle,
															@NotNull ProtocolFactoryHandle<?> factoryHandle) {
		// Raft RPC 的回复处理应该都不是block的,直接在IO线程处理,避免线程池堆满等待又无法唤醒导致死锁
		try {
			responseHandle.handle(rpc);
		} catch (Throwable e) { // run handle. 必须捕捉所有异常。logger.error
			logger.error("", e);
		}
	}

	@SuppressWarnings("MethodMayBeStatic")
	public @NotNull LinkdUserSession newSession(@NotNull AsyncSocket so) {
		return new LinkdUserSession(so.getSessionId());
	}

	@Override
	public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
		so.setUserState(newSession(so));
		super.OnSocketAccept(so);
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		super.OnSocketClose(so, e);
		if (so.getUserState() != null)
			((LinkdUserSession)so.getUserState()).onClose(linkdApp.linkdProviderService);
	}

	@Override
	public void onServerSocketBind(@NotNull ServerSocket ss) {
		// 需要LinkdService实现自己的查询服务器，在这里把实际绑定的地址和端口注册到名字服务器。
		try {
			if (linkdApp.onServerSocketBindAction != null)
				linkdApp.onServerSocketBindAction.run(ss);
		} catch (Exception e) {
			Task.forceThrow(e);
		}
	}

	@Override
	public boolean discard(@NotNull AsyncSocket sender, int moduleId, int protocolId, int size) throws Exception {
		/*
		【新修订：实现成忽略ProviderService的带宽过载配置，
		因为ProviderService的输入最终也会反映到LinkdService的输出。
		否则这里应该是max(LinkdService.Rate, ProviderService.Rate)】
		*/
		var opt = getSocketOptions().getOverBandwidth();
		if (opt == null)
			return false; // discard no
		var rate = (double)(curSendSpeed) / opt;

		// 总控
		if (rate > getSocketOptions().getOverBandwidthFusingRate()) // 1.0
			return true; // 熔断: discard all，其他级别在回调中处理。
		if (rate < getSocketOptions().getOverBandwidthNormalRate()) // 0.7
			return false; // 整体负载小于0.7,全部不丢弃

		/*
		对于游戏可以针对【Move协议】使用下面的策略.
		if (moduleId == Map.ModuleId && protocolId == Map.Move.ProtocolId)
			return Zeze.Util.Random.getInstance().nextInt(100) < (int)((rate - 0.7) / (1.0 - 0.7) * 100);
		return false; // 其他协议全部不丢弃，除非达到熔断。
		*/
		if (linkdApp.discardAction != null)
			return linkdApp.discardAction.call(sender, moduleId, protocolId, size, rate);

		// 应用没有定制丢弃策略，那么熔断前都不丢弃。
		return false;
	}
}
