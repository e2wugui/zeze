package Zeze.Arch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Builtin.LinkdBase.ReportError;
import Zeze.Builtin.Provider.AnnounceProviderInfo;
import Zeze.Builtin.Provider.BBind;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.Provider.Broadcast;
import Zeze.Builtin.Provider.CheckLinkSession;
import Zeze.Builtin.Provider.Kick;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Builtin.Provider.Subscribe;
import Zeze.Builtin.Provider.UnBind;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.LoginQueueServer;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutLong;
import Zeze.Util.Str;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Linkd上处理Provider协议的模块。
 */
public class LinkdProvider extends AbstractLinkdProvider {
	private static final @NotNull Logger logger = LogManager.getLogger(LinkdProvider.class);
	protected static final @Nullable String dumpFilename = System.getProperty("dumpLinkdOutput");
	protected static final boolean enableDump = dumpFilename != null;
	protected static final IOException sendException = new IOException("LinkdProvider send failed");

	protected LinkdApp linkdApp;
	protected ProviderDistributeVersion distributes;
	//private int firstModuleWithConfigTypeDefault;

	// 用于客户端选择Provider，只支持一种Provider。如果要支持多种，需要客户端增加参数，这个不考虑了。
	// 内部的ModuleRedirect ModuleRedirectAll Transmit都携带了ServiceNamePrefix参数，所以，
	// 内部的Provider可以支持完全不同的solution，不过这个仅仅保留给未来扩展用，
	// 不建议在一个项目里面使用多个Prefix。
	// FND2-A1-4：写者ProcessAnnounceProviderInfo（provider连接EL），读者makeServiceName→choice路由
	// （客户端连接EL），两套EventLoop无同步边；无volatile时客户端EL长期读到""，查不到订阅状态，
	// 假性"无provider"路由失败。对照ProviderSession跨线程字段全volatile。
	private volatile @NotNull String serverServiceNamePrefix = "";

	protected @Nullable FileOutputStream dumpFile;
	protected @Nullable AsyncSocket dumpSocket;
	// 私有锁: 避免暴露this监视器,Service/Module实例被外部广泛共享;锁内做文件IO,ReentrantLock避免虚拟线程持锁阻塞时pin载体(JDK21-23)
	private final ReentrantLock dumpLock = new ReentrantLock();
	private boolean dumpClosed;
	private final ConcurrentHashMap<Integer, AsyncSocket> serverId2ProviderSocket = new ConcurrentHashMap<>();

	public ProviderDistributeVersion getDistributes() {
		return distributes;
	}

	public @NotNull String getServerServiceNamePrefix() {
		return serverServiceNamePrefix;
	}

	/**
	 * @return 错误码. 0表示成功; [1,999]表示错误
	 */
	public int choiceProvider(@NotNull AsyncSocket link, int moduleId, @NotNull Predicate<AsyncSocket> onSend) {
		var userSession = (LinkdUserSession)link.getUserState();
		var providerSessionId = userSession.tryGetProvider(moduleId);
		if (providerSessionId != null) {
			var socket = linkdApp.linkdProviderService.GetSocket(providerSessionId);
			if (socket != null && onSend.test(socket))
				return 0; // done
			// 原来绑定的provider找不到连接，尝试继续从静态绑定里面查找。
			// 此时应该处于 UnBind 过程中。
		}

		var provider = new OutLong();
		int r = choiceProviderAndBind(moduleId, userSession.clientAppVersion, link, provider);
		if (r == 0) {
			var providerSocket = linkdApp.linkdProviderService.GetSocket(provider.value);
			if (providerSocket != null && onSend.test(providerSocket)) // ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
				return 0;
			// else
			// 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
		}
		return r;
	}

	public ConcurrentHashMap<Integer, AsyncSocket> getServerId2ProviderSocket() {
		return serverId2ProviderSocket;
	}

