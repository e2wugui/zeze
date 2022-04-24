package Zeze.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderService;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Game.Online.BAccount;
import Zeze.Builtin.Game.Online.BOnline;
import Zeze.Builtin.Game.Online.SReliableNotify;
import Zeze.Builtin.Game.Online.taccount;
import Zeze.Builtin.Provider.BLinkBroken;
import Zeze.Builtin.Provider.Broadcast;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Builtin.ProviderDirect.BTransmitContext;
import Zeze.Builtin.ProviderDirect.Transmit;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Online extends AbstractOnline {
	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return bean.getTypeId();
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		throw new UnsupportedOperationException("Online Memory Table Dynamic Only.");
	}

	protected static final Logger logger = LogManager.getLogger(Online.class);

	private final ProviderApp app;

	@FunctionalInterface
	public interface TransmitAction {
		long call(long sender, long target, Binary parameter);
	}

	public int getLocalCount() {
		return _tlocal.getCacheSize();
	}

	public void walkLocal() {

	}

	/*
   a) 本机正常Login/Logout/LinkBroken
   b) 本机Logout/LinkBroken丢失或迟到，而已经在其他机器上Login，此时需要Redirect过来。
   c) Redirect丢失，本机需要一个机制遍历TableCache，慢慢检测并清除Memory中登录状态无效的数据。
	*/
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

	public Online(ProviderApp app) {
		this.app = app;
		RegisterProtocols(app.ProviderService);
		RegisterZezeTables(app.Zeze);
	}

	@Override
	public void UnRegister() {
		UnRegisterProtocols(app.ProviderService);
		UnRegisterZezeTables(app.Zeze);
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

	public final void onLinkBroken(long roleId, BLinkBroken arg) {
		var online = _tonline.get(roleId);
		if (online == null || online.getLinkSid() != arg.getLinkSid())
			return; // skip not owner

		// 设置状态，延迟以后，准备删除记录。
		online.setState(BOnline.StateNetBroken);

		Task.schedule(10 * 60 * 1000, () -> { // 10 minutes for relogin
			app.Zeze.NewProcedure(() -> {
				// 网络断开后延迟删除在线状态。这里简单判断一下是否StateNetBroken。
				// 由于CLogin,CReLogin的时候没有取消Timeout，所以有可能再次登录断线后，会被上一次断线的Timeout删除。
				// 造成延迟时间不准确。管理Timeout有点烦，先这样吧。
				var online2 = _tonline.get(roleId);
				// 删除前检查：
				// 1）State是网络断开（StateNetBroken），因为Delay期间可能重新连接并登录（ReLogin）；
				// 2）Owner：当前LinkSid是关闭连接，理由同上。
				if (online2 != null && online2.getState() == BOnline.StateNetBroken && online2.getLinkSid() == arg.getLinkSid())
					_tonline.remove(roleId);
				return Procedure.Success;
			}, "Game.Online.onLinkBroken").Call();
		});
	}

	public final void send(long roleId, Protocol<?> p) {
		send(roleId, p, false);
	}

	public final void send(long roleId, Protocol<?> p, boolean waitConfirm) {
		send(roleId, p.getTypeId(), new Binary(p.Encode()), waitConfirm);
	}

	// 广播不支持 waitConfirm
	public final void send(Iterable<Long> roleIds, Protocol<?> p) {
		send(roleIds, p.getTypeId(), new Binary(p.Encode()));
	}

	// 广播不支持 waitConfirm
	public final void sendAccount(String account, Protocol<?> p) {
		BAccount bAccount = _taccount.get(account);
		if (bAccount != null)
			send(new ArrayList<>(bAccount.getRoles()), p);
	}

	public final void sendWhileCommit(long roleId, Protocol<?> p) {
		sendWhileCommit(roleId, p, false);
	}

	public final void sendWhileCommit(long roleId, Protocol<?> p, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> send(roleId, p, WaitConfirm));
	}

	public final void sendWhileCommit(Iterable<Long> roleIds, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> send(roleIds, p));
	}

	public final void sendWhileRollback(long roleId, Protocol<?> p) {
		sendWhileRollback(roleId, p, false);
	}

	public final void sendWhileRollback(long roleId, Protocol<?> p, boolean waitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> send(roleId, p, waitConfirm));
	}

	public final void sendWhileRollback(Iterable<Long> roleIds, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> send(roleIds, p));
	}

	private void send(long roleId, long typeId, Binary fullEncodedProtocol, boolean waitConfirm) {
		var future = waitConfirm ? new TaskCompletionSource<Long>() : null;

		// 发送协议请求在另外的事务中执行。
		app.Zeze.getTaskOneByOneByKey().Execute(roleId, () -> Task.Call(app.Zeze.NewProcedure(() -> {
			sendInProcedure(List.of(roleId), typeId, fullEncodedProtocol, future);
			return Procedure.Success;
		}, "Game.Online.send"), null, null));

		if (future != null)
			future.await();
	}

	// 广播不支持 WaitConfirm
	private void send(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
		// 发送协议请求在另外的事务中执行。
		Task.run(app.Zeze.NewProcedure(() -> {
			sendInProcedure(roleIds, typeId, fullEncodedProtocol, null);
			return Procedure.Success;
		}, "Game.Online.send"));
	}

	private void sendInProcedure(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol, TaskCompletionSource<Long> future) {
		// 发送消息为了用上TaskOneByOne，只能一个一个发送，为了少改代码，先使用旧的GroupByLink接口。
		var groups = groupByLink(roleIds);
		long serialId = 0;
		if (future != null) {
			var confirmContext = new ConfirmContext(future);
			// 必须在真正发送前全部加入，否则要是发生结果很快返回，
			// 导致异步问题：错误的认为所有 Confirm 都收到。
			for (var group : groups) {
				if (group.linkSocket != null) // skip not online
					confirmContext.linkNames.add(group.linkName);
			}
			serialId = app.ProviderService.AddManualContextWithTimeout(confirmContext, 5000);
		}

		for (var group : groups) {
			if (group.linkSocket == null)
				continue; // skip not online

			var send = new Send();
			send.Argument.setProtocolType(typeId);
			send.Argument.setProtocolWholeData(fullEncodedProtocol);
			send.Argument.setConfirmSerialId(serialId);

			for (var ctx : group.roles.values())
				send.Argument.getLinkSids().add(ctx.getLinkSid());
			group.linkSocket.Send(send);
		}
	}

	public static final class RoleOnLink {
		String linkName = "";
		AsyncSocket linkSocket;
		int providerId = -1;
		long providerSessionId;
		final HashMap<Long, BTransmitContext> roles = new HashMap<>();
	}

	public final Collection<RoleOnLink> groupByLink(Iterable<Long> roleIds) {
		var groups = new HashMap<String, RoleOnLink>();
		var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.linkName, groupNotOnline);

		for (var roleId : roleIds) {
			var online = _tonline.get(roleId);
			if (online == null || online.getState() != BOnline.StateOnline) {
				groupNotOnline.roles.putIfAbsent(roleId, new BTransmitContext());
				continue;
			}

			var connector = app.ProviderService.getLinks().get(online.getLinkName());
			if (connector == null) {
				groupNotOnline.roles.putIfAbsent(roleId, new BTransmitContext());
				continue;
			}
			if (!connector.isHandshakeDone()) {
				groupNotOnline.roles.putIfAbsent(roleId, new BTransmitContext());
				continue;
			}
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(online.getLinkName());
			if (group == null) {
				group = new RoleOnLink();
				group.linkName = online.getLinkName();
				group.linkSocket = connector.getSocket();
				group.providerId = online.getProviderId();
				group.providerSessionId = online.getProviderSessionId();
				groups.put(group.linkName, group);
			}
			BTransmitContext tempVar = new BTransmitContext(); // 使用 TryAdd，忽略重复的 roleId。
			tempVar.setLinkSid(online.getLinkSid());
			tempVar.setProviderId(online.getProviderId());
			tempVar.setProviderSessionId(online.getProviderSessionId());
			group.roles.putIfAbsent(roleId, tempVar);
		}
		return groups.values();
	}

	public final class ConfirmContext extends Service.ManualContext {
		final HashSet<String> linkNames = new HashSet<>();
		final TaskCompletionSource<Long> future;

		public ConfirmContext(TaskCompletionSource<Long> future) {
			this.future = future;
		}

		@Override
		public void OnRemoved() {
			future.SetResult(super.getSessionId());
		}

		@SuppressWarnings("unused")
		public long processLinkConfirm(String linkName) {
			synchronized (this) {
				linkNames.remove(linkName);
				if (linkNames.isEmpty())
					app.ProviderService.<ConfirmContext>TryRemoveManualContext(getSessionId());
				return Procedure.Success;
			}
		}
	}

	public final void addReliableNotifyMark(long roleId, String listenerName) {
		var online = _tonline.get(roleId);
		if (online == null || online.getState() != BOnline.StateOnline)
			throw new RuntimeException("Not Online. AddReliableNotifyMark: " + listenerName);
		online.getReliableNotifyMark().add(listenerName);
	}

	public final void removeReliableNotifyMark(long roleId, String listenerName) {
		// 移除尽量通过，不做任何判断。
		if (_tonline.get(roleId) != null)
			_tonline.get(roleId).getReliableNotifyMark().remove(listenerName);
	}

	public final void sendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol<?> p) {
		sendReliableNotifyWhileCommit(roleId, listenerName, p, false);
	}

	public final void sendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol<?> p, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> sendReliableNotify(roleId, listenerName, p, WaitConfirm));
	}

	public final void sendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		sendReliableNotifyWhileCommit(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

	public final void sendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm));
	}

	public final void sendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol<?> p) {
		sendReliableNotifyWhileRollback(roleId, listenerName, p, false);
	}

	public final void sendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol<?> p, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> sendReliableNotify(roleId, listenerName, p, WaitConfirm));
	}

	public final void sendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		sendReliableNotifyWhileRollback(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

	public final void sendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm));
	}

	public final void sendReliableNotify(long roleId, String listenerName, Protocol<?> p) {
		sendReliableNotify(roleId, listenerName, p, false);
	}

	public final void sendReliableNotify(long roleId, String listenerName, Protocol<?> p, boolean WaitConfirm) {
		sendReliableNotify(roleId, listenerName, p.getTypeId(), new Binary(p.Encode()), WaitConfirm);
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 */

	public final void sendReliableNotify(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

	public final void sendReliableNotify(long roleId, String listenerName, long typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		var future = WaitConfirm ? new TaskCompletionSource<Long>() : null;

		app.Zeze.getTaskOneByOneByKey().Execute(listenerName,
				app.Zeze.NewProcedure(() -> {
					BOnline online = _tonline.get(roleId);
					if (online == null || online.getState() == BOnline.StateOffline) {
						return Procedure.Success;
					}
					if (!online.getReliableNotifyMark().contains(listenerName)) {
						return Procedure.Success; // 相关数据装载的时候要同步设置这个。
					}

					// 先保存在再发送，然后客户端还会确认。
					// see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
					online.getReliableNotifyQueue().add(fullEncodedProtocol);
					if (online.getState() == BOnline.StateOnline) {
						var notify = new SReliableNotify(); // 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
						notify.Argument.setReliableNotifyTotalCountStart(online.getReliableNotifyTotalCount());
						notify.Argument.getNotifies().add(fullEncodedProtocol);

						sendInProcedure(List.of(roleId), notify.getTypeId(), new Binary(notify.Encode()), future);
					}
					online.setReliableNotifyTotalCount(online.getReliableNotifyConfirmCount() + 1); // 后加，start 是 Queue.Add 之前的。
					return Procedure.Success;
				}, "Game.Online.sendReliableNotify." + listenerName), null);

		if (future != null)
			future.await();
	}

	private void broadcast(long typeId, Binary fullEncodedProtocol, int time, boolean WaitConfirm) {
		TaskCompletionSource<Long> future = null;
		long serialId = 0;
		if (WaitConfirm) {
			future = new TaskCompletionSource<>();
			var confirmContext = new ConfirmContext(future);
			for (var link : app.ProviderService.getLinks().values()) {
				if (link.getSocket() != null)
					confirmContext.linkNames.add(link.getName());
			}
			serialId = app.ProviderService.AddManualContextWithTimeout(confirmContext, 5000);
		}

		var broadcast = new Broadcast();
		broadcast.Argument.setProtocolType(typeId);
		broadcast.Argument.setProtocolWholeData(fullEncodedProtocol);
		broadcast.Argument.setConfirmSerialId(serialId);
		broadcast.Argument.setTime(time);

		for (var link : app.ProviderService.getLinks().values()) {
			if (link.getSocket() != null)
				link.getSocket().Send(broadcast);
		}

		if (future != null)
			future.await();
	}

	public final void broadcast(Protocol<?> p, int time) {
		broadcast(p, time, false);
	}

	public final void broadcast(Protocol<?> p) {
		broadcast(p, 60 * 1000, false);
	}

	public final void broadcast(Protocol<?> p, int time, boolean WaitConfirm) {
		broadcast(p.getTypeId(), new Binary(p.Encode()), time, WaitConfirm);
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
				Task.Call(app.Zeze.NewProcedure(() -> handle.call(sender, target, parameter),
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
			if (online == null || online.getState() != BOnline.StateOnline) {
				groupNotOnline.roles.add(roleId);
				continue;
			}

			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(online.getProviderId());
			if (group == null) {
				group = new RoleOnServer();
				group.ServerId = online.getProviderId();
				groups.put(online.getProviderId(), group);
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
		if (app.Zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isEmpty()) {
			// 没有启用cache-sync，马上触发本地任务。
			processTransmit(sender, actionName, roleIds, parameter);
			return;
		}

		var groups = groupByServerId(roleIds);
		RoleOnServer groupLocal = null;
		for (var group : groups) {
			if (group.ServerId == app.Zeze.getConfig().getServerId()) {
				// loopback 就是当前gs.
				groupLocal = merge(groupLocal, group);
				continue;
			}
			var transmit = new Transmit();
			transmit.Argument.setActionName(actionName);
			transmit.Argument.setSender(sender);
			transmit.Argument.setServiceNamePrefix(app.ServerServiceNamePrefix);
			//TODO transmit.Argument.getRoles().putAll(group.roles);
			if (parameter != null) {
				// not used
				// transmit.Argument.setParameterBeanName(parameter.getClass().getName());
				transmit.Argument.setParameterBeanValue(parameter);
			}
			//TODO app.Distribute.ChoiceProviderByServerId(app.ServerServiceNamePrefix, );
			//TODO group.linkSocket.Send(transmit);
		}
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
		Task.run(app.Zeze.NewProcedure(() -> {
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

	@Override
	protected long ProcessLoginRequest(Zeze.Builtin.Game.Online.Login rpc) {
		var session = ProviderUserSession.Get(rpc);

		BAccount account = _taccount.get(session.getAccount());
		if (account == null)
			return ErrorCode(ResultCodeAccountNotExist);
		account.setLastLoginRoleId(rpc.Argument.getRoleId());

		BOnline online = _tonline.getOrAdd(rpc.Argument.getRoleId());
		online.setLinkName(session.getLinkName());
		online.setLinkSid(session.getSessionId());
		online.setState(BOnline.StateOnline);

		online.getReliableNotifyMark().clear();
		online.getReliableNotifyQueue().clear();
		online.setReliableNotifyConfirmCount(0);
		online.setReliableNotifyTotalCount(0);

		var linkSession = (ProviderService.LinkSession)session.getLink().getUserState();
		online.setProviderId(app.Zeze.getConfig().getServerId());
		online.setProviderSessionId(linkSession.getProviderSessionId());

		// 先提交结果再设置状态。
		// see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
		session.SendResponseWhileCommit(rpc);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getSessionId());
			setUserState.Argument.getStates().add(rpc.Argument.getRoleId());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		return Procedure.Success;
	}

	@Override
	protected long ProcessReLoginRequest(Zeze.Builtin.Game.Online.ReLogin rpc) {
		var session = ProviderUserSession.Get(rpc);

		BAccount account = _taccount.get(session.getAccount());
		if (account == null)
			return ErrorCode(ResultCodeAccountNotExist);
		if (account.getLastLoginRoleId() != rpc.Argument.getRoleId())
			return ErrorCode(ResultCodeNotLastLoginRoleId);

		BOnline online = _tonline.get(rpc.Argument.getRoleId());
		if (online == null)
			return ErrorCode(ResultCodeOnlineDataNotFound);
		online.setLinkName(session.getLinkName());
		online.setLinkSid(session.getSessionId());
		online.setState(BOnline.StateOnline);

		// 先发结果，再发送同步数据（ReliableNotifySync）。
		// 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
		session.SendResponseWhileCommit(rpc);
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getSessionId());
			setUserState.Argument.getStates().add(rpc.Argument.getRoleId());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		var syncResultCode = reliableNotifySync(session, rpc.Argument.getReliableNotifyConfirmCount(), online);
		if (syncResultCode != ResultCodeSuccess)
			return ErrorCode(syncResultCode);

		return Procedure.Success;
	}

	private int reliableNotifySync(ProviderUserSession session, long reliableNotifyConfirmCount, BOnline online) {
		return reliableNotifySync(session, reliableNotifyConfirmCount, online, true);
	}

	private int reliableNotifySync(ProviderUserSession session, long reliableNotifyConfirmCount, BOnline online, boolean sync) {
		if (reliableNotifyConfirmCount < online.getReliableNotifyConfirmCount()
				|| reliableNotifyConfirmCount > online.getReliableNotifyTotalCount()
				|| reliableNotifyConfirmCount - online.getReliableNotifyConfirmCount() > online.getReliableNotifyQueue().size()) {
			return ResultCodeReliableNotifyConfirmCountOutOfRange;
		}

		int confirmCount = (int)(reliableNotifyConfirmCount - online.getReliableNotifyConfirmCount());

		if (sync) {
			var notify = new SReliableNotify();
			notify.Argument.setReliableNotifyTotalCountStart(reliableNotifyConfirmCount);
			for (int i = confirmCount; i < online.getReliableNotifyQueue().size(); i++)
				notify.Argument.getNotifies().add(online.getReliableNotifyQueue().get(i));
			session.SendResponseWhileCommit(notify);
		}
		//noinspection ListRemoveInLoop
		for (int ir = 0; ir < confirmCount; ir++)
			online.getReliableNotifyQueue().remove(0);
		//online.getReliableNotifyQueue().RemoveRange(0, confirmCount);
		online.setReliableNotifyConfirmCount(reliableNotifyConfirmCount);
		return ResultCodeSuccess;
	}

	@Override
	protected long ProcessLogoutRequest(Zeze.Builtin.Game.Online.Logout rpc) {
		var session = ProviderUserSession.Get(rpc);
		if (session.getRoleId() == null)
			return ErrorCode(ResultCodeNotLogin);

		_tonline.remove(session.getRoleId());

		// 先设置状态，再发送Logout结果。
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getSessionId());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		session.SendResponseWhileCommit(rpc);
		// 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
		// App.Load.LogoutCount.IncrementAndGet();
		return Procedure.Success;
	}

	@Override
	protected long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Game.Online.ReliableNotifyConfirm rpc) {
		var session = ProviderUserSession.Get(rpc);

		BOnline online = _tonline.get(session.getRoleId());
		if (online == null || online.getState() == BOnline.StateOffline)
			return ErrorCode(ResultCodeOnlineDataNotFound);

		session.SendResponseWhileCommit(rpc); // 同步前提交。

		var syncResultCode = reliableNotifySync(session, rpc.Argument.getReliableNotifyConfirmCount(), online, false);
		if (syncResultCode != ResultCodeSuccess)
			return ErrorCode(syncResultCode);

		return Procedure.Success;
	}
}
