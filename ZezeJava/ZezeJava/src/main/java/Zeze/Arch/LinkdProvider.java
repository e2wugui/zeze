package Zeze.Arch;

import java.util.Objects;

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
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Linkd上处理Provider协议的模块。
 */
public class LinkdProvider extends AbstractLinkdProvider {
	private static final Logger logger = LogManager.getLogger(LinkdProvider.class);
	protected static final @Nullable String dumpFilename = System.getProperty("dumpLinkdOutput");
	protected static final boolean enableDump = dumpFilename != null;
	protected static final IOException sendException = new IOException("LinkdProvider send failed");

	protected @Nullable LinkdApp linkdApp;

	protected final LinkdApp requireLinkdApp() {
		return Objects.requireNonNull(linkdApp, "linkd service is not attached to an application");
	}
	protected @Nullable ProviderDistributeVersion distributes;

	// 用于客户端选择Provider，只支持一种Provider。如果要支持多种，需要客户端增加参数。
	// 内部的ModuleRedirect ModuleRedirectAll Transmit都携带了ServiceNamePrefix参数，所以，
	// 内部的Provider可以支持完全不同的solution，不过这个仅仅保留给未来扩展用，
	// 不建议在一个项目里面使用多个Prefix。
	private volatile String serverServiceNamePrefix = "";

	protected @Nullable FileOutputStream dumpFile;
	protected @Nullable AsyncSocket dumpSocket;
	// 保护 dumpFile 懒初始化与并发写，使用 ReentrantLock 避免锁内文件 IO 阻塞虚拟线程载体。
	private final ReentrantLock dumpLock = new ReentrantLock();
	private boolean dumpClosed;
	private final ConcurrentHashMap<Integer, AsyncSocket> serverId2ProviderSocket = new ConcurrentHashMap<>();

	public @Nullable ProviderDistributeVersion getDistributes() {
		return distributes;
	}

	public String getServerServiceNamePrefix() {
		return serverServiceNamePrefix;
	}

	/**
	 * @return 错误码. 0表示成功; [1,999]表示错误
	 */
	public int choiceProvider(@Nullable AsyncSocket link, int moduleId, Predicate<AsyncSocket> onSend) {
		//noinspection DataFlowIssue
		var userSession = (LinkdUserSession)link.getUserState();
		//noinspection DataFlowIssue
		var providerSessionId = userSession.tryGetProvider(moduleId);
		if (providerSessionId != null) {
			var socket = requireLinkdApp().linkdProviderService.GetSocket(providerSessionId);
			if (socket != null && onSend.test(socket))
				return 0; // done
			// 原来绑定的provider找不到连接，尝试继续从静态绑定里面查找。
			// 此时应该处于 UnBind 过程中。
		}

		var provider = new OutLong();
		int r = choiceProviderAndBind(moduleId, userSession.clientAppVersion, link, provider);
		if (r == 0) {
			var providerSocket = requireLinkdApp().linkdProviderService.GetSocket(provider.value);
			if (providerSocket != null && onSend.test(providerSocket)) // ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
				return 0;
			// 找到 provider 但发送前连接已关闭，视作未找到返回错误码。
			return 102;
		}
		return r;
	}

	public ConcurrentHashMap<Integer, AsyncSocket> getServerId2ProviderSocket() {
		return serverId2ProviderSocket;
	}

	/**
	 * @return 错误码. 0表示成功; [1,9]表示错误
	 */
	public int choiceProvider(@Nullable AsyncSocket link, Binary tokenBin) throws Exception {
		var load = requireLinkdApp().getLinkdLoad();
		var loginQueueAgent = load != null ? load.getLoginQueueAgent() : null;
		if (loginQueueAgent == null) {
			logger.error("choiceProvider: LoginQueueAgent not configured"
					+ " (linkd config missing 'LoginQueueAgent' service node).");
			return 4;
		}
		var secret = loginQueueAgent.getSecret();
		if (secret == null) {
			logger.error("choiceProvider: LoginQueueAgent secret not ready (not connected to LoginQueueServer)");
			return 5;
		}
		var token = LoginQueueServer.decodeToken(secret, tokenBin);
		if (token.getExpireTime() < System.currentTimeMillis())
			return 1;
		if (token.getLinkServerId() != requireLinkdApp().zeze.getConfig().getServerId())
			return 2;

		if (token.getServerId() < 0)
			return 0; // 用户link-gs强绑等自定义选择模式下，不需要后面的选择和绑定。

		// token.getSerialId() 用于严格重放。
		// 根据provider.getServerId()查找provider，并且bind所有静态模块到session。
		var providerSocket = serverId2ProviderSocket.get(token.getServerId());
		if (null == providerSocket)
			return 3;
		var providerSession = (LinkdProviderSession)providerSocket.getUserState();
		//noinspection DataFlowIssue
		var linkSession = (LinkdUserSession)link.getUserState();
		//noinspection DataFlowIssue
		var staticBinds = providerSession.getStaticBinds();
		Objects.requireNonNull(linkSession).bind(requireLinkdApp().linkdProviderService, link, staticBinds.keySet(), providerSocket);
		logger.info("static bind: account={}, moduleIds.size={}, provider={}",
				linkSession.account, staticBinds.size(), providerSocket.getRemoteAddress());
		return 0;
	}