	/**
	 * @return 错误码. 0表示成功; [1,9]表示错误
	 */
	public int choiceProvider(@NotNull AsyncSocket link, Binary tokenBin) throws Exception {
		var token = LoginQueueServer.decodeToken(linkdApp.getLinkdLoad().getLoginQueueAgent().getSecret(), tokenBin);
		if (token.getExpireTime() < System.currentTimeMillis())
			return 1;
		if (token.getLinkServerId() != linkdApp.zeze.getConfig().getServerId())
			return 2;

		if (token.getServerId() < 0)
			return 0; // 用户link-gs强绑等自定义选择模式下，不需要后面的选择和绑定。

		// token.getSerialId() 用于严格重放。
		// 根据provider.getServerId()查找provider，并且bind所有静态模块到session。
		var providerSocket = serverId2ProviderSocket.get(token.getServerId());
		if (null == providerSocket)
			return 3;
		var providerSession = (LinkdProviderSession)providerSocket.getUserState();
		var linkSession = (LinkdUserSession)link.getUserState();
		var staticBinds = providerSession.getStaticBinds();
		linkSession.bind(linkdApp.linkdProviderService, link, staticBinds.keySet(), providerSocket);
		logger.info("static bind: account={}, moduleIds.size={}, provider={}",
				linkSession.account, staticBinds.size(), providerSocket.getRemoteAddress());
		return 0;
	}

	public boolean choiceHashWithoutBind(int moduleId, long clientVersion, int hash, @NotNull OutLong provider) {
		provider.value = 0L;
		var distribute = distributes.selectDistribute(clientVersion >>> 48);
		if (distribute == null)
			return false;
		var serviceName = ProviderDistribute.makeServiceName(serverServiceNamePrefix, moduleId);
		//noinspection DataFlowIssue
		var providers = distribute.zeze.getServiceManager().getSubscribeStates().get(serviceName);
		return providers != null && distribute.choiceHash(providers, hash, provider);
	}

	public @NotNull String makeServiceName(int moduleId) {
		return ProviderDistribute.makeServiceName(serverServiceNamePrefix, moduleId);
	}

	public @Nullable ProviderModuleState getProviderModuleState(int moduleId) {
		//noinspection DataFlowIssue
		var providers = distributes.zeze.getServiceManager().getSubscribeStates().get(makeServiceName(moduleId));
		return providers != null ? (ProviderModuleState)providers.getSubscribeInfo().getLocalState() : null;
	}

