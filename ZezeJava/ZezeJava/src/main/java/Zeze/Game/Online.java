package Zeze.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.AppBase;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderUserSession;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.Game.Online.BAccount;
import Zeze.Builtin.Game.Online.BAny;
import Zeze.Builtin.Game.Online.BLocal;
import Zeze.Builtin.Game.Online.BNotify;
import Zeze.Builtin.Game.Online.BOnline;
import Zeze.Builtin.Game.Online.SReliableNotify;
import Zeze.Builtin.Game.Online.taccount;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.BLinkBroken;
import Zeze.Builtin.Provider.Broadcast;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Builtin.ProviderDirect.Transmit;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.OutLong;
import Zeze.Util.Random;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Online extends AbstractOnline {
	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return bean.getTypeId();
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		throw new UnsupportedOperationException("Online Memory Table Dynamic Not Need.");
	}

	protected static final Logger logger = LogManager.getLogger(Online.class);

	public final ProviderApp ProviderApp;

	public int getLocalCount() {
		return _tlocal.getCacheSize();
	}

	public long walkLocal(TableWalkHandle<Long, BLocal> walker) {
		return _tlocal.WalkCache(walker);
	}

	public void setLocalBean(long roleId, String key, Bean bean) {
		var bLocal = _tlocal.get(roleId);
		if (null == bLocal)
			throw new RuntimeException("roleid not online. " + roleId);
		var bAny = new BAny();
		bAny.getAny().setBean(bean);
		bLocal.getDatas().put(key, bAny);
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

	private final Zeze.Util.EventDispatcher loginEvents = new EventDispatcher("Online.Login");
	private final Zeze.Util.EventDispatcher reloginEvents = new EventDispatcher("Online.Relogin");
	private final Zeze.Util.EventDispatcher logoutEvents = new EventDispatcher("Online.Logout");
	private final Zeze.Util.EventDispatcher localRemoveEvents = new EventDispatcher("Online.Local.Remove");

	private final AtomicLong LoginTimes = new AtomicLong();

	public long getLoginTimes() {
		return LoginTimes.get();
	}

	// 登录事件
	public Zeze.Util.EventDispatcher getLoginEvents() {
		return loginEvents;
	}

	// 断线重连事件
	public Zeze.Util.EventDispatcher getReloginEvents() {
		return reloginEvents;
	}

	// 登出事件
	public Zeze.Util.EventDispatcher getLogoutEvents() {
		return logoutEvents;
	}

	// 角色从本机删除事件
	public Zeze.Util.EventDispatcher getLocalRemoveEvents() {
		return localRemoveEvents;
	}

	public taccount getTableAccount() {
		return _taccount;
	}

	/**
	 * Func<sender, target, result>
	 * sender: 查询发起者，结果发送给他。
	 * target: 查询目标角色。
	 * result: 返回值，int，按普通事务处理过程返回值处理。
	 */
	private final ConcurrentHashMap<String, TransmitAction> transmitActions = new ConcurrentHashMap<>();
	public final LoadReporter LoadReporter;

	public Online(AppBase app) {
		if (app != null) {
			this.ProviderApp = app.getZeze().Redirect.ProviderApp;
			RegisterProtocols(ProviderApp.ProviderService);
			RegisterZezeTables(ProviderApp.Zeze);
		} else // for RedirectGenMain
			this.ProviderApp = null;

		LoadReporter = new LoadReporter(this);
	}

	private Future<?> VerifyLocalTimer;
	public void Start() {
		LoadReporter.Start();
		VerifyLocalTimer = Task.scheduleAt(3 + Random.getInstance().nextInt(3), 10, this::verifyLocal);
		ProviderApp.BuiltinModules.put(this.getFullName(), this);
	}

	public void Stop() {
		LoadReporter.Stop();
		if (null != VerifyLocalTimer)
			VerifyLocalTimer.cancel(false);	}

	@Override
	public void UnRegister() {
		UnRegisterProtocols(ProviderApp.ProviderService);
		UnRegisterZezeTables(ProviderApp.Zeze);
	}

	public final ConcurrentHashMap<String, TransmitAction> getTransmitActions() {
		return transmitActions;
	}

	public long addRole(String account, long roleId) {
		BAccount bAccount = _taccount.getOrAdd(account);
		if (bAccount.getName().isEmpty())
			bAccount.setName(account);
		if (!bAccount.getRoles().contains(roleId))
			bAccount.getRoles().add(roleId);
		return Procedure.Success;
	}

	public long removeRole(String account, long roleId) {
		BAccount bAccount = _taccount.get(account);
		if (bAccount == null)
			return ResultCodeAccountNotExist;
		if (!bAccount.getRoles().remove(roleId))
			return ResultCodeRoleNotExist;
		return Procedure.Success;
	}

	public long setLastLoginRoleId(String account, long roleId) {
		BAccount bAccount = _taccount.get(account);
		if (bAccount == null)
			return ResultCodeAccountNotExist;
		if (!bAccount.getRoles().contains(roleId))
			return ResultCodeRoleNotExist;
		bAccount.setLastLoginRoleId(roleId);
		return Procedure.Success;
	}

	private void removeLocalAndTrigger(long roleId) throws Throwable {
		var arg = new LocalRemoveEventArgument();
		arg.RoleId = roleId;
		arg.LocalData = _tlocal.get(roleId).Copy();

		_tlocal.remove(roleId); // remove first

		localRemoveEvents.triggerEmbed(this, arg);
		localRemoveEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(()->localRemoveEvents.triggerThread(this, arg));
	}

	private void removeOnlineAndTrigger(long roleId) throws Throwable {
		var arg = new LogoutEventArgument();
		arg.RoleId = roleId;
		arg.OnlineData = _tonline.get(roleId).Copy();

		_tonline.remove(roleId); // remove first

		logoutEvents.triggerEmbed(this, arg);
		logoutEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(()->logoutEvents.triggerThread(this, arg));
	}

	private void loginTrigger(long roleId) throws Throwable {
		var arg = new LoginArgument();
		arg.RoleId = roleId;

		LoginTimes.incrementAndGet();
		loginEvents.triggerEmbed(this, arg);
		loginEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> loginEvents.triggerThread(this, arg));
	}

	private void reloginTrigger(long roleId) throws Throwable {
		var arg = new LoginArgument();
		arg.RoleId = roleId;

		LoginTimes.incrementAndGet();
		reloginEvents.triggerEmbed(this, arg);
		reloginEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> reloginEvents.triggerThread(this, arg));
	}

	public final void onLinkBroken(long roleId, BLinkBroken arg) throws Throwable {
		long currentLoginVersion;
		{
			var online = _tonline.get(roleId);
			// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
			if (online == null || online.getLinkSid() != arg.getLinkSid())
				return;
			var version = _tversion.getOrAdd(roleId);
			var local = _tlocal.get(roleId);
			if (local == null)
				return; // 不在本机登录。

			currentLoginVersion = local.getLoginVersion();
			if (version.getLoginVersion() != currentLoginVersion)
				removeLocalAndTrigger(roleId); // 本机数据已经过时，马上删除。
		}

		final long currentLoginVersionFinal = currentLoginVersion;
		Transaction.getCurrent().RunWhileCommit(() -> {
			// delay 10 minutes
			Task.schedule(10 * 60 * 1000, () -> {
				ProviderApp.Zeze.NewProcedure(() -> {
					// local online 独立判断version分别尝试删除。
					var local = _tlocal.get(roleId);
					if (null != local && local.getLoginVersion() == currentLoginVersionFinal) {
						removeLocalAndTrigger(roleId);
					}
					// 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
					var online = _tonline.get(roleId);
					var version = _tversion.getOrAdd(roleId);
					if (null != online && version.getLoginVersion() == currentLoginVersionFinal) {
						removeOnlineAndTrigger(roleId);
					}
					return Procedure.Success;
				}, "Game.Online.onLinkBroken").Call();
			});
		});
	}

	public final void send(long roleId, Protocol<?> p) {
		send(roleId, p.getTypeId(), new Binary(p.Encode()));
	}

	public final void send(Iterable<Long> roleIds, Protocol<?> p) {
		send(roleIds, p.getTypeId(), new Binary(p.Encode()));
	}

	public final void sendAccount(String account, Protocol<?> p) {
		BAccount bAccount = _taccount.get(account);
		if (bAccount != null)
			send(new ArrayList<>(bAccount.getRoles()), p);
	}

	public final void sendWhileCommit(long roleId, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> send(roleId, p));
	}

	public final void sendWhileCommit(Iterable<Long> roleIds, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> send(roleIds, p));
	}

	public final void sendWhileRollback(long roleId, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> send(roleId, p));
	}

	public final void sendWhileRollback(Iterable<Long> roleIds, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> send(roleIds, p));
	}

	private void send(long roleId, long typeId, Binary fullEncodedProtocol) {
		// 发送协议请求在另外的事务中执行。
		ProviderApp.Zeze.getTaskOneByOneByKey().Execute(roleId, () -> Task.Call(ProviderApp.Zeze.NewProcedure(() -> {
			sendInProcedure(List.of(roleId), typeId, fullEncodedProtocol);
			return Procedure.Success;
		}, "Game.Online.send"), null, null));
	}

	@SuppressWarnings("unused")
	private void send(Collection<Long> roles, long typeId, Binary fullEncodedProtocol) {
		if (roles.size() > 0) {
			ProviderApp.Zeze.getTaskOneByOneByKey().ExecuteCyclicBarrier(roles,
					ProviderApp.Zeze.NewProcedure(() -> {
						sendInProcedure(roles, typeId, fullEncodedProtocol);
						return Procedure.Success;
			}, "Game.Online.send"), null);
		}
	}

	private void send(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
		// 发送协议请求在另外的事务中执行。
		Task.run(ProviderApp.Zeze.NewProcedure(() -> {
			sendInProcedure(roleIds, typeId, fullEncodedProtocol);
			return Procedure.Success;
		}, "Game.Online.send"));
	}

	private void sendInProcedure(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
		// 发送消息为了用上TaskOneByOne，只能一个一个发送，为了少改代码，先使用旧的GroupByLink接口。
		var groups = groupByLink(roleIds);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> {
			for (var group : groups) {
				if (group.linkSocket == null)
					continue; // skip not online

				var send = new Send();
				send.Argument.setProtocolType(typeId);
				send.Argument.setProtocolWholeData(fullEncodedProtocol);
				send.Argument.getLinkSids().addAll(group.roles.values());
				group.linkSocket.Send(send);
			}
		});
	}

	public static final class RoleOnLink {
		String linkName = "";
		AsyncSocket linkSocket;
		int serverId = -1;
//		long providerSessionId;
		final HashMap<Long, Long> roles = new HashMap<>();
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

			var connector = ProviderApp.ProviderService.getLinks().get(online.getLinkName());
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
		}
		return groups.values();
	}

	public final void addReliableNotifyMark(long roleId, String listenerName) {
		var online = _tonline.get(roleId);
		if (online == null)
			throw new RuntimeException("Not Online. AddReliableNotifyMark: " + listenerName);
		var version = _tversion.getOrAdd(roleId);
		version.getReliableNotifyMark().add(listenerName);
	}

	public final void removeReliableNotifyMark(long roleId, String listenerName) {
		// 移除尽量通过，不做任何判断。
		var version = _tversion.getOrAdd(roleId);
		version.getReliableNotifyMark().remove(listenerName);
	}

	public final void sendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> sendReliableNotify(roleId, listenerName, p));
	}

	public final void sendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
	}

	public final void sendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> sendReliableNotify(roleId, listenerName, p));
	}

	public final void sendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
	}

	public final void sendReliableNotify(long roleId, String listenerName, Protocol<?> p) {
		sendReliableNotify(roleId, listenerName, p.getTypeId(), new Binary(p.Encode()));
	}

	private Zeze.Collections.Queue<BNotify> openQueue(long roleId) {
		return ProviderApp.Zeze.getQueueModule().open("Zeze.Game.Online.ReliableNotifyQueue:" + roleId, BNotify.class);
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 */
	public final void sendReliableNotify(long roleId, String listenerName, long typeId, Binary fullEncodedProtocol) {
		ProviderApp.Zeze.getTaskOneByOneByKey().Execute(listenerName,
				ProviderApp.Zeze.NewProcedure(() -> {
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
					var notify = new SReliableNotify();
					notify.Argument.setReliableNotifyIndex(version.getReliableNotifyIndex());
					version.setReliableNotifyIndex(version.getReliableNotifyIndex() + 1); // after set notify.Argument
					notify.Argument.getNotifies().add(fullEncodedProtocol);

					sendInProcedure(List.of(roleId), notify.getTypeId(), new Binary(notify.Encode()));
					return Procedure.Success;
				}, "Game.Online.sendReliableNotify." + listenerName), null);
	}

	private void broadcast(long typeId, Binary fullEncodedProtocol, int time) {
//		TaskCompletionSource<Long> future = null;
		var broadcast = new Broadcast();
		broadcast.Argument.setProtocolType(typeId);
		broadcast.Argument.setProtocolWholeData(fullEncodedProtocol);
		broadcast.Argument.setTime(time);

		for (var link : ProviderApp.ProviderService.getLinks().values()) {
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
		broadcast(p.getTypeId(), new Binary(p.Encode()), time);
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
				Task.Call(ProviderApp.Zeze.NewProcedure(() -> handle.call(sender, target, parameter),
						"Game.Online.transmit: " + actionName), null, null);
			}
		}
	}

	public static final class RoleOnServer {
		int ServerId = -1; // ProviderId
		final HashSet<Long> roles = new HashSet<>();
	}

	public final Collection<RoleOnServer> groupByServerId(Iterable<Long> roleIds) {
		var groups = new HashMap<Integer, RoleOnServer>();
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
				group.ServerId = version.getServerId();
				groups.put(group.ServerId, group);
			}
			group.roles.add(roleId);
		}
		return groups.values();
	}

	private RoleOnServer merge(RoleOnServer current, RoleOnServer m) {
		if (null == current)
			return m;
		current.roles.addAll(m.roles);
		return current;
	}

	private void transmitInProcedure(long sender, String actionName, Iterable<Long> roleIds, Binary parameter) {
		if (ProviderApp.Zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isEmpty()) {
			// 没有启用cache-sync，马上触发本地任务。
			processTransmit(sender, actionName, roleIds, parameter);
			return;
		}

		var groups = groupByServerId(roleIds);
		RoleOnServer groupLocal = null;
		for (var group : groups) {
			if (group.ServerId == -1 || group.ServerId == ProviderApp.Zeze.getConfig().getServerId()) {
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
			var ps = ProviderApp.ProviderDirectService.ProviderByServerId.get(group.ServerId);
			if (null == ps) {
				assert groupLocal != null;
				groupLocal.roles.addAll(group.roles);
				continue;
			}
			var socket = ProviderApp.ProviderDirectService.GetSocket(ps.getSessionId());
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
			throw new RuntimeException("Unknown Action Name: " + actionName);
		ByteBuffer bb;
		if (parameter != null) {
			int preSize = parameter.getPreAllocSize();
			bb = ByteBuffer.Allocate(preSize);
			parameter.Encode(bb);
			if (bb.WriteIndex > preSize)
				parameter.setPreAllocSize(bb.WriteIndex);
		} else
			bb = null;
		// 发送协议请求在另外的事务中执行。
		Task.run(ProviderApp.Zeze.NewProcedure(() -> {
			transmitInProcedure(sender, actionName, roleIds, bb != null ? new Binary(bb) : null);
			return Procedure.Success;
		}, "Game.Online.transmit"), null, null);
	}

	public final void transmitWhileCommit(long sender, String actionName, long roleId) {
		transmitWhileCommit(sender, actionName, roleId, null);
	}

	public final void transmitWhileCommit(long sender, String actionName, long roleId, Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new RuntimeException("Unknown Action Name: " + actionName);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> transmit(sender, actionName, roleId, parameter));
	}

	public final void transmitWhileCommit(long sender, String actionName, Iterable<Long> roleIds) {
		transmitWhileCommit(sender, actionName, roleIds, null);
	}

	public final void transmitWhileCommit(long sender, String actionName, Iterable<Long> roleIds, Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new RuntimeException("Unknown Action Name: " + actionName);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> transmit(sender, actionName, roleIds, parameter));
	}

	public final void transmitWhileRollback(long sender, String actionName, long roleId) {
		transmitWhileRollback(sender, actionName, roleId, null);
	}

	public final void transmitWhileRollback(long sender, String actionName, long roleId, Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new RuntimeException("Unknown Action Name: " + actionName);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> transmit(sender, actionName, roleId, parameter));
	}

	public final void transmitWhileRollback(long sender, String actionName, Iterable<Long> roleIds) {
		transmitWhileRollback(sender, actionName, roleIds, null);
	}

	public final void transmitWhileRollback(long sender, String actionName, Iterable<Long> roleIds, Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new RuntimeException("Unknown Action Name: " + actionName);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> transmit(sender, actionName, roleIds, parameter));
	}

	private void verifyLocal() {
		var roleId = new OutLong();
		_tlocal.WalkCache(
				(k, v) -> {
					// 先得到roleId
					roleId.Value = k;
					return true;
					},
				() -> {
					// 锁外执行事务
					try {
						ProviderApp.Zeze.NewProcedure(() -> {
							tryRemoveLocal(roleId.Value);
							return 0L;
							}, "VerifyLocal:" + roleId.Value).Call();
					} catch (Throwable e) {
						logger.error(e);
					}
				});
		// 随机开始时间，避免验证操作过于集中。3:10 - 5:10
		VerifyLocalTimer = Task.scheduleAt(3 + Random.getInstance().nextInt(3), 10, this::verifyLocal);
	}

	private void tryRemoveLocal(long roleId) throws Throwable {
		var online = _tonline.get(roleId);
		var local = _tlocal.get(roleId);

		if (null == local)
			return;
		// null == online && null == local -> do nothing
		// null != online && null == local -> do nothing

		var version = _tversion.getOrAdd(roleId);
		if ((null == online) || (version.getLoginVersion() != local.getLoginVersion()))
			removeLocalAndTrigger(roleId);
	}

	@RedirectToServer
	protected RedirectFuture<Long> redirectNotify(int serverId, long roleId) throws Throwable {
		tryRemoveLocal(roleId);
		return RedirectFuture.finish(0L);
	}

	@Override
	protected long ProcessLoginRequest(Zeze.Builtin.Game.Online.Login rpc) throws Throwable {
		var session = ProviderUserSession.get(rpc);

		var account = _taccount.getOrAdd(session.getAccount());
		if (!account.getRoles().contains(rpc.Argument.getRoleId()))
			return ErrorCode(ResultCodeRoleNotExist);
		account.setLastLoginRoleId(rpc.Argument.getRoleId());

		var online = _tonline.getOrAdd(rpc.Argument.getRoleId());
		var local = _tlocal.getOrAdd(rpc.Argument.getRoleId());
		var version = _tversion.getOrAdd(rpc.Argument.getRoleId());
		// login exist && not local
		if (version.getLoginVersion() != 0 && version.getLoginVersion() != local.getLoginVersion()) {
			redirectNotify(version.getServerId(), rpc.Argument.getRoleId());
		}
		var loginVersion = account.getLastLoginVersion() + 1;
		account.setLastLoginVersion(loginVersion);
		version.setLoginVersion(loginVersion);
		local.setLoginVersion(loginVersion);

		if (!online.getLinkName().equals(session.getLinkName()) || online.getLinkSid() != session.getLinkSid()) {
			ProviderApp.ProviderService.kick(online.getLinkName(), online.getLinkSid(),
					BKick.ErrorDuplicateLogin, "duplicate role login");
		}
		if (!online.getLinkName().equals(session.getLinkName()))
			online.setLinkName(session.getLinkName());
		if (online.getLinkSid() != session.getLinkSid())
			online.setLinkSid(session.getLinkSid());

		version.getReliableNotifyMark().clear();
		openQueue(rpc.Argument.getRoleId()).clear();
		version.setReliableNotifyConfirmIndex(0);
		version.setReliableNotifyIndex(0);

		// var linkSession = (ProviderService.LinkSession)session.getLink().getUserState();
		version.setServerId(ProviderApp.Zeze.getConfig().getServerId());

		loginTrigger(rpc.Argument.getRoleId());

		// 先提交结果再设置状态。
		// see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
		session.sendResponseWhileCommit(rpc);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			setUserState.Argument.setContext(String.valueOf(rpc.Argument.getRoleId()));
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		return Procedure.Success;
	}

	@Override
	protected long ProcessReLoginRequest(Zeze.Builtin.Game.Online.ReLogin rpc) throws Throwable {
		var session = ProviderUserSession.get(rpc);

		BAccount account = _taccount.get(session.getAccount());
		if (account == null)
			return ErrorCode(ResultCodeAccountNotExist);
		if (account.getLastLoginRoleId() != rpc.Argument.getRoleId())
			return ErrorCode(ResultCodeNotLastLoginRoleId);

		BOnline online = _tonline.get(rpc.Argument.getRoleId());
		if (online == null)
			return ErrorCode(ResultCodeOnlineDataNotFound);

		var local = _tlocal.getOrAdd(rpc.Argument.getRoleId());
		var version = _tversion.getOrAdd(rpc.Argument.getRoleId());

		// login exist && not local
		if (version.getLoginVersion() != 0 && version.getLoginVersion() != local.getLoginVersion()) {
			redirectNotify(version.getServerId(), rpc.Argument.getRoleId());
		}
		var loginVersion = account.getLastLoginVersion() + 1;
		account.setLastLoginVersion(loginVersion);
		version.setLoginVersion(loginVersion);
		local.setLoginVersion(loginVersion);

		if (!online.getLinkName().equals(session.getLinkName()) || online.getLinkSid() != session.getLinkSid()) {
			ProviderApp.ProviderService.kick(online.getLinkName(), online.getLinkSid(),
					BKick.ErrorDuplicateLogin, "duplicate role login");
		}
		if (!online.getLinkName().equals(session.getLinkName()))
			online.setLinkName(session.getLinkName());
		if (online.getLinkSid() != session.getLinkSid())
			online.setLinkSid(session.getLinkSid());

		//noinspection ConstantConditions
		reloginTrigger(rpc.Argument.getRoleId());

		// 先发结果，再发送同步数据（ReliableNotifySync）。
		// 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
		session.sendResponseWhileCommit(rpc);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			setUserState.Argument.setContext(String.valueOf(rpc.Argument.getRoleId()));
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		var syncResultCode = reliableNotifySync(rpc.Argument.getRoleId(), session, rpc.Argument.getReliableNotifyConfirmIndex());
		if (syncResultCode != ResultCodeSuccess)
			return ErrorCode(syncResultCode);

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
			var notify = new SReliableNotify();
			notify.Argument.setReliableNotifyIndex(index);
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
	protected long ProcessLogoutRequest(Zeze.Builtin.Game.Online.Logout rpc) throws Throwable {
		var session = ProviderUserSession.get(rpc);
		if (session.getRoleId() == null)
			return ErrorCode(ResultCodeNotLogin);

		var local = _tlocal.get(session.getRoleId());
		var online = _tonline.get(session.getRoleId());
		var version = _tversion.getOrAdd(session.getRoleId());

		// 登录在其他机器上。
		if (local == null && online != null)
			redirectNotify(version.getServerId(), session.getRoleId());
		if (null != local)
			removeLocalAndTrigger(session.getRoleId());
		if (null != online)
			removeOnlineAndTrigger(session.getRoleId());

		// 先设置状态，再发送Logout结果。
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		session.sendResponseWhileCommit(rpc);
		// 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
		// App.Load.LogoutCount.IncrementAndGet();
		return Procedure.Success;
	}

	@Override
	protected long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Game.Online.ReliableNotifyConfirm rpc) {
		var session = ProviderUserSession.get(rpc);

		BOnline online = _tonline.get(session.getRoleId());
		if (online == null)
			return ErrorCode(ResultCodeOnlineDataNotFound);

		session.sendResponseWhileCommit(rpc); // 同步前提交。

		//noinspection ConstantConditions
		var syncResultCode = reliableNotifySync(session.getRoleId(), session,
				rpc.Argument.getReliableNotifyConfirmIndex(), rpc.Argument.isSync());
		if (syncResultCode != ResultCodeSuccess)
			return ErrorCode(syncResultCode);

		return Procedure.Success;
	}
}
