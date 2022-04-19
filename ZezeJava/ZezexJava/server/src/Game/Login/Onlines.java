package Game.Login;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Game.App;
import Zeze.Beans.Provider.Broadcast;
import Zeze.Beans.Provider.Send;
import Zeze.Beans.ProviderDirect.BTransmitContext;
import Zeze.Beans.ProviderDirect.Transmit;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Func3;
import Zeze.Util.TaskCompletionSource;

public class Onlines {
	private final tonline table;

	public Onlines(tonline table) {
		this.table = table;
	}

	public final void OnLinkBroken(long roleId) {
		var online = table.get(roleId);
		if (online != null) {
			online.setState(BOnline.StateNetBroken);
		}

		Zeze.Util.Task.schedule(10 * 60 * 1000, () -> { // 10 minutes for relogin
			App.getInstance().Zeze.NewProcedure(() -> {
				// 网络断开后延迟删除在线状态。这里简单判断一下是否StateNetBroken。
				// 由于CLogin,CReLogin的时候没有取消Timeout，所以有可能再次登录断线后，会被上一次断线的Timeout删除。
				// 造成延迟时间不准确。管理Timeout有点烦，先这样吧。
				var online2 = table.get(roleId);
				if (online2 != null && online2.getState() == BOnline.StateNetBroken) {
					table.remove(roleId);
				}
				App.getInstance().getLoad().getLogoutCount().incrementAndGet();

				return Procedure.Success;
			}, "Onlines.OnLinkBroken").Call();
		});
	}

	public final void AddReliableNotifyMark(long roleId, String listenerName) {
		var online = table.get(roleId);
		if (online == null || online.getState() != BOnline.StateOnline) {
			throw new RuntimeException("Not Online. AddReliableNotifyMark: " + listenerName);
		}
		online.getReliableNotifyMark().add(listenerName);
	}