	/**
	 * @return 错误码. 0表示成功; [1,99]表示错误
	 */
	public int choiceProviderAndBind(int moduleId, long clientVersion, @NotNull AsyncSocket link,
									 @NotNull OutLong provider) {
		provider.value = 0L;
		var distribute = distributes.selectDistribute(clientVersion >>> 48);
		if (distribute == null)
			return 1;
		//noinspection DataFlowIssue
		var providers = distribute.zeze.getServiceManager().getSubscribeStates().get(makeServiceName(moduleId));
		if (providers == null)
			return 2;
		var linkSession = (LinkdUserSession)link.getUserState();

		// 这里保存的 ProviderModuleState 是该moduleId的第一个bind请求去订阅时记录下来的，
		// 这里仅使用里面的ChoiceType和ConfigType。这两个参数对于相同的moduleId都是一样的。
		// 如果需要某个provider.SessionId，需要查询 ServiceInfoListSortedByIdentity 里的ServiceInfo.LocalState。
		var providerModuleState = (ProviderModuleState)providers.getSubscribeInfo().getLocalState();
		assert providerModuleState != null;
		switch (providerModuleState.choiceType) {
		case BModule.ChoiceTypeHashAccount:
			if (!distribute.choiceHash(providers, Bean.hash32(linkSession.getAccount()), provider))
				return 11;
			break; // bind static later

		case BModule.ChoiceTypeHashRoleId:
			var roleId = linkSession.getRoleId();
			if (roleId == null || !distribute.choiceHash(providers, ByteBuffer.calc_hashnr(roleId), provider))
				return 21;
			break; // bind static later

		case BModule.ChoiceTypeFeedFullOneByOne:
			if (!distribute.choiceFeedFullOneByOne(providers, provider))
				return 31;
			break; // bind static later

		case BModule.ChoiceTypeHashSourceAddress:
			var remoteAddress = link.getRemoteAddress();
			if (null == remoteAddress)
				return 41;
			if (!distribute.choiceHash(providers, remoteAddress.hashCode(), provider))
				return 42;
			break; // bind static later

		case BModule.ChoiceTypeLoad:
			if (!distribute.choiceLoad(providers, provider))
				return 51;
			break; // bind static later

		case BModule.ChoiceTypeRequest:
			// fall down
		default:
			if (!distribute.choiceRequest(providers, provider))
				return 61;
			break; // bind static later
		}

		// 这里不判断null，如果失败让这次选择失败，否则选中了，又没有Bind以后更不好处理。
		var providerSocket = linkdApp.linkdProviderService.GetSocket(provider.value);
		ProviderSession ps;
		if (providerSocket == null
				|| providerSocket.isClosed()
				|| (ps = (ProviderSession)providerSocket.getUserState()).isDisableChoice()
				|| !ProviderDistribute.checkAppVersion(ps.appVersion, clientVersion)) {
			// 版本不匹配，继续尝试查找。
			providerSocket = null; // clear first.

			providers.lock();
			try {
				for (int i = 0, n = providers.getLocalStates().size(); i < n; i++) {
					var e = providers.getNextStateEntry();
					if (e == null)
						return 71;
					var sessionId = ((ProviderModuleState)e.getValue()).sessionId;
					providerSocket = linkdApp.linkdProviderService.GetSocket(sessionId);
					if (providerSocket == null || providerSocket.isClosed()) {
						providerSocket = null;
						continue; // 这种查找在socket没有时继续尝试。
					}

					ps = (ProviderSession)providerSocket.getUserState();
					if (!ps.isDisableChoice()
							&& ProviderDistribute.checkAppVersion(ps.appVersion, clientVersion)) {
						provider.value = sessionId;
						break;
					}
					providerSocket = null; // BUG，否则如果刚好是最后一个，跳出循环后面的条件就成立了。
				}
			} finally {
				providers.unlock();
			}
			if (providerSocket == null) // 这个条件，见上BUG。
				return 72;
		}

		// 动态模块允许使用这个方法查找provider，
		// 但是不会主动注册到linkUserSession，每次都需要重新查找。
		// 动态模块需要主动bind/unbind。
		// XXX
		if (!providerModuleState.dynamic) {
			var staticBinds = ((LinkdProviderSession)providerSocket.getUserState()).getStaticBinds();
			linkSession.bind(linkdApp.linkdProviderService, link, staticBinds.keySet(), providerSocket);
			logger.info("static bind: account={}, moduleIds.size={}, provider={}, configType={}, choiceType={}," +
							" clientVersion={}",
					linkSession.account, staticBinds.size(), providerSocket.getRemoteAddress(),
					false, providerModuleState.choiceType, Str.toVersionStr(clientVersion));
		}/* else if (providerModuleState.dynamic == BModule.ConfigTypeSpecial) {
			// special 不跟随大部队，单独bind。
			linkSession.bind(linkdApp.linkdProviderService, link, List.of(moduleId), providerSocket);
			logger.info("special bind: account={}, moduleId={}, provider={}, configType={}, choiceType={}," +
							" clientVersion={}",
					linkSession.account, moduleId, providerSocket.getRemoteAddress(),
					providerModuleState.dynamic, providerModuleState.choiceType, Str.toVersionStr(clientVersion));
		}
		*/
		return 0;
	}

