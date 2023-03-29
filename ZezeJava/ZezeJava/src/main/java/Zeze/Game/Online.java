package Zeze.Game;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.AppBase;
import Zeze.Arch.Beans.BSend;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderService;
import Zeze.Arch.ProviderUserSession;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.Game.Online.BAny;
import Zeze.Builtin.Game.Online.BDelayLogoutCustom;
import Zeze.Builtin.Game.Online.BLink;
import Zeze.Builtin.Game.Online.BLocal;
import Zeze.Builtin.Game.Online.BNotify;
import Zeze.Builtin.Game.Online.BOnline;
import Zeze.Builtin.Game.Online.BReliableNotify;
import Zeze.Builtin.Game.Online.BVersion;
import Zeze.Builtin.Game.Online.SReliableNotify;
import Zeze.Builtin.Provider.BBroadcast;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.Broadcast;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Builtin.ProviderDirect.Transmit;
import Zeze.Collections.BeanFactory;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.EventDispatcher;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.LongList;
import Zeze.Util.OutLong;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Online extends AbstractOnline {
	protected static final Logger logger = LogManager.getLogger(Online.class);
	static @Nullable Online instance;

	public final @NotNull ProviderApp providerApp;
	private final @NotNull ProviderLoad load;
	private final AtomicLong loginTimes = new AtomicLong();

	private final EventDispatcher loginEvents = new EventDispatcher("Online.Login");
	private final EventDispatcher reloginEvents = new EventDispatcher("Online.Relogin");
	private final EventDispatcher logoutEvents = new EventDispatcher("Online.Logout");
	private final EventDispatcher localRemoveEvents = new EventDispatcher("Online.Local.Remove");
	private final EventDispatcher linkBrokenEvents = new EventDispatcher("Online.LinkBroken");

	public interface TransmitAction {
		/**
		 * @param sender 查询发起者，结果发送给他
		 * @param target 查询目标角色
		 * @return 按普通事务处理过程返回值处理
		 */
		long call(long sender, long target, @Nullable Binary parameter);
	}

	private final ConcurrentHashMap<String, TransmitAction> transmitActions = new ConcurrentHashMap<>();
	private Future<?> verifyLocalTimer;

	public static Online create(AppBase app) {
		return GenModule.createRedirectModule(Online.class, app);
	}

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}

	public @NotNull ProviderLoad getLoad() {
		return load;
	}

	public static final BeanFactory beanFactory = new BeanFactory();

	public static @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static void register(@NotNull Class<? extends Bean> cls) {
		beanFactory.register(cls);
	}

	protected Online(@NotNull AppBase app) {
		providerApp = app.getZeze().redirect.providerApp;
		RegisterProtocols(providerApp.providerService);
		RegisterZezeTables(providerApp.zeze);
		load = new ProviderLoad(this);
		instance = this;
	}

	public void start() {
		load.start();
		verifyLocalTimer = Task.scheduleAtUnsafe(3 + Random.getInstance().nextInt(3), 10, this::verifyLocal);
		providerApp.builtinModules.put(this.getFullName(), this);
	}

	public void stop() {
		instance = null;
		load.stop();
		if (null != verifyLocalTimer)
			verifyLocalTimer.cancel(false);
	}

	@Override
	public void UnRegister() {
		UnRegisterProtocols(providerApp.providerService);
		UnRegisterZezeTables(providerApp.zeze);
	}

	// 用户数据
	@SuppressWarnings("unchecked")
	public <T extends Bean> @Nullable T getUserData(long roleId) {
		var version = _tversion.get(roleId);
		if (null == version)
			return null;
		return (T)version.getUserData().getBean();
	}

	// 在线状态
	public @Nullable BOnline getOnline(long roleId) {
		return _tonline.get(roleId);
	}

	// 在线异变数据
	public @Nullable BVersion getData(long roleId) {
		return _tversion.get(roleId);
	}

	public <T extends Bean> void setUserData(long roleId, @NotNull T data) {
		_tversion.getOrAdd(roleId).getUserData().setBean(data);
	}

	public int getLocalCount() {
		return _tlocal.getCacheSize();
	}

	public long walkLocal(@NotNull TableWalkHandle<Long, BLocal> walker) {
		return _tlocal.walkCache(walker);
	}

	public long getLoginTimes() {
		return loginTimes.get();
	}

	// 登录事件
	public @NotNull EventDispatcher getLoginEvents() {
		return loginEvents;
	}

	public @NotNull EventDispatcher getLinkBrokenEvents() {
		return linkBrokenEvents;
	}

	// 断线重连事件
	public @NotNull EventDispatcher getReloginEvents() {
		return reloginEvents;
	}

	// 登出事件
	public @NotNull EventDispatcher getLogoutEvents() {
		return logoutEvents;
	}

	// 角色从本机删除事件
	public @NotNull EventDispatcher getLocalRemoveEvents() {
		return localRemoveEvents;
	}

	public @NotNull ConcurrentHashMap<String, TransmitAction> getTransmitActions() {
		return transmitActions;
	}

	public <T extends Bean> void setLocalBean(long roleId, @NotNull String key, @NotNull T bean) {
		var bLocal = _tlocal.get(roleId);
		if (null == bLocal)
			throw new IllegalStateException("roleId not online. " + roleId);
		beanFactory.register(bean);
		var bAny = new BAny();
		bAny.getAny().setBean(bean);
		bLocal.getDatas().put(key, bAny);
	}

	public void removeLocalBean(long roleId, @NotNull String key) {
		var bLocal = _tlocal.get(roleId);
		if (null == bLocal)
			return;
		bLocal.getDatas().remove(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> @Nullable T getLocalBean(long roleId, @NotNull String key) {
		var bLocal = _tlocal.get(roleId);
		if (null == bLocal)
			return null;
		var data = bLocal.getDatas().get(key);
		if (null == data)
			return null;
		return (T)data.getAny().getBean();
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> @NotNull T getOrAddLocalBean(long roleId, @NotNull String key, @NotNull T defaultHint) {
		var bLocal = _tlocal.getOrAdd(roleId);
		var data = bLocal.getDatas().getOrAdd(key);
		if (data.getAny().getBean().typeId() == defaultHint.typeId())
			return (T)data.getAny().getBean();
		data.getAny().setBean(defaultHint);
		return defaultHint;
	}

	private long removeLocalAndTrigger(long roleId) throws Exception {
		var arg = new LocalRemoveEventArgument();
		arg.roleId = roleId;
		arg.local = _tlocal.get(roleId);

		// local 没有数据不触发事件？
		if (null != arg.local) {
			_tlocal.remove(roleId); // remove first

			var ret = localRemoveEvents.triggerEmbed(this, arg);
			if (0 != ret)
				return ret;
			localRemoveEvents.triggerProcedure(providerApp.zeze, this, arg);
			Transaction.whileCommit(() -> localRemoveEvents.triggerThread(this, arg));
		}
		return 0;
	}

	public @Nullable Long getLogoutVersion(long roleId) {
		/*
		if (_tonline.get(roleId) != null)
			return null; // is online
		*/
		var version = _tversion.get(roleId);
		if (null == version)
			return null; // no version
		return version.getLogoutVersion();
	}

	public @Nullable Long getLoginVersion(long roleId) {
		var version = _tversion.get(roleId);
		if (null == version)
			return null; // no version
		return version.getLoginVersion();
	}

	public @Nullable Long getLocalLoginVersion(long roleId) {
		var local = _tlocal.get(roleId);
		if (null == local)
			return null;
		return local.getLoginVersion();
	}

	public @Nullable Long getGlobalLoginVersion(long roleId) {
		var version = _tversion.get(roleId);
		if (null == version)
			return null;
		return version.getLoginVersion();
	}

	public boolean isLogin(long roleId) {
		return null != _tonline.get(roleId);
	}

	private static boolean assignLogoutVersion(@NotNull BVersion version) {
		if (version.getLoginVersion() == version.getLogoutVersion()) {
			return false;
		}
		version.setLogoutVersion(version.getLoginVersion());
		return true;
	}

	private long logoutTrigger(long roleId, @NotNull LogoutReason logoutReason) throws Exception {
		var arg = new LogoutEventArgument();
		arg.roleId = roleId;
		arg.logoutReason = logoutReason;

		var version = _tversion.get(roleId);

		// 提前删除，可能事件里面需要使用这个判断已经登出。
		// 外面补发logoutTrigger之后需要重新getOrAdd一次。
		_tonline.remove(roleId); // remove first

		// 总是尝试通知上一次登录的服务器，里面会忽略本机。
		if (version != null)
			tryRedirectRemoveLocal(version.getServerId(), roleId);
		// 总是删除
		removeLocalAndTrigger(roleId);

		var ret = logoutEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		logoutEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> logoutEvents.triggerThread(this, arg));
		return 0;
	}

	@SuppressWarnings("UnusedReturnValue")
	private long linkBrokenTrigger(@SuppressWarnings("unused") @NotNull String account, long roleId) throws Exception {
		var arg = new LinkBrokenArgument();
		arg.roleId = roleId;

		// 由于account可能没有，这里就不传递这个参数了。
		var ret = linkBrokenEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		linkBrokenEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> linkBrokenEvents.triggerThread(this, arg));
		return 0;
	}

	private long loginTrigger(@NotNull String account, long roleId) throws Exception {
		var arg = new LoginArgument();
		arg.roleId = roleId;
		arg.account = account;

		loginTimes.incrementAndGet();
		var ret = loginEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		loginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> loginEvents.triggerThread(this, arg));
		return 0;
	}

	private long reloginTrigger(@NotNull String account, long roleId) throws Exception {
		var arg = new LoginArgument();
		arg.roleId = roleId;
		arg.account = account;

		loginTimes.incrementAndGet();
		var ret = reloginEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		reloginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> reloginEvents.triggerThread(this, arg));
		return 0;
	}

	private long tryLogout(@NotNull BDelayLogoutCustom custom) throws Exception {
		var roleId = custom.getRoleId();
		var currentLoginVersion = custom.getLoginVersion();
		// local online 独立判断version分别尝试删除。
		var local = _tlocal.get(roleId);
		if (null != local && local.getLoginVersion() == currentLoginVersion) {
			var ret = removeLocalAndTrigger(roleId);
			if (0 != ret)
				return ret;
		}
		// 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
		var online = _tonline.get(roleId);
		var version = _tversion.getOrAdd(roleId);
		if (null != online && version.getLoginVersion() == currentLoginVersion && assignLogoutVersion(version)) {
			var ret = logoutTrigger(roleId, LogoutReason.LOGOUT);
			if (0 != ret)
				return ret;
		}
		return Procedure.Success;
	}

	public static class DelayLogout implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			if (null != instance) {
				var ret = instance.tryLogout((BDelayLogoutCustom)context.customData);
				if (ret != 0)
					Online.logger.error("tryLogout fail. {}", ret);
			}
		}

		@Override
		public void onTimerCancel() throws Exception {
		}
	}

	public long linkBroken(@NotNull String account, long roleId, @NotNull String linkName, long linkSid) throws Exception {
		long currentLoginVersion;
		{
			var online = _tonline.get(roleId);
			// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
			if (online == null || !online.getLink().getLinkName().equals(linkName)
					|| online.getLink().getLinkSid() != linkSid)
				return 0;
			var version = _tversion.getOrAdd(roleId);
			var local = _tlocal.get(roleId);
			if (local == null)
				return 0; // 不在本机登录。

			currentLoginVersion = local.getLoginVersion();
			if (version.getLoginVersion() != currentLoginVersion) {
				var ret = removeLocalAndTrigger(roleId); // 本机数据已经过时，马上删除。
				if (0 != ret)
					return ret;
			}
		}
		linkBrokenTrigger(account, roleId);
		// for shorter use
		var zeze = providerApp.zeze;
		var delay = zeze.getConfig().getOnlineLogoutDelay();
		zeze.getTimer().schedule(delay, DelayLogout.class, new BDelayLogoutCustom(roleId, currentLoginVersion));
		return 0;
	}

	public void send(long roleId, @NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", roleId, p);
		sendDirect(roleId, typeId, new Binary(p.encode()));
	}

	public void sendResponse(long roleId, @NotNull Rpc<?, ?> r) {
		r.setRequest(false);
		send(roleId, r);
	}

	public void send(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (roleIds.isEmpty())
			return;
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var sb = new StringBuilder();
			for (var roleId : roleIds)
				sb.append(roleId).append(',');
			int n = sb.length();
			if (n > 0)
				sb.setLength(n - 1);
			var idsStr = sb.toString();
			AsyncSocket.log("Send", idsStr, p);
		}
		send(roleIds, typeId, new Binary(p.encode()));
	}

	public void sendWhileCommit(long roleId, @NotNull Protocol<?> p) {
		Transaction.whileCommit(() -> send(roleId, p));
	}

	public void sendWhileCommit(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		Transaction.whileCommit(() -> send(roleIds, p));
	}

	public void sendResponseWhileCommit(long roleId, @NotNull Rpc<?, ?> r) {
		Transaction.whileCommit(() -> {
			r.setRequest(false);
			send(roleId, r);
		});
	}

	public void sendWhileRollback(long roleId, @NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> send(roleId, p));
	}

	public void sendWhileRollback(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> send(roleIds, p));
	}

