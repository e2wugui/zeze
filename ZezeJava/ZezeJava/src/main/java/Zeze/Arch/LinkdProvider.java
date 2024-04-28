package Zeze.Arch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
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
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutLong;
import Zeze.Util.Str;
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
	private int firstModuleWithConfigTypeDefault;

	// 用于客户端选择Provider，只支持一种Provider。如果要支持多种，需要客户端增加参数，这个不考虑了。
	// 内部的ModuleRedirect ModuleRedirectAll Transmit都携带了ServiceNamePrefix参数，所以，
	// 内部的Provider可以支持完全不同的solution，不过这个仅仅保留给未来扩展用，
	// 不建议在一个项目里面使用多个Prefix。
	private @NotNull String serverServiceNamePrefix = "";

	protected @Nullable FileOutputStream dumpFile;
	protected @Nullable AsyncSocket dumpSocket;

	public ProviderDistributeVersion getDistributes() {
		return distributes;
	}

	public @NotNull String getServerServiceNamePrefix() {
		return serverServiceNamePrefix;
	}

	public boolean choiceProvider(@NotNull AsyncSocket link, int moduleId, @NotNull Predicate<AsyncSocket> onSend) {
		var userSession = (LinkdUserSession)link.getUserState();
		var providerSessionId = userSession.tryGetProvider(moduleId);
		if (providerSessionId != null) {
			var socket = linkdApp.linkdProviderService.GetSocket(providerSessionId);
			if (socket != null && onSend.test(socket))
				return true; // done
			// 原来绑定的provider找不到连接，尝试继续从静态绑定里面查找。
			// 此时应该处于 UnBind 过程中。
		}

		var provider = new OutLong();
		if (choiceProviderAndBind(moduleId, userSession.clientAppVersion, link, provider)) {
			var providerSocket = linkdApp.linkdProviderService.GetSocket(provider.value);
			//noinspection RedundantIfStatement
			if (providerSocket != null && onSend.test(providerSocket)) // ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
				return true;
			// else
			// 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
		}
		return false;
	}

	public boolean choiceHashWithoutBind(int moduleId, long version, int hash, @NotNull OutLong provider) {
		provider.value = 0L;
		var distribute = distributes.selectDistribute(version);
		if (distribute == null)
			return false;
		var serviceName = ProviderDistribute.makeServiceName(serverServiceNamePrefix, moduleId);
		var providers = distribute.zeze.getServiceManager().getSubscribeStates().get(serviceName);
		return providers != null && distribute.choiceHash(providers, hash, provider);
	}

	public @NotNull String makeServiceName(int moduleId) {
		return ProviderDistribute.makeServiceName(serverServiceNamePrefix, moduleId);
	}

	public @Nullable ProviderModuleState getProviderModuleState(int moduleId) {
		var providers = distributes.zeze.getServiceManager().getSubscribeStates().get(makeServiceName(moduleId));
		return providers != null ? (ProviderModuleState)providers.getSubscribeInfo().getLocalState() : null;
	}

	public boolean choiceProviderAndBind(int moduleId, long clientVersion, @NotNull AsyncSocket link,
										 @NotNull OutLong provider) {
		provider.value = 0L;
		var distribute = distributes.selectDistribute(clientVersion);
		if (distribute == null)
			return false;
		var providers = distribute.zeze.getServiceManager().getSubscribeStates().get(makeServiceName(moduleId));
		if (providers == null)
			return false;
		var linkSession = (LinkdUserSession)link.getUserState();

		// 这里保存的 ProviderModuleState 是该moduleId的第一个bind请求去订阅时记录下来的，
		// 这里仅使用里面的ChoiceType和ConfigType。这两个参数对于相同的moduleId都是一样的。
		// 如果需要某个provider.SessionId，需要查询 ServiceInfoListSortedByIdentity 里的ServiceInfo.LocalState。
		var providerModuleState = (ProviderModuleState)providers.getSubscribeInfo().getLocalState();
		assert providerModuleState != null;
		switch (providerModuleState.choiceType) {
		case BModule.ChoiceTypeHashAccount:
			if (!distribute.choiceHash(providers, Bean.hash32(linkSession.getAccount()), provider))
				return false;
			break; // bind static later

		case BModule.ChoiceTypeHashRoleId:
			var roleId = linkSession.getRoleId();
			if (roleId == null || !distribute.choiceHash(providers, ByteBuffer.calc_hashnr(roleId), provider))
				return false;
			break; // bind static later

		case BModule.ChoiceTypeFeedFullOneByOne:
			if (!distribute.choiceFeedFullOneByOne(providers, provider))
				return false;
			break; // bind static later

		case BModule.ChoiceTypeHashSourceAddress:
			var remoteAddress = link.getRemoteAddress();
			if (null == remoteAddress)
				return false;
			if (!distribute.choiceHash(providers, remoteAddress.hashCode(), provider))
				return false;
			break; // bind static later

		case BModule.ChoiceTypeLoad:
			if (!distribute.choiceLoad(providers, provider))
				return false;
			break; // bind static later

		case BModule.ChoiceTypeRequest:
			// fall down
		default:
			if (!distribute.choiceRequest(providers, provider))
				return false;
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
						return false;
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
				return false;
		}

		// 动态模块允许使用这个方法查找provider，
		// 但是不会主动注册到linkUserSession，每次都需要重新查找。
		// 动态模块需要主动bind/unbind。
		// XXX
		if (providerModuleState.configType == BModule.ConfigTypeDefault) {
			var staticBinds = ((LinkdProviderSession)providerSocket.getUserState()).getStaticBinds();
			linkSession.bind(linkdApp.linkdProviderService, link, staticBinds.keySet(), providerSocket);
			logger.info("static bind: account={}, moduleIds.size={}, provider={}, configType={}, choiceType={}",
					linkSession.account, staticBinds.size(), providerSocket.getRemoteAddress(),
					providerModuleState.configType, providerModuleState.choiceType);
		} else if (providerModuleState.configType == BModule.ConfigTypeSpecial) {
			// special 不跟随大部队，单独bind。
			linkSession.bind(linkdApp.linkdProviderService, link, List.of(moduleId), providerSocket);
			logger.info("special bind: account={}, moduleId={}, provider={}, configType={}, choiceType={}",
					linkSession.account, moduleId, providerSocket.getRemoteAddress(),
					providerModuleState.configType, providerModuleState.choiceType);
		}
		return true;
	}

	public void onProviderClose(@NotNull AsyncSocket provider) {
		var providerSession = (LinkdProviderSession)provider.getUserState();
		if (providerSession == null)
			return;

		// unbind module
		unBindModules(provider, providerSession.getStaticBinds().keySet(), true);
		providerSession.getStaticBinds().clear();

		// unbind LinkSession
		var linkSessionIds = providerSession.getLinkSessionIds();
		providerSession.getLinkSessionIdsLock().lock();
		try {
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
			linkSessionIds.clear();
		} finally {
			providerSession.getLinkSessionIdsLock().unlock();
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
				if (firstModuleWithConfigTypeDefault == 0 && module.getConfigType() == BModule.ConfigTypeDefault) {
					//noinspection DataFlowIssue,ConstantValue
					firstModuleWithConfigTypeDefault = module.getConfigType();
				}
				var providerModuleState = new ProviderModuleState(providerSession.getSessionId(),
						moduleId, module.getChoiceType(), module.getConfigType());
				var serviceName = ProviderDistribute.makeServiceName(providerInfo.getServiceNamePrefix(), moduleId);
				var subState = distributes.zeze.getServiceManager().subscribeService(
						new BSubscribeInfo(serviceName, 0, providerModuleState));
				// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
				// 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
				subState.setIdentityLocalState(providerInfo.getServiceIdentity(), providerModuleState);
				providerSession.getStaticBinds().add(moduleId);
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
					moduleId, module.getChoiceType(), module.getConfigType());
			var serviceName = ProviderDistribute.makeServiceName(providerInfo.getServiceNamePrefix(), moduleId);
			var subState = distributes.zeze.getServiceManager().subscribeService(
					new BSubscribeInfo(serviceName, 0, providerModuleState));
			// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
			// 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
			subState.setIdentityLocalState(providerInfo.getServiceIdentity(), providerModuleState);
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
			var volatileProviders = distributes.zeze.getServiceManager().getSubscribeStates().get(serviceName);
			if (volatileProviders != null) {
				// UnBind 不删除provider-list，这个总是通过ServiceManager通告更新。
				// 这里仅仅设置该moduleId对应的服务的状态不可用。
				volatileProviders.setIdentityLocalState(providerInfo.getServiceIdentity(), null);
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

	protected void tryDump(@NotNull AsyncSocket s, @NotNull Binary pdata) throws IOException {
		if (dumpFile == null) {
			assert dumpFilename != null;
			dumpFile = new FileOutputStream(dumpFilename);
			dumpSocket = s;
		}
		if (dumpSocket == s)
			dumpFile.write(pdata.bytesUnsafe(), pdata.getOffset(), pdata.size());
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
		linkdApp.linkdService.foreach(socket -> {
			// auth 通过就允许发送广播。
			// 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
			// 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
			var linkSession = (LinkdUserSession)socket.getUserState();
			if (linkSession != null && linkSession.isAuthed() && !linkSession.getUserState().getContext().isEmpty()) {
				socket.Send(pdata);
				if (enableDump)
					tryDump(socket, pdata);
			}
		});
		return Procedure.Success;
	}

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
		session.serverLoadIp = arg.getProviderDirectIp();
		session.serverLoadPort = arg.getProviderDirectPort();
		session.appVersion = arg.getAppVersion();
		session.disableChoice = arg.isDisableChoice();
		linkdApp.linkdProviderService.providerSessions.put(session.getServerLoadName(), session);

		return Procedure.Success;
	}

	@Override
	protected long ProcessSetDisableChoiceRequest(@NotNull Zeze.Builtin.Provider.SetDisableChoice r) throws Exception {
		var session = (LinkdProviderSession)r.getSender().getUserState();
		session.setDisableChoice(r.Argument.isDisableChoice());
		r.SendResultCode(0);
		return 0;
	}
}