	public void onProviderClose(@NotNull AsyncSocket provider) {
		var providerSession = (LinkdProviderSession)provider.getUserState();
		if (providerSession == null)
			return;

		// 条件删除：迟到的关闭事件不得误删同serverId的新会话注册
		// （provider侧先发现半开连接并重连，新连接握手announce已put，旧连接的关闭回调后到）。
		serverId2ProviderSocket.remove(providerSession.serverId, provider);
		// providerSessions此前没有删除点：直连ip/port改配或provider缩容时条目会永久残留；同样按所有权条件删除。
		linkdApp.linkdProviderService.providerSessions.remove(providerSession.getServerLoadName(), providerSession);

		// unbind module
		// 与ProcessBindRequest/ProcessSubscribeRequest的写入互斥（同一把monitor），
		// 保证"bind写入"与"close清理"全序：要么bind先完成（这里能清到），
		// 要么close先完成（bind在锁内发现isClosed跳过写入）。
		synchronized (providerSession) {
			unBindModules(provider, providerSession.getStaticBinds().keySet(), true);
			providerSession.getStaticBinds().clear();
			// 动态模块（Subscribe注册的）同样清理：unBindModules内按sessionId条件移除，
			// 重连的新会话已重新Subscribe覆盖时不会误删。
			unBindModules(provider, providerSession.getDynamicSubscribes().keySet(), true);
			providerSession.getDynamicSubscribes().clear();
		}

		// unbind LinkSession
		// unbind会获取LinkdUserSession.bindsLock，与bind路径（bindsLock->linkSessionIdsLock）锁序相反，
		// 持有linkSessionIdsLock调用会AB-BA死锁。同LinkdUserSession.onClose：锁内只换出快照，锁外执行unbind与发送。
		var linkSessionIds = providerSession.swapLinkSessionIds();
		for (var it = linkSessionIds.iterator(); it.moveToNext(); ) {
			int moduleId = it.key();
			var p = moduleId == Online.ModuleId || moduleId == Zeze.Game.Online.ModuleId
					? new ReportError(new BReportError.Data(BReportError.FromLink, BReportError.CodeProviderBroken,
					null)) : null;
			for (var it2 = it.value().iterator(); it2.moveToNext(); ) {
				var link = linkdApp.linkdService.GetSocket(it2.value());
				if (link != null) {
					var linkSession = (LinkdUserSession)link.getUserState();
					if (linkSession != null) {
						linkSession.unbind(linkdApp.linkdProviderService, link, moduleId, provider, true);
						if (p != null)
							p.Send(link);
					}
				}
			}
		}
	}