//	public void send(long roleId, long typeId, Binary fullEncodedProtocol) {
//		// 发送协议请求在另外的事务中执行。
//		providerApp.zeze.getTaskOneByOneByKey().Execute(roleId,
//				() -> Task.call(providerApp.zeze.newProcedure(() -> {
//					sendEmbed(List.of(roleId), typeId, fullEncodedProtocol);
//					return Procedure.Success;
//				}, "Online.send")), DispatchMode.Normal);
//	}

	public void send(@NotNull Collection<Long> roleIds, long typeId, @NotNull Binary fullEncodedProtocol) {
		int roleCount = roleIds.size();
		if (roleCount == 1) {
			var it = roleIds.iterator();
			if (it.hasNext()) // 不确定roleIds是否稳定,所以还是判断一下保险
				sendDirect(it.next(), typeId, fullEncodedProtocol);
		} else if (roleCount > 1) {
			sendDirect(roleIds, typeId, fullEncodedProtocol);
//			providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(roleIds, providerApp.zeze.newProcedure(() -> {
//				sendEmbed(roleIds, typeId, fullEncodedProtocol);
//				return Procedure.Success;
//			}, "Online.send"), null, DispatchMode.Normal);
		}
	}

//	public void sendNoBarrier(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
//		// 发送协议请求在另外的事务中执行。
//		Task.run(providerApp.zeze.newProcedure(() -> {
//			sendEmbed(roleIds, typeId, fullEncodedProtocol);
//			return Procedure.Success;
//		}, "Online.send"), null, null, DispatchMode.Normal);
//	}

	private long triggerLinkBroken(@NotNull String linkName, @NotNull LongList errorSids,
								   @NotNull Map<Long, Long> context) {
		errorSids.foreach(linkSid -> providerApp.zeze.newProcedure(() -> {
			var roleId = context.get(linkSid);
			// 补发的linkBroken没有account上下文。
			return roleId != null ? linkBroken("", roleId, linkName, linkSid) : 0;
		}, "Online.triggerLinkBroken").call());
		return 0;
	}

	/**
	 * 直接通过Link链接发送Send协议。
	 *
	 * @param link     link
	 * @param contexts context
	 * @param send     protocol
	 */
	public boolean send(@NotNull AsyncSocket link, @NotNull Map<Long, Long> contexts, @NotNull Send send) {
		return send.Send(link, rpc -> triggerLinkBroken(ProviderService.getLinkName(link),
				send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
	}

	// 直接通过 linkName, linkSid 发送协议。
	public boolean send(@NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		return send(null, linkName, linkSid, p);
	}

	public boolean send(@Nullable Long roleId, @NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		var connector = providerApp.providerService.getLinks().get(linkName);
		if (null == connector) {
			logger.warn("link connector not found. name={}", linkName);
			return false;
		}
		var link = connector.getSocket();
		if (null == link) {
			logger.warn("link socket not found. name={}", linkName);
			return false;
		}
		var send = new Send(new BSend(p.getTypeId(), new Binary(p.encode())));
		send.Argument.getLinkSids().add(linkSid);
		return send(link, roleId != null ? Map.of(linkSid, roleId) : Map.of(), send);
	}

	//	public void send(Collection<Long> keys, AsyncSocket to, Map<Long, Long> contexts, Send send) {
//		if (keys.size() > 1) {
//			send.Send(to, rpc -> triggerLinkBroken(ProviderService.getLinkName(to),
//					send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
//		} else {
//			//noinspection CodeBlock2Expr
//			providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(keys, "sendOneByOne", () -> {
//				send.Send(to, rpc -> triggerLinkBroken(ProviderService.getLinkName(to),
//						send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
//			}, null, DispatchMode.Normal);
//		}
//	}

//	public void sendEmbed(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
//		var groups = groupByLink(roleIds);
//		Transaction.whileCommit(() -> {
//			for (var group : groups) {
//				if (group.linkSocket == null)
//					continue; // skip not online
//
//				var send = new Send(new Zeze.Arch.Beans.BSend(typeId, fullEncodedProtocol));
//				send.Argument.getLinkSids().addAll(group.roles.values());
//				send(group.linkSocket, group.contexts, send);
//			}
//		});
//	}

	public static final class LinkRoles {
		final @NotNull AsyncSocket linkSocket;
		final @NotNull Send send;
		final LongList roleIds = new LongList();

		public LinkRoles(@NotNull AsyncSocket linkSocket, long typeId, @NotNull Binary fullEncodedProtocol) {
			this.linkSocket = linkSocket;
			send = new Send(new BSend(typeId, fullEncodedProtocol));
		}
	}

	// 可在事务外执行
	public void sendDirect(@NotNull Iterable<Long> roleIds, long typeId, @NotNull Binary fullEncodedProtocol) {
		var roleIdSet = new LongHashSet();
		for (var roleId : roleIds)
			roleIdSet.add(roleId); // 去重
		if (roleIdSet.isEmpty())
			return;
		var groups = new HashMap<String, LinkRoles>();
		var links = providerApp.providerService.getLinks();
		for (var it = roleIdSet.iterator(); it.moveToNext(); ) {
			var roleId = it.value();
			var online = _tonline.selectDirty(roleId);
			if (online == null) {
				logger.warn("sendDirect: not found roleId={} in _tonline", roleId);
				continue;
			}
			var link = online.getLink();
			var linkName = link.getLinkName();
			var connector = links.get(linkName);
			if (connector == null) {
				logger.warn("sendDirect: not found connector for linkName={} roleId={}", linkName, roleId);
				continue;
			}
			if (!connector.isHandshakeDone()) {
				logger.warn("sendDirect: not isHandshakeDone for linkName={} roleId={}", linkName, roleId);
				continue;
			}
			// 后面保存connector.socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(linkName);
			if (group == null) {
				var linkSocket = connector.getSocket();
				if (linkSocket == null) {
					logger.warn("sendDirect: closed connector for linkName={} roleId={}", linkName, roleId);
					continue;
				}
				groups.put(linkName, group = new LinkRoles(linkSocket, typeId, fullEncodedProtocol));
			}
			group.send.Argument.getLinkSids().add(link.getLinkSid());
			group.roleIds.add(roleId);
		}
		for (var group : groups.values()) {
			group.send.Send(group.linkSocket, rpc -> {
				var send = group.send;
				var errorSids = send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids();
				errorSids.foreach(linkSid -> providerApp.zeze.newProcedure(() -> {
					int idx = group.send.Argument.getLinkSids().indexOf(linkSid);
					return idx >= 0 ? linkBroken("", group.roleIds.get(idx), // 补发的linkBroken没有account上下文
							ProviderService.getLinkName(group.linkSocket), linkSid) : 0;
				}, "Online.triggerLinkBroken2").call());
				return Procedure.Success;
			});
		}
	}

	// 可在事务外执行
	public void sendDirect(long roleId, long typeId, @NotNull Binary fullEncodedProtocol) {
		var online = _tonline.selectDirty(roleId);
		if (online == null) {
			logger.warn("sendDirect: not found roleId={} in _tonline", roleId);
			return;
		}
		var link = online.getLink();
		var linkName = link.getLinkName();
		var connector = providerApp.providerService.getLinks().get(linkName);
		if (connector == null) {
			logger.warn("sendDirect: not found connector for linkName={} roleId={}", linkName, roleId);
			return;
		}
		if (!connector.isHandshakeDone()) {
			logger.warn("sendDirect: not isHandshakeDone for linkName={} roleId={}", linkName, roleId);
			return;
		}
		// 后面保存connector.socket并使用，如果之后连接被关闭，以后发送协议失败。
		var linkSocket = connector.getSocket();
		if (linkSocket == null) {
			logger.warn("sendDirect: closed connector for linkName={} roleId={}", linkName, roleId);
			return;
		}
		var send = new Send(new BSend(typeId, fullEncodedProtocol));
		send.Argument.getLinkSids().add(link.getLinkSid());
		send.Send(linkSocket, rpc -> {
			if (send.isTimeout() || !send.Result.getErrorLinkSids().isEmpty()) {
				var linkSid = send.Argument.getLinkSids().get(0);
				providerApp.zeze.newProcedure(() -> linkBroken("", roleId, linkName, linkSid), // 补发的linkBroken没有account上下文
						"Online.triggerLinkBroken1").call();
			}
			return Procedure.Success;
		});
	}

//	public static final class RoleOnLink {
//		String linkName = "";
//		AsyncSocket linkSocket;
//		// long providerSessionId;
//		final HashMap<Long, Long> roles = new HashMap<>(); // roleid -> linksid
//		final HashMap<Long, Long> contexts = new HashMap<>(); // linksid -> roleid
//	}
//
//	public Collection<RoleOnLink> groupByLink(Iterable<Long> roleIds) {
//		var groups = new HashMap<String, RoleOnLink>();
//		var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
//		groups.put(groupNotOnline.linkName, groupNotOnline);
//
//		for (var roleId : roleIds) {
//			var online = _tonline.get(roleId);
//			if (online == null) {
//				groupNotOnline.roles.putIfAbsent(roleId, null);
//				logger.warn("groupByLink: not found roleId={} in _tonline", roleId);
//				continue;
//			}
//
//			var linkName = online.getLink().getLinkName();
//			var connector = providerApp.providerService.getLinks().get(linkName);
//			if (connector == null) {
//				logger.warn("groupByLink: not found connector for linkName={} roleId={}", linkName, roleId);
//				groupNotOnline.roles.putIfAbsent(roleId, null);
//				continue;
//			}
//			if (!connector.isHandshakeDone()) {
//				logger.warn("groupByLink: not isHandshakeDone for linkName={} roleId={}", linkName, roleId);
//				groupNotOnline.roles.putIfAbsent(roleId, null);
//				continue;
//			}
//			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
//			var group = groups.get(linkName);
//			if (group == null) {
//				group = new RoleOnLink();
//				group.linkName = linkName;
//				group.linkSocket = connector.getSocket();
//				groups.put(group.linkName, group);
//			}
//			var linkSid = online.getLink().getLinkSid();
//			group.roles.putIfAbsent(roleId, linkSid);
//			group.contexts.putIfAbsent(linkSid, roleId);
//		}
//		return groups.values();
//	}

	public void addReliableNotifyMark(long roleId, @NotNull String listenerName) {
		var online = _tonline.get(roleId);
		if (online == null)
			throw new IllegalStateException("Not Online. AddReliableNotifyMark: " + listenerName);
		var version = _tversion.getOrAdd(roleId);
		version.getReliableNotifyMark().add(listenerName);
	}

	public void removeReliableNotifyMark(long roleId, @NotNull String listenerName) {
		// 移除尽量通过，不做任何判断。
		var version = _tversion.getOrAdd(roleId);
		version.getReliableNotifyMark().remove(listenerName);
	}

	public void sendReliableNotifyWhileCommit(long roleId, @NotNull String listenerName, @NotNull Protocol<?> p) {
		Transaction.whileCommit(() -> sendReliableNotify(roleId, listenerName, p));
	}

	public void sendReliableNotifyWhileCommit(long roleId, @NotNull String listenerName, int typeId,
											  @NotNull Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
	}

	public void sendReliableNotifyWhileRollback(long roleId, @NotNull String listenerName, @NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> sendReliableNotify(roleId, listenerName, p));
	}

	public void sendReliableNotifyWhileRollback(long roleId, @NotNull String listenerName, int typeId,
												@NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
	}

	public void sendReliableNotify(long roleId, @NotNull String listenerName, Protocol<?> p) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", roleId + ":" + listenerName, p);
		sendReliableNotify(roleId, listenerName, typeId, new Binary(p.encode()));
	}

	private @NotNull Zeze.Collections.Queue<BNotify> openQueue(long roleId) {
		return providerApp.zeze.getQueueModule().open("Zeze.Game.Online.ReliableNotifyQueue:" + roleId, BNotify.class);
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 */
	public void sendReliableNotify(long roleId, @NotNull String listenerName, long typeId,
								   @NotNull Binary fullEncodedProtocol) {
		providerApp.zeze.runTaskOneByOneByKey(listenerName, "Online.sendReliableNotify." + listenerName, () -> {
			BOnline online = _tonline.get(roleId);
			if (online == null) {
				return Procedure.Success;
			}
			var version = _tversion.getOrAdd(roleId);
			if (!version.getReliableNotifyMark().contains(listenerName)) {
				return Procedure.Success; // 相关数据装载的时候要同步设置这个。
			}

			// 先保存在再发送，然后客户端还会确认。
			// see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
			var queue = openQueue(roleId);
			var bNotify = new BNotify();
			bNotify.setFullEncodedProtocol(fullEncodedProtocol);
			queue.add(bNotify);

			// 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
			var notify = new SReliableNotify(new BReliableNotify(version.getReliableNotifyIndex()));
			version.setReliableNotifyIndex(version.getReliableNotifyIndex() + 1); // after set notify.Argument
			notify.Argument.getNotifies().add(fullEncodedProtocol);

			Transaction.whileCommit(() -> sendDirect(roleId, notify.getTypeId(), new Binary(notify.encode())));
//			sendEmbed(List.of(roleId), notify.getTypeId(), new Binary(notify.encode()));
			return Procedure.Success;
		});
	}

	/**
	 * 转发查询请求给RoleId。
	 *
	 * @param sender     查询发起者，结果发送给他。
	 * @param actionName 查询处理的实现
	 * @param roleId     目标角色
	 */
	public void transmit(long sender, @NotNull String actionName, long roleId, @Nullable Serializable parameter) {
		transmit(sender, actionName, List.of(roleId), parameter);
	}

	public void transmit(long sender, @NotNull String actionName, long roleId) {
		transmit(sender, actionName, roleId, null);
	}

	public void processTransmit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
								@Nullable Binary parameter) {
		var handle = transmitActions.get(actionName);
		if (handle != null) {
			for (var target : roleIds) {
				Task.call(providerApp.zeze.newProcedure(() -> handle.call(sender, target, parameter),
						"Online.transmit: " + actionName));
			}
		}
	}

	public static final class RoleOnServer {
		int serverId = -1; // providerId
		final HashSet<Long> roles = new HashSet<>();
	}

	public @NotNull IntHashMap<RoleOnServer> groupByServerId(@NotNull Iterable<Long> roleIds) {
		var groups = new IntHashMap<RoleOnServer>();
		var groupNotOnline = new RoleOnServer(); // LinkName is Empty And Socket is null.
		groups.put(-1, groupNotOnline);

		for (var roleId : roleIds) {
			var online = _tonline.get(roleId);
			if (online == null) {
				groupNotOnline.roles.add(roleId);
				continue;
			}

			var version = _tversion.getOrAdd(roleId);
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(version.getServerId());
			if (group == null) {
				group = new RoleOnServer();
				group.serverId = version.getServerId();
				groups.put(group.serverId, group);
			}
			group.roles.add(roleId);
		}
		return groups;
	}

	private static @NotNull RoleOnServer merge(@Nullable RoleOnServer current, @NotNull RoleOnServer m) {
		if (null == current)
			return m;
		current.roles.addAll(m.roles);
		return current;
	}

	private void transmitInProcedure(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
									 @Nullable Binary parameter) {
		if (providerApp.zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isEmpty()) {
			// 没有启用cache-sync，马上触发本地任务。
			processTransmit(sender, actionName, roleIds, parameter);
			return;
		}

		var groups = groupByServerId(roleIds);
		RoleOnServer groupLocal = null;
		for (var it = groups.iterator(); it.moveToNext(); ) {
			var group = it.value();
			if (group.serverId == -1 || group.serverId == providerApp.zeze.getConfig().getServerId()) {
				// loopback 就是当前gs.
				groupLocal = merge(groupLocal, group);
				continue;
			}
			var transmit = new Transmit();
			transmit.Argument.setActionName(actionName);
			transmit.Argument.setSender(sender);
			transmit.Argument.getRoles().addAll(group.roles);
			if (parameter != null) {
				transmit.Argument.setParameter(parameter);
			}
			var ps = providerApp.providerDirectService.providerByServerId.get(group.serverId);
			if (null == ps) {
				assert groupLocal != null;
				groupLocal.roles.addAll(group.roles);
				continue;
			}
			var socket = providerApp.providerDirectService.GetSocket(ps.getSessionId());
			if (null == socket) {
				assert groupLocal != null;
				groupLocal.roles.addAll(group.roles);
				continue;
			}
			transmit.Send(socket);
		}
		if (groupLocal != null && !groupLocal.roles.isEmpty())
			processTransmit(sender, actionName, groupLocal.roles, parameter);
	}

	public void transmit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds) {
		transmit(sender, actionName, roleIds, null);
	}

	public void transmit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
						 @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		ByteBuffer bb;
		if (parameter != null) {
			int preSize = parameter.preAllocSize();
			bb = ByteBuffer.Allocate(preSize);
			parameter.encode(bb);
			if (bb.WriteIndex > preSize)
				parameter.preAllocSize(bb.WriteIndex);
		} else
			bb = null;
		// 发送协议请求在另外的事务中执行。
		Task.run(providerApp.zeze.newProcedure(() -> {
			transmitInProcedure(sender, actionName, roleIds, bb != null ? new Binary(bb) : null);
			return Procedure.Success;
		}, "Online.transmit"), null, null, DispatchMode.Normal);
	}

	public void transmitWhileCommit(long sender, @NotNull String actionName, long roleId) {
		transmitWhileCommit(sender, actionName, roleId, null);
	}

	public void transmitWhileCommit(long sender, @NotNull String actionName, long roleId, @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(sender, actionName, roleId, parameter));
	}

	public void transmitWhileCommit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds) {
		transmitWhileCommit(sender, actionName, roleIds, null);
	}

	public void transmitWhileCommit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
									@Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(sender, actionName, roleIds, parameter));
	}

	public void transmitWhileRollback(long sender, @NotNull String actionName, long roleId) {
		transmitWhileRollback(sender, actionName, roleId, null);
	}

	public void transmitWhileRollback(long sender, @NotNull String actionName, long roleId,
									  @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileRollback(() -> transmit(sender, actionName, roleId, parameter));
	}

	public void transmitWhileRollback(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds) {
		transmitWhileRollback(sender, actionName, roleIds, null);
	}

	public void transmitWhileRollback(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
									  @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileRollback(() -> transmit(sender, actionName, roleIds, parameter));
	}

	private void broadcast(long typeId, @NotNull Binary fullEncodedProtocol, int time) {
//		TaskCompletionSource<Long> future = null;
		var broadcast = new Broadcast(new BBroadcast(typeId, fullEncodedProtocol, time));
		for (var link : providerApp.providerService.getLinks().values()) {
			if (link.getSocket() != null)
				link.getSocket().Send(broadcast);
		}

//		if (future != null)
//			future.await();
	}

	public void broadcast(@NotNull Protocol<?> p) {
		broadcast(p, 60 * 1000);
	}

	public void broadcast(@NotNull Protocol<?> p, int time) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Broc", providerApp.providerService.getLinks().size(), p);
		broadcast(typeId, new Binary(p.encode()), time);
	}

	private void verifyLocal() {
		var roleId = new OutLong();
		_tlocal.walkCache((k, v) -> {
			// 先得到roleId
			roleId.value = k;
			return true;
		}, () -> {
			// 锁外执行事务
			try {
				providerApp.zeze.newProcedure(() -> {
					tryRemoveLocal(roleId.value);
					return 0L;
				}, "Online.verifyLocal:" + roleId.value).call();
			} catch (Exception e) {
				logger.error("", e);
			}
		});
		// 随机开始时间，避免验证操作过于集中。3:10 - 5:10
		verifyLocalTimer = Task.scheduleAtUnsafe(3 + Random.getInstance().nextInt(3), 10, this::verifyLocal);
	}

	private long tryRemoveLocal(long roleId) throws Exception {
		var online = _tonline.get(roleId);
		var local = _tlocal.get(roleId);

		if (null == local)
			return 0;
		// null == online && null == local -> do nothing
		// null != online && null == local -> do nothing

		var version = _tversion.getOrAdd(roleId);
		if ((null == online) || (version.getLoginVersion() != local.getLoginVersion()))
			return removeLocalAndTrigger(roleId);
		return 0;
	}

	@RedirectToServer
	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	protected void redirectRemoveLocal(int serverId, long roleId) {
		providerApp.zeze.newProcedure(() -> tryRemoveLocal(roleId), "Online.redirectRemoveLocal").call();
	}

	private void tryRedirectRemoveLocal(int serverId, long roleId) {
		if (providerApp.zeze.getConfig().getServerId() != serverId
				&& providerApp.providerDirectService.providerByServerId.containsKey(serverId))
			redirectRemoveLocal(serverId, roleId);
	}

	@Override
	protected long ProcessLoginRequest(Zeze.Builtin.Game.Online.Login rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			setUserState.Argument.setContext(String.valueOf(rpc.Argument.getRoleId()));
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		var online = _tonline.getOrAdd(rpc.Argument.getRoleId());
		var link = online.getLink();

		if (!link.getLinkName().equals(session.getLinkName()) || link.getLinkSid() != session.getLinkSid()) {
			providerApp.providerService.kick(link.getLinkName(), link.getLinkSid(),
					BKick.ErrorDuplicateLogin, "duplicate role login");
		}

		var local = _tlocal.getOrAdd(rpc.Argument.getRoleId());
		var version = _tversion.getOrAdd(rpc.Argument.getRoleId());

		// login exist
		if (assignLogoutVersion(version)) {
			var ret = logoutTrigger(rpc.Argument.getRoleId(), LogoutReason.LOGIN);
			if (0 != ret)
				return ret;
			// trigger remove; new record
			online = _tonline.getOrAdd(rpc.Argument.getRoleId());
			local = _tlocal.getOrAdd(rpc.Argument.getRoleId());
			link = online.getLink();
		}

		var loginVersion = version.getLoginVersion() + 1;
		version.setLoginVersion(loginVersion);
		local.setLoginVersion(loginVersion);

		if (link.getLinkSid() != session.getLinkSid() || !link.getLinkName().equals(session.getLinkName()))
			online.setLink(new BLink(session.getLinkName(), session.getLinkSid()));

		version.getReliableNotifyMark().clear();
		openQueue(rpc.Argument.getRoleId()).clear();
		version.setReliableNotifyConfirmIndex(0);
		version.setReliableNotifyIndex(0);

		// var linkSession = (ProviderService.LinkSession)session.getLink().getUserState();
		version.setServerId(providerApp.zeze.getConfig().getServerId());

		var ret = loginTrigger(session.getAccount(), rpc.Argument.getRoleId());
		if (0 != ret)
			return ret;

		// 先提交结果再设置状态。
		// see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
		session.sendResponseWhileCommit(rpc);
		return Procedure.Success;
	}

	@Override
	protected long ProcessReLoginRequest(Zeze.Builtin.Game.Online.ReLogin rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			setUserState.Argument.setContext(String.valueOf(rpc.Argument.getRoleId()));
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		var online = _tonline.getOrAdd(rpc.Argument.getRoleId());
		var link = online.getLink();

		if (!link.getLinkName().equals(session.getLinkName()) || link.getLinkSid() != session.getLinkSid()) {
			providerApp.providerService.kick(link.getLinkName(), link.getLinkSid(),
					BKick.ErrorDuplicateLogin, "duplicate role login");
		}

		var local = _tlocal.getOrAdd(rpc.Argument.getRoleId());
		var version = _tversion.getOrAdd(rpc.Argument.getRoleId());

		// login exist
		if (assignLogoutVersion(version)) {
			var ret = logoutTrigger(rpc.Argument.getRoleId(), LogoutReason.RE_LOGIN);
			if (0 != ret)
				return ret;
			// trigger remove; new record
			online = _tonline.getOrAdd(rpc.Argument.getRoleId());
			local = _tlocal.getOrAdd(rpc.Argument.getRoleId());
		}

		var loginVersion = version.getLoginVersion() + 1;
		version.setLoginVersion(loginVersion);
		local.setLoginVersion(loginVersion);

		if (link.getLinkSid() != session.getLinkSid() || !link.getLinkName().equals(session.getLinkName()))
			online.setLink(new BLink(session.getLinkName(), session.getLinkSid()));

		var ret = reloginTrigger(session.getAccount(), rpc.Argument.getRoleId());
		if (0 != ret)
			return ret;

		// 先发结果，再发送同步数据（ReliableNotifySync）。
		// 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
		session.sendResponseWhileCommit(rpc);

		var syncResultCode = reliableNotifySync(rpc.Argument.getRoleId(), session,
				rpc.Argument.getReliableNotifyConfirmIndex());
		if (syncResultCode != ResultCodeSuccess)
			return errorCode(syncResultCode);

		return Procedure.Success;
	}

	@Override
	protected long ProcessLogoutRequest(Zeze.Builtin.Game.Online.Logout rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);
		if (session.getRoleId() == null)
			return errorCode(ResultCodeNotLogin);

		//var local = _tlocal.get(session.getRoleId());
		var online = _tonline.get(session.getRoleId());
		var version = _tversion.getOrAdd(session.getRoleId());

		if (null != online) {
			if (assignLogoutVersion(version)) {
				var ret = logoutTrigger(session.getRoleId(), LogoutReason.LOGOUT);
				if (0 != ret)
					return ret;
				// 到这里online被删除了。
			}
		}
		// 先设置状态，再发送Logout结果。
		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		session.sendResponseWhileCommit(rpc);
		// 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
		// App.Load.LogoutCount.IncrementAndGet();
		return Procedure.Success;
	}

	private int reliableNotifySync(long roleId, ProviderUserSession session, long reliableNotifyConfirmCount) {
		return reliableNotifySync(roleId, session, reliableNotifyConfirmCount, true);
	}

	private int reliableNotifySync(long roleId, ProviderUserSession session, long index, boolean sync) {
		var version = _tversion.getOrAdd(roleId);
		var queue = openQueue(roleId);
		if (index < version.getReliableNotifyConfirmIndex()
				|| index > version.getReliableNotifyIndex()
				|| index - version.getReliableNotifyConfirmIndex() > queue.size()) {
			return ResultCodeReliableNotifyConfirmIndexOutOfRange;
		}

		int confirmCount = (int)(index - version.getReliableNotifyConfirmIndex());
		for (int i = 0; i < confirmCount; i++)
			queue.poll();
		if (sync) {
			var notify = new SReliableNotify(new BReliableNotify(index));
			queue.walk((nodeId, bNotify) -> {
				notify.Argument.getNotifies().add(bNotify.getFullEncodedProtocol());
				return true;
			});
			session.sendResponseWhileCommit(notify);
		}
		//online.getReliableNotifyQueue().RemoveRange(0, confirmCount);
		version.setReliableNotifyConfirmIndex(index);
		return ResultCodeSuccess;
	}

	@Override
	protected long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Game.Online.ReliableNotifyConfirm rpc) {
		var session = ProviderUserSession.get(rpc);

		var online = _tonline.get(session.getRoleId());
		if (online == null)
			return errorCode(ResultCodeOnlineDataNotFound);

		session.sendResponseWhileCommit(rpc); // 同步前提交。

		var syncResultCode = reliableNotifySync(session.getRoleId(), session,
				rpc.Argument.getReliableNotifyConfirmIndex(), rpc.Argument.isSync());
		if (syncResultCode != ResultCodeSuccess)
			return errorCode(syncResultCode);

		return Procedure.Success;
	}
}
