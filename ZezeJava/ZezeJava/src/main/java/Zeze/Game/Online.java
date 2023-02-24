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
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderService;
import Zeze.Arch.ProviderUserSession;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.Game.Online.BAny;
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
import Zeze.Util.LongList;
import Zeze.Util.OutLong;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Online extends AbstractOnline {
	protected static final Logger logger = LogManager.getLogger(Online.class);

	public final ProviderApp providerApp;
	private final ProviderLoad load;
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
		long call(long sender, long target, Binary parameter);
	}

	private final ConcurrentHashMap<String, TransmitAction> transmitActions = new ConcurrentHashMap<>();
	private Future<?> verifyLocalTimer;

	public static Online create(AppBase app) {
		return GenModule.createRedirectModule(Online.class, app);
	}

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}

	public ProviderLoad getLoad() {
		return load;
	}

	public static final BeanFactory beanFactory = new BeanFactory();

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static void register(Class<? extends Bean> cls) {
		beanFactory.register(cls);
	}

	protected Online(AppBase app) {
		providerApp = app.getZeze().redirect.providerApp;
		RegisterProtocols(providerApp.providerService);
		RegisterZezeTables(providerApp.zeze);
		load = new ProviderLoad(this);
	}

	public void start() {
		load.start();
		verifyLocalTimer = Task.scheduleAtUnsafe(3 + Random.getInstance().nextInt(3), 10, this::verifyLocal);
		providerApp.builtinModules.put(this.getFullName(), this);
	}

	public void stop() {
		load.stop();
		if (null != verifyLocalTimer)
			verifyLocalTimer.cancel(false);
	}

	@Override
	public void UnRegister() {
		if (null != providerApp) {
			UnRegisterProtocols(providerApp.providerService);
			UnRegisterZezeTables(providerApp.zeze);
		}
	}

	// 用户数据
	@SuppressWarnings("unchecked")
	public <T extends Bean> T getUserData(long roleId) {
		var version = _tversion.get(roleId);
		if (null == version)
			return null;
		return (T)version.getUserData().getBean();
	}

	// 在线状态
	public BOnline getOnline(long roleId) {
		return _tonline.get(roleId);
	}

	// 在线异变数据
	public BVersion getData(long roleId) {
		return _tversion.get(roleId);
	}

	public <T extends Bean> void setUserData(long roleId, T data) {
		_tversion.getOrAdd(roleId).getUserData().setBean(data);
	}

	public int getLocalCount() {
		return _tlocal.getCacheSize();
	}

	public long walkLocal(TableWalkHandle<Long, BLocal> walker) {
		return _tlocal.walkCache(walker);
	}

	public long getLoginTimes() {
		return loginTimes.get();
	}

	// 登录事件
	public EventDispatcher getLoginEvents() {
		return loginEvents;
	}

	public EventDispatcher getLinkBrokenEvents() {
		return linkBrokenEvents;
	}

	// 断线重连事件
	public EventDispatcher getReloginEvents() {
		return reloginEvents;
	}

	// 登出事件
	public EventDispatcher getLogoutEvents() {
		return logoutEvents;
	}

	// 角色从本机删除事件
	public EventDispatcher getLocalRemoveEvents() {
		return localRemoveEvents;
	}

	public final ConcurrentHashMap<String, TransmitAction> getTransmitActions() {
		return transmitActions;
	}

	public <T extends Bean> void setLocalBean(long roleId, String key, T bean) {
		var bLocal = _tlocal.get(roleId);
		if (null == bLocal)
			throw new IllegalStateException("roleId not online. " + roleId);
		beanFactory.register(bean);
		var bAny = new BAny();
		bAny.getAny().setBean(bean);
		bLocal.getDatas().put(key, bAny);
	}

	public void removeLocalBean(long roleId, String key) {
		var bLocal = _tlocal.get(roleId);
		if (null == bLocal)
			return;
		bLocal.getDatas().remove(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> T getLocalBean(long roleId, String key) {
		var bLocal = _tlocal.get(roleId);
		if (null == bLocal)
			return null;
		var data = bLocal.getDatas().get(key);
		if (null == data)
			return null;
		return (T)data.getAny().getBean();
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> T getOrAddLocalBean(long roleId, String key, T defaultHint) {
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

	public Long getLogoutVersion(long roleId) {
		/*
		if (_tonline.get(roleId) != null)
			return null; // is online
		*/
		var version = _tversion.get(roleId);
		if (null == version)
			return null; // no version
		return version.getLogoutVersion();
	}

	public Long getLoginVersion(long roleId) {
		var version = _tversion.get(roleId);
		if (null == version)
			return null; // no version
		return version.getLoginVersion();
	}

	public Long getLocalLoginVersion(long roleId) {
		var local = _tlocal.get(roleId);
		if (null == local)
			return null;
		return local.getLoginVersion();
	}

	public Long getGlobalLoginVersion(long roleId) {
		var version = _tversion.get(roleId);
		if (null == version)
			return null;
		return version.getLoginVersion();
	}

	public boolean isLogin(long roleId) {
		return null != _tonline.get(roleId);
	}

	private static boolean assignLogoutVersion(BVersion version) {
		if (version.getLoginVersion() == version.getLogoutVersion()) {
			return false;
		}
		version.setLogoutVersion(version.getLoginVersion());
		return true;
	}

	private long logoutTrigger(long roleId, LogoutReason logoutReason) throws Exception {
		var arg = new LogoutEventArgument();
		arg.roleId = roleId;
		arg.logoutReason = logoutReason;

		var version = _tversion.get(roleId);

		// 提前删除，可能事件里面需要使用这个判断已经登出。
		// 外面补发logoutTrigger之后需要重新getOrAdd一次。
		_tonline.remove(roleId); // remove first

		// 总是尝试通知上一次登录的服务器，里面会忽略本机。
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
	private long linkBrokenTrigger(@SuppressWarnings("unused") String account, long roleId) throws Exception {
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

	private long loginTrigger(String account, long roleId) throws Exception {
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

	private long reloginTrigger(String account, long roleId) throws Exception {
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

	public final long linkBroken(String account, long roleId, String linkName, long linkSid) throws Exception {
		long currentLoginVersion;
		{
			var online = _tonline.get(roleId);
			// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
			if (online == null || !online.getLinkName().equals(linkName) || online.getLinkSid() != linkSid)
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
		Transaction.whileCommit(() -> {
			// delay for real logout
			Task.schedule(providerApp.zeze.getConfig().getOnlineLogoutDelay(), () ->
					providerApp.zeze.newProcedure(() -> {
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
					}, "Game.Online.onLinkBroken").call());
		});
		return 0;
	}

	public final void send(long roleId, Protocol<?> p) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var log = AsyncSocket.logger;
			var level = AsyncSocket.PROTOCOL_LOG_LEVEL;
			var className = p.getClass().getSimpleName();
			if (p instanceof Rpc) {
				var rpc = ((Rpc<?, ?>)p);
				var rpcSessionId = rpc.getSessionId();
				if (rpc.isRequest())
					log.log(level, "Send:{} {}:{} {}", roleId, className, rpcSessionId, p.Argument);
				else {
					log.log(level, "Send:{} {}:{}>{} {}", roleId, className, rpcSessionId,
							p.getResultCode(), rpc.Result);
				}
			} else if (p.getResultCode() == 0)
				log.log(level, "Send:{} {} {}", roleId, className, p.Argument);
			else
				log.log(level, "Send:{} {}>{} {}", roleId, className, p.getResultCode(), p.Argument);
		}
		send(roleId, typeId, new Binary(p.encode()));
	}

	public final void send(Collection<Long> roleIds, Protocol<?> p) {
		if (roleIds.size() <= 0)
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
			var log = AsyncSocket.logger;
			var level = AsyncSocket.PROTOCOL_LOG_LEVEL;
			var className = p.getClass().getSimpleName();
			if (p instanceof Rpc) {
				var rpc = ((Rpc<?, ?>)p);
				var rpcSessionId = rpc.getSessionId();
				if (rpc.isRequest())
					log.log(level, "Send:{} {}:{} {}", idsStr, className, rpcSessionId, p.Argument);
				else {
					log.log(level, "Send:{} {}:{}>{} {}", idsStr, className, rpcSessionId,
							p.getResultCode(), rpc.Result);
				}
			} else if (p.getResultCode() == 0)
				log.log(level, "Send:{} {} {}", idsStr, className, p.Argument);
			else
				log.log(level, "Send:{} {}>{} {}", idsStr, className, p.getResultCode(), p.Argument);
		}
		send(roleIds, typeId, new Binary(p.encode()));
	}

	public final void sendWhileCommit(long roleId, Protocol<?> p) {
		Transaction.whileCommit(() -> send(roleId, p));
	}

	public final void sendWhileCommit(Collection<Long> roleIds, Protocol<?> p) {
		Transaction.whileCommit(() -> send(roleIds, p));
	}

	public final void sendWhileRollback(long roleId, Protocol<?> p) {
		Transaction.whileRollback(() -> send(roleId, p));
	}

	public final void sendWhileRollback(Collection<Long> roleIds, Protocol<?> p) {
		Transaction.whileRollback(() -> send(roleIds, p));
	}

	public void send(long roleId, long typeId, Binary fullEncodedProtocol) {
		// 发送协议请求在另外的事务中执行。
		providerApp.zeze.getTaskOneByOneByKey().Execute(roleId, () -> Task.call(providerApp.zeze.newProcedure(() -> {
			sendEmbed(List.of(roleId), typeId, fullEncodedProtocol);
			return Procedure.Success;
		}, "Game.Online.send"), null, null), DispatchMode.Normal);
	}

	@SuppressWarnings("unused")
	public void send(Collection<Long> roles, long typeId, Binary fullEncodedProtocol) {
		if (roles.size() > 0) {
			providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(roles, providerApp.zeze.newProcedure(() -> {
				sendEmbed(roles, typeId, fullEncodedProtocol);
				return Procedure.Success;
			}, "Game.Online.send"), null, DispatchMode.Normal);
		}
	}

	public void send(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
		// 发送协议请求在另外的事务中执行。
		Task.run(providerApp.zeze.newProcedure(() -> {
			sendEmbed(roleIds, typeId, fullEncodedProtocol);
			return Procedure.Success;
		}, "Game.Online.send"), null, null, DispatchMode.Normal);
	}

	private long triggerLinkBroken(String linkName, LongList errorSids, Map<Long, Long> context) {
		errorSids.foreach(sid -> providerApp.zeze.newProcedure(() -> {
			var roleId = context.get(sid);
			// 补发的linkBroken没有account上下文。
			return roleId != null ? linkBroken("", roleId, linkName, sid) : 0;
		}, "triggerLinkBroken").call());
		return 0;
	}

	public void send(AsyncSocket to, Map<Long, Long> contexts, Send send) {
		send.Send(to, rpc -> triggerLinkBroken(ProviderService.getLinkName(to),
				send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
	}

	public void sendEmbed(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
		// 发送消息为了用上TaskOneByOne，只能一个一个发送，为了少改代码，先使用旧的GroupByLink接口。
		var groups = groupByLink(roleIds);
		Transaction.whileCommit(() -> {
			for (var group : groups) {
				if (group.linkSocket == null)
					continue; // skip not online

				var send = new Send(new Zeze.Arch.Beans.BSend(typeId, fullEncodedProtocol));
				send.Argument.getLinkSids().addAll(group.roles.values());
				send(group.linkSocket, group.contexts, send);
			}
		});
	}

	public static final class RoleOnLink {
		String linkName = "";
		AsyncSocket linkSocket;
		int serverId = -1;
		// long providerSessionId;
		final HashMap<Long, Long> roles = new HashMap<>(); // roleid -> linksid
		final HashMap<Long, Long> contexts = new HashMap<>(); // linksid -> roleid
	}

	public final Collection<RoleOnLink> groupByLink(Iterable<Long> roleIds) {
		var groups = new HashMap<String, RoleOnLink>();
		var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.linkName, groupNotOnline);

		for (var roleId : roleIds) {
			var online = _tonline.get(roleId);
			if (online == null) {
				groupNotOnline.roles.putIfAbsent(roleId, null);
				continue;
			}

			var connector = providerApp.providerService.getLinks().get(online.getLinkName());
			if (connector == null) {
				groupNotOnline.roles.putIfAbsent(roleId, null);
				continue;
			}
			if (!connector.isHandshakeDone()) {
				groupNotOnline.roles.putIfAbsent(roleId, null);
				continue;
			}
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(online.getLinkName());
			var version = _tversion.getOrAdd(roleId);
			if (group == null) {
				group = new RoleOnLink();
				group.linkName = online.getLinkName();
				group.linkSocket = connector.getSocket();
				group.serverId = version.getServerId();
				groups.put(group.linkName, group);
			}
			group.roles.putIfAbsent(roleId, online.getLinkSid());
			group.contexts.putIfAbsent(online.getLinkSid(), roleId);
		}
		return groups.values();
	}

	public final void addReliableNotifyMark(long roleId, String listenerName) {
		var online = _tonline.get(roleId);
		if (online == null)
			throw new IllegalStateException("Not Online. AddReliableNotifyMark: " + listenerName);
		var version = _tversion.getOrAdd(roleId);
		version.getReliableNotifyMark().add(listenerName);
	}

	public final void removeReliableNotifyMark(long roleId, String listenerName) {
		// 移除尽量通过，不做任何判断。
		var version = _tversion.getOrAdd(roleId);
		version.getReliableNotifyMark().remove(listenerName);
	}

	public final void sendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol<?> p) {
		Transaction.whileCommit(() -> sendReliableNotify(roleId, listenerName, p));
	}

	public final void sendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId,
													Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
	}

	public final void sendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol<?> p) {
		Transaction.whileRollback(() -> sendReliableNotify(roleId, listenerName, p));
	}

	public final void sendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId,
													  Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
	}

	public final void sendReliableNotify(long roleId, String listenerName, Protocol<?> p) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var log = AsyncSocket.logger;
			var level = AsyncSocket.PROTOCOL_LOG_LEVEL;
			var className = p.getClass().getSimpleName();
			if (p instanceof Rpc) {
				var rpc = ((Rpc<?, ?>)p);
				var rpcSessionId = rpc.getSessionId();
				if (rpc.isRequest())
					log.log(level, "Send:{}:{} {}:{} {}", roleId, listenerName, className, rpcSessionId, p.Argument);
				else {
					log.log(level, "Send:{}:{} {}:{}>{} {}", roleId, listenerName, className, rpcSessionId,
							p.getResultCode(), rpc.Result);
				}
			} else if (p.getResultCode() == 0)
				log.log(level, "Send:{}:{} {} {}", roleId, listenerName, className, p.Argument);
			else
				log.log(level, "Send:{}:{} {}>{} {}", roleId, listenerName, className, p.getResultCode(), p.Argument);
		}
		sendReliableNotify(roleId, listenerName, typeId, new Binary(p.encode()));
	}

	private Zeze.Collections.Queue<BNotify> openQueue(long roleId) {
		return providerApp.zeze.getQueueModule().open("Zeze.Game.Online.ReliableNotifyQueue:" + roleId, BNotify.class);
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 */
	public final void sendReliableNotify(long roleId, String listenerName, long typeId, Binary fullEncodedProtocol) {
		providerApp.zeze.runTaskOneByOneByKey(listenerName, "Game.Online.sendReliableNotify." + listenerName, () -> {
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

			sendEmbed(List.of(roleId), notify.getTypeId(), new Binary(notify.encode()));
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
	public final void transmit(long sender, String actionName, long roleId, Serializable parameter) {
		transmit(sender, actionName, List.of(roleId), parameter);
	}

	public final void transmit(long sender, String actionName, long roleId) {
		transmit(sender, actionName, roleId, null);
	}

	public final void processTransmit(long sender, String actionName, Iterable<Long> roleIds, Binary parameter) {
		var handle = transmitActions.get(actionName);
		if (handle != null) {
			for (var target : roleIds) {
				Task.call(providerApp.zeze.newProcedure(() -> handle.call(sender, target, parameter),
						"Game.Online.transmit: " + actionName), null, null);
			}
		}
	}

	public static final class RoleOnServer {
		int serverId = -1; // providerId
		final HashSet<Long> roles = new HashSet<>();
	}

	public final IntHashMap<RoleOnServer> groupByServerId(Iterable<Long> roleIds) {
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

	private static RoleOnServer merge(RoleOnServer current, RoleOnServer m) {
		if (null == current)
			return m;
		current.roles.addAll(m.roles);
		return current;
	}

	private void transmitInProcedure(long sender, String actionName, Iterable<Long> roleIds, Binary parameter) {
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

	public final void transmit(long sender, String actionName, Iterable<Long> roleIds) {
		transmit(sender, actionName, roleIds, null);
	}

	public final void transmit(long sender, String actionName, Iterable<Long> roleIds, Serializable parameter) {
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
		}, "Game.Online.transmit"), null, null, DispatchMode.Normal);
	}

	public final void transmitWhileCommit(long sender, String actionName, long roleId) {
		transmitWhileCommit(sender, actionName, roleId, null);
	}

	public final void transmitWhileCommit(long sender, String actionName, long roleId, Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(sender, actionName, roleId, parameter));
	}

	public final void transmitWhileCommit(long sender, String actionName, Iterable<Long> roleIds) {
		transmitWhileCommit(sender, actionName, roleIds, null);
	}

	public final void transmitWhileCommit(long sender, String actionName, Iterable<Long> roleIds,
										  Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(sender, actionName, roleIds, parameter));
	}

	public final void transmitWhileRollback(long sender, String actionName, long roleId) {
		transmitWhileRollback(sender, actionName, roleId, null);
	}

	public final void transmitWhileRollback(long sender, String actionName, long roleId, Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileRollback(() -> transmit(sender, actionName, roleId, parameter));
	}

	public final void transmitWhileRollback(long sender, String actionName, Iterable<Long> roleIds) {
		transmitWhileRollback(sender, actionName, roleIds, null);
	}

	public final void transmitWhileRollback(long sender, String actionName, Iterable<Long> roleIds,
											Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileRollback(() -> transmit(sender, actionName, roleIds, parameter));
	}

	private void broadcast(long typeId, Binary fullEncodedProtocol, int time) {
//		TaskCompletionSource<Long> future = null;
		var broadcast = new Broadcast(new BBroadcast(typeId, fullEncodedProtocol, time));
		for (var link : providerApp.providerService.getLinks().values()) {
			if (link.getSocket() != null)
				link.getSocket().Send(broadcast);
		}

//		if (future != null)
//			future.await();
	}

	public final void broadcast(Protocol<?> p) {
		broadcast(p, 60 * 1000);
	}

	public final void broadcast(Protocol<?> p, int time) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var log = AsyncSocket.logger;
			var level = AsyncSocket.PROTOCOL_LOG_LEVEL;
			int linkCount = providerApp.providerService.getLinks().size();
			var className = p.getClass().getSimpleName();
			if (p instanceof Rpc) {
				var rpc = ((Rpc<?, ?>)p);
				var rpcSessionId = rpc.getSessionId();
				if (rpc.isRequest())
					log.log(level, "Send[{}] {}:{} {}", linkCount, className, rpcSessionId, p.Argument);
				else {
					log.log(level, "Send[{}] {}:{}>{} {}", linkCount, className, rpcSessionId,
							p.getResultCode(), rpc.Result);
				}
			} else if (p.getResultCode() == 0)
				log.log(level, "Send[{}] {} {}", linkCount, className, p.Argument);
			else
				log.log(level, "Send[{}] {}>{} {}", linkCount, className, p.getResultCode(), p.Argument);
		}
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
				}, "VerifyLocal:" + roleId.value).call();
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
		providerApp.zeze.newProcedure(() -> tryRemoveLocal(roleId), "redirectNotify").call();
	}

	private void tryRedirectRemoveLocal(int serverId, long roleId) {
		if (providerApp.zeze.getConfig().getServerId() != serverId
				&& providerApp.providerDirectService.providerByServerId.containsKey(serverId))
			redirectRemoveLocal(serverId, roleId);
	}

	@Override
	protected long ProcessLoginRequest(Zeze.Builtin.Game.Online.Login rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		var online = _tonline.getOrAdd(rpc.Argument.getRoleId());

		if (!online.getLinkName().equals(session.getLinkName()) || online.getLinkSid() != session.getLinkSid()) {
			providerApp.providerService.kick(online.getLinkName(), online.getLinkSid(),
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
		}

		var loginVersion = version.getLoginVersion() + 1;
		version.setLoginVersion(loginVersion);
		local.setLoginVersion(loginVersion);

		if (!online.getLinkName().equals(session.getLinkName()))
			online.setLinkName(session.getLinkName());
		if (online.getLinkSid() != session.getLinkSid())
			online.setLinkSid(session.getLinkSid());

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
		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			setUserState.Argument.setContext(String.valueOf(rpc.Argument.getRoleId()));
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		return Procedure.Success;
	}

	@Override
	protected long ProcessReLoginRequest(Zeze.Builtin.Game.Online.ReLogin rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		var online = _tonline.getOrAdd(rpc.Argument.getRoleId());

		if (!online.getLinkName().equals(session.getLinkName()) || online.getLinkSid() != session.getLinkSid()) {
			providerApp.providerService.kick(online.getLinkName(), online.getLinkSid(),
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

		if (!online.getLinkName().equals(session.getLinkName()))
			online.setLinkName(session.getLinkName());
		if (online.getLinkSid() != session.getLinkSid())
			online.setLinkSid(session.getLinkSid());

		var ret = reloginTrigger(session.getAccount(), rpc.Argument.getRoleId());
		if (0 != ret)
			return ret;

		// 先发结果，再发送同步数据（ReliableNotifySync）。
		// 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
		session.sendResponseWhileCommit(rpc);
		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			setUserState.Argument.setContext(String.valueOf(rpc.Argument.getRoleId()));
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

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

		//noinspection ConstantConditions
		var syncResultCode = reliableNotifySync(session.getRoleId(), session,
				rpc.Argument.getReliableNotifyConfirmIndex(), rpc.Argument.isSync());
		if (syncResultCode != ResultCodeSuccess)
			return errorCode(syncResultCode);

		return Procedure.Success;
	}
}