	@Override
	public long ProcessBindRequest(@NotNull Bind rpc) {
		var bind = rpc.Argument;
		if (bind.getLinkSids().isEmpty()) {
			var providerSession = (LinkdProviderSession)rpc.getSender().getUserState();
			var providerInfo = providerSession.getInfo();
			for (var e : bind.getModules().entrySet()) {
				var moduleId = e.getKey();
				var module = e.getValue();
				/*
				if (firstModuleWithConfigTypeDefault == 0 && module.getConfigType() == BModule.ConfigTypeDefault) {
					//noinspection DataFlowIssue,ConstantValue
					firstModuleWithConfigTypeDefault = module.getConfigType();
				}
				*/
				var providerModuleState = new ProviderModuleState(providerSession.getSessionId(),
						moduleId, module.getChoiceType(), module.isDynamic());
				var serviceName = ProviderDistribute.makeServiceName(providerInfo.getServiceNamePrefix(), moduleId);
				//noinspection DataFlowIssue
				var subState = distributes.zeze.getServiceManager().subscribeService(
						new BSubscribeInfo(serviceName, 0, providerModuleState));
				// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
				// 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
				// 与onProviderClose的清理互斥：close先置closed标志再回调OnSocketClose（见TcpSocket.close），
				// 锁内检查isClosed得到全序——close已完成则跳过写入（否则写入的死sessionId状态无人清理），
				// close未发生则本次写入必被随后的onProviderClose清理（staticBinds已登记）。
				// subscribeService的阻塞等待必须留在锁外。
				synchronized (providerSession) {
					if (rpc.getSender().isClosed())
						break;
					subState.setIdentityLocalState(providerInfo.getServiceIdentity(), providerModuleState);
					providerSession.getStaticBinds().add(moduleId);
				}
			}
		} else {
			// 动态绑定
			for (var linkSid : bind.getLinkSids()) {
				var link = linkdApp.linkdService.GetSocket(linkSid);
				if (link != null) {
					var linkSession = (LinkdUserSession)link.getUserState();
					linkSession.bind(linkdApp.linkdProviderService, link, bind.getModules().keySet(), rpc.getSender());
					logger.info("dynamic bind: account={}, moduleIds={}, provider={}", linkSession.account,
							bind.getModules().keySet(), rpc.getSender().getRemoteAddress());
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSubscribeRequest(@NotNull Subscribe rpc) {
		var providerSession = (LinkdProviderSession)rpc.getSender().getUserState();
		var providerInfo = providerSession.getInfo();
		for (var e : rpc.Argument.getModules().entrySet()) {
			var moduleId = e.getKey();
			var module = e.getValue();
			var providerModuleState = new ProviderModuleState(providerSession.getSessionId(),
					moduleId, module.getChoiceType(), module.isDynamic());
			var serviceName = ProviderDistribute.makeServiceName(providerInfo.getServiceNamePrefix(), moduleId);
			//noinspection DataFlowIssue
			var subState = distributes.zeze.getServiceManager().subscribeService(
					new BSubscribeInfo(serviceName, 0, providerModuleState));
			// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
			// 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
			// 与ProcessBindRequest同理：防止subscribeService等待期间连接关闭后写入死sessionId状态。
			synchronized (providerSession) {
				if (rpc.getSender().isClosed())
					break;
				subState.setIdentityLocalState(providerInfo.getServiceIdentity(), providerModuleState);
				providerSession.getDynamicSubscribes().add(moduleId);
			}
		}

		rpc.SendResult();
		return Procedure.Success;
	}

	private void unBindModules(@NotNull AsyncSocket provider, @NotNull Iterable<Integer> modules) {
		unBindModules(provider, modules, false);
	}

	private void unBindModules(@NotNull AsyncSocket provider, @NotNull Iterable<Integer> modules,
							   boolean isOnProviderClose) {
		var providerSession = (LinkdProviderSession)provider.getUserState();
		var providerInfo = providerSession.getInfo();
		for (var moduleId : modules) {
			if (!isOnProviderClose)
				providerSession.getStaticBinds().remove(moduleId);
			var serviceName = ProviderDistribute.makeServiceName(providerInfo.getServiceNamePrefix(), moduleId);
			//noinspection DataFlowIssue
			var volatileProviders = distributes.zeze.getServiceManager().getSubscribeStates().get(serviceName);
			if (volatileProviders != null) {
				// UnBind 不删除provider-list，这个总是通过ServiceManager通告更新。
				// 这里仅仅设置该moduleId对应的服务的状态不可用。
				// 条件清理：identity（serverId）由重连前后两代连接共享，只有localState仍属于
				// 当前这条连接（bind时记录的sessionId）时才移除；新会话已重新Bind覆盖时不得误删。
				volatileProviders.getLocalStates().computeIfPresent(providerInfo.getServiceIdentity(), (k, v) ->
						v instanceof ProviderModuleState pms && pms.sessionId == provider.getSessionId() ? null : v);
			}
		}
	}

	@Override
	protected long ProcessUnBindRequest(@NotNull UnBind rpc) {
		if (rpc.Argument.getLinkSids().isEmpty())
			unBindModules(rpc.getSender(), rpc.Argument.getModules().keySet());
		else {
			// 动态绑定
			for (var linkSid : rpc.Argument.getLinkSids()) {
				var link = linkdApp.linkdService.GetSocket(linkSid);
				if (link != null) {
					((LinkdUserSession)link.getUserState()).unbind(linkdApp.linkdProviderService,
							link, rpc.Argument.getModules().keySet(), rpc.getSender());
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Procedure.Success;
	}

	// dumpLock: 懒初始化存在check-then-act竞争,且多IO线程并发写需要串行化(仅调试属性开启时生效)
	protected void tryDump(@NotNull AsyncSocket s, @NotNull Binary pdata) throws IOException {
		dumpLock.lock();
		try {
			if (dumpClosed)
				return;
			if (dumpFile == null) {
				assert dumpFilename != null;
				dumpFile = new FileOutputStream(dumpFilename);
				dumpSocket = s;
			}
			if (dumpSocket == s)
				dumpFile.write(pdata.bytesUnsafe(), pdata.getOffset(), pdata.size());
		} finally {
			dumpLock.unlock();
		}
	}

	@Override
	public void UnRegister() {
		// dumpClosed: 关闭后不再重建,重建的FileOutputStream会截断已dump的文件
		dumpLock.lock();
		try {
			dumpClosed = true;
			dumpSocket = null;
			if (dumpFile != null) {
				try {
					dumpFile.close();
				} catch (IOException e) {
					logger.error("LinkdProvider close dumpFile failed", e);
				}
				dumpFile = null;
			}
		} finally {
			dumpLock.unlock();
		}
	}

	private static final boolean canLogSend = AsyncSocket.ENABLE_PROTOCOL_LOG
			&& AsyncSocket.canLogProtocol(Send.TypeId_);
//	private final TaskOneByOneByKey oneByOneSender = new TaskOneByOneByKey();

	@Override
	protected long ProcessSendRequest(@NotNull Send r) throws Exception {
		var pdata = r.Argument.getProtocolWholeData();
		var linkSids = r.Argument.getLinkSids();
		int sidCount = linkSids.size();
		if (canLogSend) {
			String sidStr;
			if (sidCount == 1)
				sidStr = String.valueOf(linkSids.get(0));
			else if (sidCount <= 10) {
				var sb = new StringBuilder();
				for (int i = 0; i < sidCount; i++)
					sb.append(linkSids.get(i)).append(',');
				if (sidCount > 0)
					sb.setLength(sb.length() - 1);
				sidStr = sb.toString();
			} else
				sidStr = "[" + sidCount + ']';
			var bb = ByteBuffer.Wrap(pdata);
			bb.ReadIndex += Protocol.HEADER_SIZE;
			AsyncSocket.log("Send", sidStr, r.Argument.getProtocolType(), bb);
		}
		//*
		for (int i = 0; i < sidCount; i++) {
			var linkSid = linkSids.get(i);
			var socket = linkdApp.linkdService.GetSocket(linkSid);
			// ProtocolId现在是hash值，显示出来也不好看，以后加配置换成名字。
			if (socket != null && !socket.isClosed()) {
				// 探测协议不需要转发给客户端。
				if (CheckLinkSession.TypeId_ != r.Argument.getProtocolType()) {
					if (!socket.Send(pdata))
						socket.close(sendException);
					if (enableDump)
						tryDump(socket, pdata);
				}
			} else
				r.Result.getErrorLinkSids().add(linkSid);
		}
		r.SendResult();
		/*/
		oneByOneSender.executeBatch(linkSids, (linkSid) -> {
			var link = linkdApp.linkdService.GetSocket(linkSid);
			// ProtocolId现在是hash值，显示出来也不好看，以后加配置换成名字。
			if (link != null) {
				if (!link.Send(pdata))
					link.close();
				if (enableDump)
					tryDump(link, pdata);
			} else {
				synchronized (r) {
					r.Result.getErrorLinkSids().add(linkSid);
				}
			}
		}, r::SendResult, DispatchMode.Normal);
		// */
		return Procedure.Success;
	}

	private static final boolean canLogBroadcast = AsyncSocket.ENABLE_PROTOCOL_LOG
			&& AsyncSocket.canLogProtocol(Broadcast.TypeId_);

	@Override
	protected long ProcessBroadcast(@NotNull Broadcast protocol) throws Exception {
		var pdata = protocol.Argument.getProtocolWholeData();
		if (canLogBroadcast) {
			var bb = ByteBuffer.Wrap(pdata);
			bb.ReadIndex += Protocol.HEADER_SIZE;
			AsyncSocket.log("Broc", linkdApp.linkdService.getSocketCount(), protocol.Argument.getProtocolType(), bb);
		}
		var providerVersion = protocol.Argument.isOnlySameVersion()
				? ((ProviderSession)protocol.getSender().getUserState()).appVersion
				: 0L;
		linkdApp.linkdService.foreach(socket -> {
			// auth 通过就允许发送广播。
			// 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
			// 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
			var linkSession = (LinkdUserSession)socket.getUserState();
			if (linkSession != null && linkSession.isAuthed() && !linkSession.getUserState().getContext().isEmpty() &&
					(providerVersion == 0 ||
							ProviderDistribute.checkAppVersion(providerVersion, linkSession.getClientAppVersion()))) {
				socket.Send(pdata);
				if (enableDump)
					tryDump(socket, pdata);
			}
		});
		return Procedure.Success;
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	protected long ProcessCheckLinkSession(@NotNull CheckLinkSession p) throws Exception {
		// see ProcessSend，这里不需要处理。
		return 0;
	}

	@Override
	protected long ProcessKick(@NotNull Kick protocol) {
		linkdApp.linkdService.reportError(
				protocol.Argument.getLinksid(),
				BReportError.FromProvider,
				protocol.Argument.getCode(),
				protocol.Argument.getDesc(),
				protocol.Argument.getControl() == BKick.eControlClose);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSetUserState(@NotNull SetUserState protocol) {
		var socket = linkdApp.linkdService.GetSocket(protocol.Argument.getLinkSid());
		if (socket != null) {
			var linkSession = (LinkdUserSession)socket.getUserState();
			if (linkSession != null) {
				linkSession.setUserState(protocol.Argument.getUserState());
				return Procedure.Success;
			}
		}
		return Procedure.Unknown;
	}

	@Override
	protected long ProcessAnnounceProviderInfo(@NotNull AnnounceProviderInfo protocol) {
		var arg = protocol.Argument;
		var sender = protocol.getSender();
		if (!AsyncSocket.ENABLE_PROTOCOL_LOG) {
			logger.info("AnnounceProviderInfo[{}]: name={}, id={}, ip={}, port={}, ver={}, disableChoice={}",
					sender.getSessionId(),
					arg.getServiceNamePrefix(), arg.getServiceIdentity(), arg.getProviderDirectIp(),
					arg.getProviderDirectPort(), Str.toVersionStr(arg.getAppVersion()), arg.isDisableChoice());
		}

		var session = (LinkdProviderSession)sender.getUserState();
		session.setInfo(arg); // 全部记住

		// 下面再记录一份到其他需要的地方。这里有冗余。
		serverServiceNamePrefix = arg.getServiceNamePrefix();
		session.serverId = Integer.parseInt(arg.getServiceIdentity());
		session.serverLoadIp = arg.getProviderDirectIp();
		session.serverLoadPort = arg.getProviderDirectPort();
		session.appVersion = arg.getAppVersion();
		session.disableChoice = arg.isDisableChoice();
		linkdApp.linkdProviderService.providerSessions.put(session.getServerLoadName(), session);

		// 接管：同serverId只允许一条活跃连接，前任在宣布时终结，不依赖死亡检测
		// （linkd对接受的连接不发keepalive，半开前任的检测滞后无上界）。
		// 先put两张表再踢，读侧无空窗；被踢连接的onProviderClose按所有权条件清理，不会误删现任注册。
		// 异步踢：close会在调用线程同步走完OnSocketClose→onProviderClose全链路
		// （旧会话全部客户端的unbind与ReportError广播），不能在announce所在的io-thread上就地执行。
		var old = serverId2ProviderSocket.put(session.serverId, sender);
		if (old != null && old != sender)
			TaskSpec.ofAction(() -> old.close(new IOException(
					"superseded by new announce connection, serverId=" + session.serverId))).runNow();
		return Procedure.Success;
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	protected long ProcessSetDisableChoiceRequest(@NotNull Zeze.Builtin.Provider.SetDisableChoice r) throws Exception {
		var session = (LinkdProviderSession)r.getSender().getUserState();
		session.setDisableChoice(r.Argument.isDisableChoice());
		r.SendResultCode(0);
		return 0;
	}
}