	public final void RemoveReliableNotifyMark(long roleId, String listenerName) {
		// 移除尽量通过，不做任何判断。
		if (table.get(roleId) != null) {
			table.get(roleId).getReliableNotifyMark().remove(listenerName);
		}
	}

	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol<?> p) {
		SendReliableNotifyWhileCommit(roleId, listenerName, p, false);
	}

	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol<?> p, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> SendReliableNotify(roleId, listenerName, p, WaitConfirm));
	}

	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		SendReliableNotifyWhileCommit(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm));
	}

	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol<?> p) {
		SendReliableNotifyWhileRollback(roleId, listenerName, p, false);
	}

	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol<?> p, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> SendReliableNotify(roleId, listenerName, p, WaitConfirm));
	}

	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		SendReliableNotifyWhileRollback(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm));
	}

	public final void SendReliableNotify(long roleId, String listenerName, Protocol<?> p) {
		SendReliableNotify(roleId, listenerName, p, false);
	}

	public final void SendReliableNotify(long roleId, String listenerName, Protocol<?> p, boolean WaitConfirm) {
		SendReliableNotify(roleId, listenerName, p.getTypeId(), new Binary(p.Encode()), WaitConfirm);
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 */

	public final void SendReliableNotify(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

	public final void SendReliableNotify(long roleId, String listenerName, long typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		final TaskCompletionSource<Long> future = WaitConfirm ? new TaskCompletionSource<>() : null;

		App.getInstance().Zeze.getTaskOneByOneByKey().Execute(listenerName,
				App.getInstance().Zeze.NewProcedure(() -> {
					BOnline online = table.get(roleId);
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

						SendInProcedure(roleId, notify.getTypeId(), new Binary(notify.Encode()), future);
					}
					online.setReliableNotifyTotalCount(online.getReliableNotifyConfirmCount() + 1); // 后加，start 是 Queue.Add 之前的。
					return Procedure.Success;
				}, "SendReliableNotify." + listenerName), null);

		if (future != null) {
			future.await();
		}
	}

	public static class RoleOnLink {
		private String LinkName = "";
		private AsyncSocket LinkSocket;
		private int ProviderId = -1;
		private long ProviderSessionId;
		private final HashMap<Long, BTransmitContext> Roles = new HashMap<>();

		public final String getLinkName() {
			return LinkName;
		}

		public final void setLinkName(String value) {
			LinkName = value;
		}

		public final AsyncSocket getLinkSocket() {
			return LinkSocket;
		}

		public final void setLinkSocket(AsyncSocket value) {
			LinkSocket = value;
		}

		public final int getProviderId() {
			return ProviderId;
		}

		public final void setProviderId(int value) {
			ProviderId = value;
		}

		public final long getProviderSessionId() {
			return ProviderSessionId;
		}

		public final void setProviderSessionId(long value) {
			ProviderSessionId = value;
		}

		public final HashMap<Long, BTransmitContext> getRoles() {
			return Roles;
		}
	}

	public final Collection<RoleOnLink> GroupByLink(Collection<Long> roleIds) {
		var groups = new HashMap<String, RoleOnLink>();
		var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.getLinkName(), groupNotOnline);

		for (var roleId : roleIds) {
			var online = table.get(roleId);
			if (online == null || online.getState() != BOnline.StateOnline) {
				groupNotOnline.getRoles().putIfAbsent(roleId, new BTransmitContext());
				continue;
			}

			var connector = App.getInstance().Server.getLinks().get(online.getLinkName());
			if (connector == null) {
				groupNotOnline.getRoles().putIfAbsent(roleId, new BTransmitContext());
				continue;
			}
			if (!connector.isHandshakeDone()) {
				groupNotOnline.getRoles().putIfAbsent(roleId, new BTransmitContext());
				continue;
			}
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(online.getLinkName());
			if (group == null) {
				group = new RoleOnLink();
				group.setLinkName(online.getLinkName());
				group.setLinkSocket(connector.getSocket());
				group.setProviderId(online.getProviderId());
				group.setProviderSessionId(online.getProviderSessionId());
				groups.put(group.LinkName, group);
			}
			BTransmitContext tempVar = new BTransmitContext(); // 使用 TryAdd，忽略重复的 roleId。
			tempVar.setLinkSid(online.getLinkSid());
			tempVar.setProviderId(online.getProviderId());
			tempVar.setProviderSessionId(online.getProviderSessionId());
			group.Roles.putIfAbsent(roleId, tempVar);
		}
		return groups.values();
	}

	private void SendInProcedure(Long roleId, long typeId, Binary fullEncodedProtocol, TaskCompletionSource<Long> future) {
		// 发送消息为了用上TaskOneByOne，只能一个一个发送，为了少改代码，先使用旧的GroupByLink接口。
		var groups = GroupByLink(List.of(roleId));
		long serialId = 0;
		if (future != null) {
			var confirmContext = new ConfirmContext(future);
			// 必须在真正发送前全部加入，否则要是发生结果很快返回，
			// 导致异步问题：错误的认为所有 Confirm 都收到。
			for (var group : groups) {
				if (group.getLinkSocket() == null) {
					continue; // skip not online
				}

				confirmContext.getLinkNames().add(group.getLinkName());
			}
			serialId = App.getInstance().Server.AddManualContextWithTimeout(confirmContext, 5000);
		}

		for (var group : groups) {
			if (group.getLinkSocket() == null) {
				continue; // skip not online
			}

			var send = new Send();
			send.Argument.setProtocolType(typeId);
			send.Argument.setProtocolWholeData(fullEncodedProtocol);
			send.Argument.setConfirmSerialId(serialId);

			for (var ctx : group.getRoles().values()) {
				send.Argument.getLinkSids().add(ctx.getLinkSid());
			}
			group.getLinkSocket().Send(send);
		}
	}

	private void Send(Long roleId, long typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		final TaskCompletionSource<Long> future = WaitConfirm ? new TaskCompletionSource<>() : null;

		// 发送协议请求在另外的事务中执行。
		App.Instance.Zeze.getTaskOneByOneByKey().Execute(roleId, () ->
				Zeze.Util.Task.Call(App.Instance.Zeze.NewProcedure(() -> {
					SendInProcedure(roleId, typeId, fullEncodedProtocol, future);
					return Procedure.Success;
				}, "Onlines.Send"), null, null));

		if (future != null) {
			future.await();
		}
	}

	public final void Send(long roleId, Protocol<?> p) {
		Send(roleId, p, false);
	}

	public final void Send(long roleId, Protocol<?> p, boolean WaitConfirm) {
		Send(roleId, p.getTypeId(), new Binary(p.Encode()), WaitConfirm);
	}

	// 广播不支持 WaitConfirm
	public final void Send(Collection<Long> roleIds, Protocol<?> p) {
		for (var roleId : roleIds)
			Send(roleId, p.getTypeId(), new Binary(p.Encode()), false);
	}

	public final void SendWhileCommit(long roleId, Protocol<?> p) {
		SendWhileCommit(roleId, p, false);
	}

	public final void SendWhileCommit(long roleId, Protocol<?> p, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> Send(roleId, p, WaitConfirm));
	}

	public final void SendWhileCommit(Collection<Long> roleIds, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> Send(roleIds, p));
	}

	public final void SendWhileRollback(long roleId, Protocol<?> p) {
		SendWhileRollback(roleId, p, false);
	}

	public final void SendWhileRollback(long roleId, Protocol<?> p, boolean WaitConfirm) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> Send(roleId, p, WaitConfirm));
	}

	public final void SendWhileRollback(Collection<Long> roleIds, Protocol<?> p) {
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> Send(roleIds, p));
	}

	/**
	 * Func<sender, target, result>
	 * sender: 查询发起者，结果发送给他。
	 * target: 查询目标角色。
	 * result: 返回值，int，按普通事务处理过程返回值处理。
	 */
	private final ConcurrentHashMap<String, Func3<Long, Long, Serializable, Long>> TransmitActions = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Zeze.Util.Func1<String, Serializable>> transmitParameterFactorys = new ConcurrentHashMap<>();

	public final ConcurrentHashMap<String, Zeze.Util.Func3<Long, Long, Serializable, Long>> getTransmitActions() {
		return TransmitActions;
	}

	public final ConcurrentHashMap<String, Zeze.Util.Func1<String, Serializable>> getTransmitParameterFactorys() {
		return transmitParameterFactorys;
	}

	/**
	 * 转发查询请求给RoleId。
	 *
	 * @param sender     查询发起者，结果发送给他。
	 * @param actionName 查询处理的实现
	 * @param roleId     目标角色
	 */
	public final void Transmit(long sender, String actionName, long roleId, Serializable parameter) {
		Transmit(sender, actionName, List.of(roleId), parameter);
	}

	public final void Transmit(long sender, String actionName, long roleId) {
		Transmit(sender, actionName, roleId, null);
	}

	public final void ProcessTransmit(long sender, String actionName, java.lang.Iterable<Long> roleIds, Serializable parameter) {
		var handle = getTransmitActions().get(actionName);
		if (handle != null) {
			for (var target : roleIds) {
				Zeze.Util.Task.run(
						App.Instance.Zeze.NewProcedure(
								() -> handle.call(sender, target, parameter),
								"Game.Online.Transmit:" + actionName),
						null, null);
			}
		}
	}

	private void TransmitInProcedure(long sender, String actionName, Collection<Long> roleIds, Serializable parameter) {
		if (App.getInstance().Zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().length() == 0) {
			// 没有启用cache-sync，马上触发本地任务。
			ProcessTransmit(sender, actionName, roleIds, parameter);
			return;
		}

		var groups = GroupByLink(roleIds);
		for (var group : groups) {
			if (group.getProviderId() == App.getInstance().Zeze.getConfig().getServerId() // loopback 就是当前gs.
					|| group.getLinkSocket() == null) { // 对于不在线的角色，本机处理。
				ProcessTransmit(sender, actionName, group.getRoles().keySet(), parameter);
				continue;
			}
			var transmit = new Transmit();
			transmit.Argument.setActionName(actionName);
			transmit.Argument.setSender(sender);
			transmit.Argument.setServiceNamePrefix(App.Instance.ProviderApp.ServerServiceNamePrefix);
			transmit.Argument.getRoles().putAll(group.getRoles());
			if (parameter != null) {
				transmit.Argument.setParameterBeanName(parameter.getClass().getName());
				transmit.Argument.setParameterBeanValue(new Binary(Zeze.Serialize.ByteBuffer.Encode(parameter)));
			}
			group.getLinkSocket().Send(transmit);
		}
	}

	public final void Transmit(long sender, String actionName, Collection<Long> roleIds) {
		Transmit(sender, actionName, roleIds, null);
	}

	public final void Transmit(long sender, String actionName, Collection<Long> roleIds, Serializable parameter) {
		if (!getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unknown Action Name: " + actionName);
		}

		// 发送协议请求在另外的事务中执行。
		Zeze.Util.Task.run(App.getInstance().Zeze.NewProcedure(() -> {
			TransmitInProcedure(sender, actionName, roleIds, parameter);
			return Procedure.Success;
		}, "Onlines.Transmit"), null, null);
	}

	public final void TransmitWhileCommit(long sender, String actionName, long roleId) {
		TransmitWhileCommit(sender, actionName, roleId, null);
	}

	public final void TransmitWhileCommit(long sender, String actionName, long roleId, Serializable parameter) {
		if (!getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unknown Action Name: " + actionName);
		}
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> Transmit(sender, actionName, roleId, parameter));
	}

	public final void TransmitWhileCommit(long sender, String actionName, Collection<Long> roleIds) {
		TransmitWhileCommit(sender, actionName, roleIds, null);
	}

	public final void TransmitWhileCommit(long sender, String actionName, Collection<Long> roleIds, Serializable parameter) {
		if (!getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unknown Action Name: " + actionName);
		}
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileCommit(() -> Transmit(sender, actionName, roleIds, parameter));
	}

	public final void TransmitWhileRollback(long sender, String actionName, long roleId) {
		TransmitWhileRollback(sender, actionName, roleId, null);
	}

	public final void TransmitWhileRollback(long sender, String actionName, long roleId, Serializable parameter) {
		if (!getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unknown Action Name: " + actionName);
		}
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> Transmit(sender, actionName, roleId, parameter));
	}

	public final void TransmitWhileRollback(long sender, String actionName, Collection<Long> roleIds) {
		TransmitWhileRollback(sender, actionName, roleIds, null);
	}

	public final void TransmitWhileRollback(long sender, String actionName, Collection<Long> roleIds, Serializable parameter) {
		if (!getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unknown Action Name: " + actionName);
		}
		//noinspection ConstantConditions
		Transaction.getCurrent().RunWhileRollback(() -> Transmit(sender, actionName, roleIds, parameter));
	}

	public static class ConfirmContext extends Service.ManualContext {
		private final HashSet<String> LinkNames = new HashSet<>();

		public final HashSet<String> getLinkNames() {
			return LinkNames;
		}

		private final TaskCompletionSource<Long> Future;

		public final TaskCompletionSource<Long> getFuture() {
			return Future;
		}

		public ConfirmContext(TaskCompletionSource<Long> future) {
			Future = future;
		}

		@Override
		public void OnRemoved() {
			synchronized (this) {
				getFuture().SetResult(super.getSessionId());
			}
		}

		public final long ProcessLinkConfirm(String linkName) {
			synchronized (this) {
				getLinkNames().remove(linkName);
				if (getLinkNames().isEmpty()) {
					App.getInstance().Server.<ConfirmContext>TryRemoveManualContext(getSessionId());
				}
				return Procedure.Success;
			}
		}
	}

	private void Broadcast(long typeId, Binary fullEncodedProtocol, int time, boolean WaitConfirm) {
		TaskCompletionSource<Long> future = null;
		long serialId = 0;
		if (WaitConfirm) {
			future = new TaskCompletionSource<>();
			var confirmContext = new ConfirmContext(future);
			for (var link : App.getInstance().Server.getLinks().values()) {
				if (link.getSocket() != null) {
					confirmContext.getLinkNames().add(link.getName());
				}
			}
			serialId = App.getInstance().Server.AddManualContextWithTimeout(confirmContext, 5000);
		}

		var broadcast = new Broadcast();
		broadcast.Argument.setProtocolType(typeId);
		broadcast.Argument.setProtocolWholeData(fullEncodedProtocol);
		broadcast.Argument.setConfirmSerialId(serialId);
		broadcast.Argument.setTime(time);

		for (var link : App.getInstance().Server.getLinks().values()) {
			if (link.getSocket() != null) {
				link.getSocket().Send(broadcast);
			}
		}

		if (future != null) {
			future.await();
		}
	}

	public final void Broadcast(Protocol<?> p, int time) {
		Broadcast(p, time, false);
	}

	public final void Broadcast(Protocol<?> p) {
		Broadcast(p, 60 * 1000, false);
	}

	public final void Broadcast(Protocol<?> p, int time, boolean WaitConfirm) {
		Broadcast(p.getTypeId(), new Binary(p.Encode()), time, WaitConfirm);
	}
}