	public boolean choiceHashWithoutBind(int moduleId, long clientVersion, int hash, OutLong provider) {
		provider.value = 0L;
		//noinspection DataFlowIssue
		var distribute = distributes.selectDistribute(clientVersion >>> 48);
		if (distribute == null)
			return false;
		var serviceName = ProviderDistribute.makeServiceName(serverServiceNamePrefix, moduleId);
		//noinspection DataFlowIssue
		var providers = distribute.zeze.getServiceManager().getSubscribeStates().get(serviceName);
		return providers != null && distribute.choiceHash(providers, hash, provider);
	}

	public String makeServiceName(int moduleId) {
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
	public int choiceProviderAndBind(int moduleId, long clientVersion, AsyncSocket link,
									 OutLong provider) {
		provider.value = 0L;
		//noinspection DataFlowIssue
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
			//noinspection DataFlowIssue
			if (!distribute.choiceHash(providers, Bean.hash32(Objects.requireNonNull(linkSession.getAccount())), provider))
				return 11;
			break; // bind static later

		case BModule.ChoiceTypeHashRoleId:
			//noinspection DataFlowIssue
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
		var providerSocket = requireLinkdApp().linkdProviderService.GetSocket(provider.value);
		ProviderSession ps;
		//noinspection DataFlowIssue
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
					providerSocket = requireLinkdApp().linkdProviderService.GetSocket(sessionId);
					if (providerSocket == null || providerSocket.isClosed()) {
						providerSocket = null;
						continue; // 这种查找在socket没有时继续尝试。
					}

					ps = (ProviderSession)providerSocket.getUserState();
					//noinspection DataFlowIssue
					if (!ps.isDisableChoice()
							&& ProviderDistribute.checkAppVersion(ps.appVersion, clientVersion)) {
						provider.value = sessionId;
						break;
					}
					providerSocket = null; // 置空，否则如果刚好是最后一个，跳出循环后面的条件就成立了。
				}
			} finally {
				providers.unlock();
			}
			if (providerSocket == null) // 循环内未找到可用provider。
				return 72;
		}

		// 动态模块允许使用这个方法查找provider，
		// 但是不会主动注册到linkUserSession，每次都需要重新查找。
		// 动态模块需要主动bind/unbind。
		if (!providerModuleState.dynamic) {
			var staticBinds = ((LinkdProviderSession)providerSocket.getUserState()).getStaticBinds();
			Objects.requireNonNull(linkSession).bind(requireLinkdApp().linkdProviderService, link, staticBinds.keySet(), providerSocket);
			logger.info("static bind: account={}, moduleIds.size={}, provider={}, configType={}, choiceType={}," +
							" clientVersion={}",
					linkSession.account, staticBinds.size(), providerSocket.getRemoteAddress(),
					false, providerModuleState.choiceType, Str.toVersionStr(clientVersion));
		}
		return 0;
	}

	public void onProviderClose(AsyncSocket provider) {
		var providerSession = (LinkdProviderSession)provider.getUserState();
		if (providerSession == null)
			return;

		// 取消 TimeCounter 周期任务，避免会话更替泄漏。
		providerSession.timeCounter.close();

		// 与 announce/bind/subscribe 互斥，保证路由写入与关闭清理全序。
		synchronized (providerSession.moduleBindsMonitor) {
			// 条件删除，避免迟到的关闭事件误删同 serverId 的新连接注册。
			serverId2ProviderSocket.remove(providerSession.serverId, provider);
			requireLinkdApp().linkdProviderService.providerSessions.remove(providerSession.getServerLoadName(), providerSession);
			unBindModules(provider, providerSession.getStaticBinds().keySet(), true);
			providerSession.getStaticBinds().clear();
			// 按 sessionId 条件清理动态订阅模块。
			unBindModules(provider, providerSession.getDynamicSubscribes().keySet(), true);
			providerSession.getDynamicSubscribes().clear();
		}

		// 锁内换出快照、锁外 unbind，避免与 LinkdUserSession.bindsLock 反向加锁死锁。
		var linkSessionIds = providerSession.swapLinkSessionIds();
		for (var it = linkSessionIds.iterator(); it.moveToNext(); ) {
			int moduleId = it.key();
			var p = moduleId == Online.ModuleId || moduleId == Zeze.Game.Online.ModuleId
					? new ReportError(new BReportError.Data(BReportError.FromLink, BReportError.CodeProviderBroken,
					null)) : null;
			for (
				var it2 = it.value().iterator(); it2.moveToNext(); ) {
				var link = requireLinkdApp().linkdService.GetSocket(it2.value());
				if (link != null)
					unbindClosedProvider(provider, link, moduleId, p);
			}
		}
	}

	private void unbindClosedProvider(AsyncSocket provider, AsyncSocket link,
			int moduleId, @Nullable ReportError report) {
		var linkSession = (LinkdUserSession)link.getUserState();
		if (linkSession == null)
			return;
		// 快照取出后仍可能换绑，归属检查、解绑和通告须与bind保持全序。
		var writeLock = linkSession.bindsLock.writeLock();
		writeLock.lock();
		try {
			var owner = linkSession.binds.get(moduleId);
			if (owner == null || owner != provider.getSessionId())
				return;
			linkSession.unbind(requireLinkdApp().linkdProviderService, link, moduleId, provider, true);
			if (report != null)
				report.Send(link);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public long ProcessBindRequest(Bind rpc) {
		var bind = rpc.Argument;
		if (bind.getLinkSids().isEmpty()) {
			//noinspection DataFlowIssue
			var providerSession = (LinkdProviderSession)rpc.getSender().getUserState();
			//noinspection DataFlowIssue
			var providerInfo = providerSession.getInfo();
			for (var e : bind.getModules().entrySet()) {
				var moduleId = e.getKey();
				var module = e.getValue();
				var providerModuleState = new ProviderModuleState(providerSession.getSessionId(),
						moduleId, module.getChoiceType(), module.isDynamic());
				//noinspection DataFlowIssue
				var serviceName = ProviderDistribute.makeServiceName(providerInfo.getServiceNamePrefix(), moduleId);
				//noinspection DataFlowIssue
				var subState = distributes.zeze.getServiceManager().subscribeService(
						new BSubscribeInfo(serviceName, 0, providerModuleState));
				// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
				// 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
				// 阻塞订阅留在锁外；锁内检查 isClosed 与 onProviderClose 保持全序。
				synchronized (providerSession.moduleBindsMonitor) {
					if (rpc.getSender().isClosed())
						break;
					subState.setIdentityLocalState(providerInfo.getServiceIdentity(), providerModuleState);
					providerSession.getStaticBinds().add(moduleId);
				}
			}
		} else {
			// 动态绑定
			for (var linkSid : bind.getLinkSids()) {
				var link = requireLinkdApp().linkdService.GetSocket(linkSid);
				if (link != null) {
					var linkSession = (LinkdUserSession)link.getUserState();
					if (linkSession == null)
						continue;
					Objects.requireNonNull(linkSession).bind(requireLinkdApp().linkdProviderService, link, bind.getModules().keySet(), Objects.requireNonNull(rpc.getSender()));
					logger.info("dynamic bind: account={}, moduleIds={}, provider={}", linkSession.account,
							bind.getModules().keySet(), rpc.getSender().getRemoteAddress());
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSubscribeRequest(Subscribe rpc) {
		//noinspection DataFlowIssue
		var providerSession = (LinkdProviderSession)rpc.getSender().getUserState();
		//noinspection DataFlowIssue
		var providerInfo = providerSession.getInfo();
		for (var e : rpc.Argument.getModules().entrySet()) {
			var moduleId = e.getKey();
			var module = e.getValue();
			var providerModuleState = new ProviderModuleState(providerSession.getSessionId(),
					moduleId, module.getChoiceType(), module.isDynamic());
			//noinspection DataFlowIssue
			var serviceName = ProviderDistribute.makeServiceName(providerInfo.getServiceNamePrefix(), moduleId);
			//noinspection DataFlowIssue
			var subState = distributes.zeze.getServiceManager().subscribeService(
					new BSubscribeInfo(serviceName, 0, providerModuleState));
			// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
			// 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
			// 锁内检查 isClosed，与 onProviderClose 保持全序。
			synchronized (providerSession.moduleBindsMonitor) {
				if (rpc.getSender().isClosed())
					break;
				subState.setIdentityLocalState(providerInfo.getServiceIdentity(), providerModuleState);
				providerSession.getDynamicSubscribes().add(moduleId);
			}
		}

		rpc.SendResult();
		return Procedure.Success;
	}

	private void unBindModules(AsyncSocket provider, Iterable<Integer> modules) {
		unBindModules(provider, modules, false);
	}

	private void unBindModules(AsyncSocket provider, Iterable<Integer> modules,
							   boolean isOnProviderClose) {
		var providerSession = (LinkdProviderSession)provider.getUserState();
		//noinspection DataFlowIssue
		var providerInfo = providerSession.getInfo();
		for (var moduleId : modules) {
			if (!isOnProviderClose)
				providerSession.getStaticBinds().remove(moduleId);
			//noinspection DataFlowIssue
			var serviceName = ProviderDistribute.makeServiceName(providerInfo.getServiceNamePrefix(), moduleId);
			//noinspection DataFlowIssue
			var volatileProviders = distributes.zeze.getServiceManager().getSubscribeStates().get(serviceName);
			if (volatileProviders != null) {
				// UnBind 不删除 provider-list（由 ServiceManager 通告更新），仅按 sessionId 条件移除本连接的 localState。
				volatileProviders.getLocalStates().computeIfPresent(providerInfo.getServiceIdentity(), (k, v) ->
						v instanceof ProviderModuleState pms && pms.sessionId == provider.getSessionId() ? null : v);
			}
		}
	}

	@Override
	protected long ProcessUnBindRequest(UnBind rpc) {
		if (rpc.Argument.getLinkSids().isEmpty())
			unBindModules(Objects.requireNonNull(rpc.getSender()), rpc.Argument.getModules().keySet());
		else {
			// 动态绑定
			for (var linkSid : rpc.Argument.getLinkSids()) {
				var link = requireLinkdApp().linkdService.GetSocket(linkSid);
				if (link != null) {
					var linkSession = (LinkdUserSession)link.getUserState();
					if (linkSession == null)
						continue;
					linkSession.unbind(requireLinkdApp().linkdProviderService,
							link, rpc.Argument.getModules().keySet(), Objects.requireNonNull(rpc.getSender()));
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Procedure.Success;
	}

	protected void tryDump(AsyncSocket s, Binary pdata) throws IOException {
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
		// 标记关闭后不再重建，防止截断已写出的 dump 文件。
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

	/** linkd 零解码转发的发送归因：逐条解析协议头统计字节数。 */
	private static void countSendStream(Binary pdata) {
		var bytes = pdata.bytesUnsafe();
		int offset = pdata.getOffset();
		int length = pdata.size();
		while (length >= Protocol.HEADER_SIZE) {
			int moduleId = ByteBuffer.ToInt(bytes, offset);
			int protocolId = ByteBuffer.ToInt(bytes, offset + 4);
			int size = ByteBuffer.ToInt(bytes, offset + 8);
			if (size < 0 || size > length - Protocol.HEADER_SIZE) {
				logger.warn("countSendStream: moduleId={}, protocolId={}, invalid size={}, remaining={}",
						moduleId, protocolId, size, length);
				break;
			}
			size += Protocol.HEADER_SIZE;
			ZezeCounter.instance.addSendSize(Protocol.makeTypeId(moduleId, protocolId), size);
			offset += size;
			length -= size;
		}
	}

	@Override
	protected long ProcessSendRequest(Send r) throws Exception {
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
		for (int i = 0; i < sidCount; i++) {
			var linkSid = linkSids.get(i);
			var socket = requireLinkdApp().linkdService.GetSocket(linkSid);
			// ProtocolId现在是hash值，显示出来也不好看，以后加配置换成名字。
			if (socket != null && !socket.isClosed()) {
				// 探测协议不需要转发给客户端。
				if (CheckLinkSession.TypeId_ != r.Argument.getProtocolType()) {
					if (socket.Send(pdata)) // 仅发送成功时计数。
						countSendStream(pdata);
					else {
						r.Result.getErrorLinkSids().add(linkSid);
						socket.close(sendException);
					}
					if (enableDump)
						tryDump(socket, pdata);
				}
			} else
				r.Result.getErrorLinkSids().add(linkSid);
		}
		r.SendResult();
		return Procedure.Success;
	}

	private static final boolean canLogBroadcast = AsyncSocket.ENABLE_PROTOCOL_LOG
			&& AsyncSocket.canLogProtocol(Broadcast.TypeId_);

	@Override
	protected long ProcessBroadcast(Broadcast protocol) throws Exception {
		var pdata = protocol.Argument.getProtocolWholeData();
		if (canLogBroadcast) {
			var bb = ByteBuffer.Wrap(pdata);
			bb.ReadIndex += Protocol.HEADER_SIZE;
			AsyncSocket.log("Broc", requireLinkdApp().linkdService.getSocketCount(), protocol.Argument.getProtocolType(), bb);
		}
		//noinspection DataFlowIssue
		var providerVersion = protocol.Argument.isOnlySameVersion()
			? ((ProviderSession)protocol.getSender().getUserState()).appVersion
				: 0L;
		requireLinkdApp().linkdService.foreach(socket -> {
			// auth 通过就允许发送广播。
			// 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
			// 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
			var linkSession = (LinkdUserSession)socket.getUserState();
			if (linkSession != null && linkSession.isAuthed() && !linkSession.getUserState().getContext().isEmpty() &&
					(providerVersion == 0 ||
							ProviderDistribute.checkAppVersion(providerVersion, linkSession.getClientAppVersion()))) {
				if (socket.Send(pdata)) // 仅发送成功时计数。
					countSendStream(pdata);
				if (enableDump)
					tryDump(socket, pdata);
			}
		});
		return Procedure.Success;
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	protected long ProcessCheckLinkSession(CheckLinkSession p) throws Exception {
		// see ProcessSend，这里不需要处理。
		return 0;
	}

	@Override
	protected long ProcessKick(Kick protocol) {
		requireLinkdApp().linkdService.reportError(
				protocol.Argument.getLinksid(),
				BReportError.FromProvider,
				protocol.Argument.getCode(),
				protocol.Argument.getDesc(),
				protocol.Argument.getControl() == BKick.eControlClose);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSetUserState(SetUserState protocol) {
		var socket = requireLinkdApp().linkdService.GetSocket(protocol.Argument.getLinkSid());
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
	protected long ProcessAnnounceProviderInfo(AnnounceProviderInfo protocol) {
		var arg = protocol.Argument;
		var sender = Objects.requireNonNull(protocol.getSender());
		if (!AsyncSocket.ENABLE_PROTOCOL_LOG) {
			logger.info("AnnounceProviderInfo[{}]: name={}, id={}, ip={}, port={}, ver={}, disableChoice={}",
					sender.getSessionId(),
					arg.getServiceNamePrefix(), arg.getServiceIdentity(), arg.getProviderDirectIp(),
					arg.getProviderDirectPort(), Str.toVersionStr(arg.getAppVersion()), arg.isDisableChoice());
		}

		var session = Objects.requireNonNull((LinkdProviderSession)sender.getUserState());
		@Nullable AsyncSocket old;
		int serverId;
		synchronized (session.moduleBindsMonitor) {
			if (sender.isClosed())
				return Procedure.Success;
			session.setInfo(arg); // 全部记住

			// 下面再记录一份到其他需要的地方。这里有冗余。
			serverServiceNamePrefix = arg.getServiceNamePrefix();
			serverId = Integer.parseInt(arg.getServiceIdentity());
			session.serverId = serverId;
			session.serverLoadIp = arg.getProviderDirectIp();
			session.serverLoadPort = arg.getProviderDirectPort();
			session.appVersion = arg.getAppVersion();
			session.disableChoice = arg.isDisableChoice();
			requireLinkdApp().linkdProviderService.providerSessions.put(session.getServerLoadName(), session);

			// 同 serverId 接管：先更新映射再异步关闭旧连接，避免在 IO 线程同步触发 onProviderClose。
			old = serverId2ProviderSocket.put(serverId, sender);
		}
		if (old != null && old != sender) {
			var finalOld = old;
			var finalServerId = serverId;
			TaskSpec.ofAction(() -> finalOld.close(new IOException(
					"superseded by new announce connection, serverId=" + finalServerId))).runNow();
		}
		return Procedure.Success;
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	protected long ProcessSetDisableChoiceRequest(Zeze.Builtin.Provider.SetDisableChoice r) throws Exception {
		//noinspection DataFlowIssue
		var session = (LinkdProviderSession)r.getSender().getUserState();
		//noinspection DataFlowIssue
		session.setDisableChoice(r.Argument.isDisableChoice());
		r.SendResultCode(0);
		return 0;
	}
}
