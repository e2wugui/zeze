package Game.Login;

import Zeze.Net.*;
import Zeze.Transaction.*;
import Game.*;
import Zeze.Util.TaskCompletionSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

public class Onlines {
	private tonline table;

	public Onlines(tonline table) {
		this.table = table;
	}

	public final void OnLinkBroken(long roleId) {
		var online = table.Get(roleId);
		if (null != online) {
			online.setState(BOnline.StateNetBroken);
		}

		Zeze.Util.Task.schedule((ThisTask) -> {
				App.getInstance().Zeze.NewProcedure(() -> {
					// 网络断开后延迟删除在线状态。这里简单判断一下是否StateNetBroken。
					// 由于CLogin,CReLogin的时候没有取消Timeout，所以有可能再次登录断线后，会被上一次断线的Timeout删除。
					// 造成延迟时间不准确。管理Timeout有点烦，先这样吧。
					var online2 = table.Get(roleId);
					if (null != online2 && online2.getState() == BOnline.StateNetBroken) {
						table.Remove(roleId);
					}
					App.getInstance().getLoad().getLogoutCount().incrementAndGet();

					return Procedure.Success;
				}, "Onlines.OnLinkBroken", null).Call();
		}, 10 * 60 * 1000, -1); // 10 minuts for relogin
	}

	public final void AddReliableNotifyMark(long roleId, String listenerName) {
		var online = table.Get(roleId);
		if (null == online || online.getState() != BOnline.StateOnline) {
			throw new RuntimeException("Not Online. AddReliableNotifyMark: " + listenerName);
		}
		online.getReliableNotifyMark().add(listenerName);
	}

	public final void RemoveReliableNotifyMark(long roleId, String listenerName) {
		// 移除尽量通过，不做任何判断。
		if (table.Get(roleId) != null) {
			table.Get(roleId).getReliableNotifyMark().remove(listenerName);
		}
	}


	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol p) {
		SendReliableNotifyWhileCommit(roleId, listenerName, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotifyWhileCommit(long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol p, boolean WaitConfirm) {
		Transaction.getCurrent().RunWhileCommit(() -> SendReliableNotify(roleId, listenerName, p, WaitConfirm));
	}


	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		SendReliableNotifyWhileCommit(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotifyWhileCommit(long roleId, string listenerName, int typeId, Binary fullEncodedProtocol, bool WaitConfirm = false)
	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		Transaction.getCurrent().RunWhileCommit(() -> SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm));
	}


	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol p) {
		SendReliableNotifyWhileRollback(roleId, listenerName, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotifyWhileRollback(long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol p, boolean WaitConfirm) {
		Transaction.getCurrent().RunWhileRollback(() -> SendReliableNotify(roleId, listenerName, p, WaitConfirm));
	}


	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		SendReliableNotifyWhileRollback(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotifyWhileRollback(long roleId, string listenerName, int typeId, Binary fullEncodedProtocol, bool WaitConfirm = false)
	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		Transaction.getCurrent().RunWhileRollback(() -> SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm));
	}


	public final void SendReliableNotify(long roleId, String listenerName, Protocol p) {
		SendReliableNotify(roleId, listenerName, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotify(long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
	public final void SendReliableNotify(long roleId, String listenerName, Protocol p, boolean WaitConfirm) {
		SendReliableNotify(roleId, listenerName, p.getTypeId(), new Binary(p.Encode()), WaitConfirm);
	}

	/** 
	 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 
	 @param roleId
	 @param listenerName
	 @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	*/

	public final void SendReliableNotify(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotify(long roleId, string listenerName, int typeId, Binary fullEncodedProtocol, bool WaitConfirm = false)
	public final void SendReliableNotify(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		final TaskCompletionSource<Long> future = WaitConfirm ? new TaskCompletionSource<Long>() : null;

		App.getInstance().Zeze.getTaskOneByOneByKey().Execute(listenerName,
				App.getInstance().Zeze.NewProcedure(() -> {
					BOnline online = table.Get(roleId);
					if (null == online || online.getState() == BOnline.StateOffline) {
						return Procedure.Success;
					}
					if (false == online.getReliableNotifyMark().contains(listenerName)) {
						return Procedure.Success; // 相关数据装载的时候要同步设置这个。
					}

					// 先保存在再发送，然后客户端还会确认。
					// see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
					online.getReliableNotifyQueue().add(fullEncodedProtocol);
					if (online.getState() == BOnline.StateOnline) {
						var notify = new SReliableNotify(); // 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
						notify.Argument.setReliableNotifyTotalCountStart(online.getReliableNotifyTotalCount());
						notify.Argument.getNotifies().add(fullEncodedProtocol);

						SendInProcedure(new ArrayList<Long>(Arrays.asList(roleId)), notify.getTypeId(), new Binary(notify.Encode()), future);
					}
					online.setReliableNotifyTotalCount(online.getReliableNotifyConfirmCount() + 1); // 后加，start 是 Queue.Add 之前的。
					return Procedure.Success;
		}, "SendReliableNotify." + listenerName, null), null);

		if (future != null) {
			future.Wait();
		}
	}

	public static class RoleOnLink {
		private String LinkName = "";
		public final String getLinkName() {
			return LinkName;
		}
		public final void setLinkName(String value) {
			LinkName = value;
		}
		private AsyncSocket LinkSocket;
		public final AsyncSocket getLinkSocket() {
			return LinkSocket;
		}
		public final void setLinkSocket(AsyncSocket value) {
			LinkSocket = value;
		}
		private int ProviderId = -1;
		public final int getProviderId() {
			return ProviderId;
		}
		public final void setProviderId(int value) {
			ProviderId = value;
		}
		private long ProviderSessionId;
		public final long getProviderSessionId() {
			return ProviderSessionId;
		}
		public final void setProviderSessionId(long value) {
			ProviderSessionId = value;
		}
		private HashMap<Long, Zezex.Provider.BTransmitContext> Roles = new HashMap<Long, Zezex.Provider.BTransmitContext> ();
		public final HashMap<Long, Zezex.Provider.BTransmitContext> getRoles() {
			return Roles;
		}
	}

	public final Collection<RoleOnLink> GroupByLink(Collection<Long> roleIds) {
		var groups = new HashMap<String, RoleOnLink>();
		var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.getLinkName(), groupNotOnline);

		for (var roleId : roleIds) {
			var online = table.Get(roleId);
			if (null == online || online.getState() != BOnline.StateOnline) {
				groupNotOnline.getRoles().putIfAbsent(roleId, new Zezex.Provider.BTransmitContext());
				continue;
			}

			var connector = App.getInstance().Server.getLinks().get(online.getLinkName());
			if (null == connector) {
				groupNotOnline.getRoles().putIfAbsent(roleId, new Zezex.Provider.BTransmitContext());
				continue;
			}
			if (false == connector.isHandshakeDone()) {
				groupNotOnline.getRoles().putIfAbsent(roleId, new Zezex.Provider.BTransmitContext());
				continue;
			}
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(online.getLinkName());
			if (null == group) {
				group = new RoleOnLink();
				group.setLinkName(online.getLinkName());
				group.setLinkSocket(connector.getSocket());
				group.setProviderId(online.getProviderId());
				group.setProviderSessionId(online.getProviderSessionId());
				groups.put(group.LinkName, group);
			}
			Zezex.Provider.BTransmitContext tempVar = new Zezex.Provider.BTransmitContext(); // 使用 TryAdd，忽略重复的 roleId。
			tempVar.setLinkSid(online.getLinkSid());
			tempVar.setProviderId(online.getProviderId());
			tempVar.setProviderSessionId(online.getProviderSessionId());
			group.Roles.putIfAbsent(roleId, tempVar);
		}
		return groups.values();
	}

	private void SendInProcedure(Collection<Long> roleIds, int typeId, Binary fullEncodedProtocol, TaskCompletionSource<Long> future) {
		var groups = GroupByLink(roleIds);
		long serialId = 0;
		if (null != future) {
			var confrmContext = new ConfirmContext(future);
			// 必须在真正发送前全部加入，否则要是发生结果很快返回，
			// 导致异步问题：错误的认为所有 Confirm 都收到。
			for (var group : groups) {
				if (group.getLinkSocket() == null) {
					continue; // skip not online
				}

				confrmContext.getLinkNames().add(group.getLinkName());
			}
			serialId = App.getInstance().Server.AddManualContextWithTimeout(confrmContext, 5000);
		}

		for (var group : groups) {
			if (group.getLinkSocket() == null) {
				continue; // skip not online
			}

			var send = new Zezex.Provider.Send();
			send.Argument.setProtocolType(typeId);
			send.Argument.setProtocolWholeData(fullEncodedProtocol);
			send.Argument.setConfirmSerialId(serialId);

			for (var ctx : group.getRoles().values()) {
				send.Argument.getLinkSids().add(ctx.getLinkSid());
			}
			group.getLinkSocket().Send(send);
		}
	}

	private void Send(Collection<Long> roleIds, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		final TaskCompletionSource<Long> future = WaitConfirm ? new TaskCompletionSource<Long>() : null;

		// 发送协议请求在另外的事务中执行。
		Zeze.Util.Task.Run(App.Instance.Zeze.NewProcedure(() -> {
				SendInProcedure(roleIds, typeId, fullEncodedProtocol, future);
				return Procedure.Success;
		}, "Onlines.Send", null), null, null);

		if (future != null) {
			future.Wait();
		}
	}


	public final void Send(long roleId, Protocol p) {
		Send(roleId, p, false);
	}

	public final void Send(long roleId, Protocol p, boolean WaitConfirm) {
		Send(new ArrayList<Long>(Arrays.asList(roleId)), p.getTypeId(), new Binary(p.Encode()), WaitConfirm);
	}


	public final void Send(java.util.Collection<Long> roleIds, Protocol p) {
		Send(roleIds, p, false);
	}

	public final void Send(Collection<Long> roleIds, Protocol p, boolean WaitConfirm) {
		Send(roleIds, p.getTypeId(), new Binary(p.Encode()), WaitConfirm);
	}


	public final void SendWhileCommit(long roleId, Protocol p) {
		SendWhileCommit(roleId, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendWhileCommit(long roleId, Protocol p, bool WaitConfirm = false)
	public final void SendWhileCommit(long roleId, Protocol p, boolean WaitConfirm) {
		Transaction.getCurrent().RunWhileCommit(() -> Send(roleId, p, WaitConfirm));
	}


	public final void SendWhileCommit(java.util.Collection<Long> roleIds, Protocol p) {
		SendWhileCommit(roleIds, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendWhileCommit(ICollection<long> roleIds, Protocol p, bool WaitConfirm = false)
	public final void SendWhileCommit(Collection<Long> roleIds, Protocol p, boolean WaitConfirm) {
		Transaction.getCurrent().RunWhileCommit(() -> Send(roleIds, p, WaitConfirm));
	}


	public final void SendWhileRollback(long roleId, Protocol p) {
		SendWhileRollback(roleId, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendWhileRollback(long roleId, Protocol p, bool WaitConfirm = false)
	public final void SendWhileRollback(long roleId, Protocol p, boolean WaitConfirm) {
		Transaction.getCurrent().RunWhileRollback(() -> Send(roleId, p, WaitConfirm));
	}


	public final void SendWhileRollback(java.util.Collection<Long> roleIds, Protocol p) {
		SendWhileRollback(roleIds, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendWhileRollback(ICollection<long> roleIds, Protocol p, bool WaitConfirm = false)
	public final void SendWhileRollback(Collection<Long> roleIds, Protocol p, boolean WaitConfirm) {
		Transaction.getCurrent().RunWhileRollback(() -> Send(roleIds, p, WaitConfirm));
	}

	/** 
	 Func<sender, target, result>
	 sender: 查询发起者，结果发送给他。
	 target: 查询目标角色。
	 result: 返回值，int，按普通事务处理过程返回值处理。
	*/
	private ConcurrentHashMap<String, Zeze.Util.Func2<Long, Long, Integer>> TransmitActions = new ConcurrentHashMap<> ();
	public final ConcurrentHashMap<String, Zeze.Util.Func2<Long, Long, Integer>> getTransmitActions() {
		return TransmitActions;
	}

	/** 
	 转发查询请求给RoleId。
	 
	 @param sender 查询发起者，结果发送给他。
	 @param actionName 查询处理的实现
	 @param roleId 目标角色
	*/
	public final void Transmit(long sender, String actionName, long roleId) {
		Transmit(sender, actionName, new ArrayList<Long>(Arrays.asList(roleId)));
	}

	public final void ProcessTransmit(long sender, String actionName, java.lang.Iterable<Long> roleIds) {
		var handle = getTransmitActions().get(actionName);
		if (null != handle) {
			for (var target : roleIds) {
				Zeze.Util.Task.Run(
						App.Instance.Zeze.NewProcedure(
								() -> handle.call(sender, target),
								"Game.Online.Transmit:" + actionName, null),
						null, null);
			}
		}
	}

	private void TransmitInProcedure(long sender, String actionName, Collection<Long> roleIds) {
		if (App.getInstance().Zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().length() == 0) {
			// 没有启用cache-sync，马上触发本地任务。
			ProcessTransmit(sender, actionName, roleIds);
			return;
		}

		var groups = GroupByLink(roleIds);
		for (var group : groups) {
			if (group.getProviderId() == App.getInstance().Zeze.getConfig().getServerId()) {
				// loopback 就是当前gs.
				ProcessTransmit(sender, actionName, group.getRoles().keySet());
				continue;
			}
			var transmit = new Zezex.Provider.Transmit();
			transmit.Argument.setActionName(actionName);
			transmit.Argument.setSender(sender);
			transmit.Argument.setServiceNamePrefix(App.ServerServiceNamePrefix);
			transmit.Argument.getRoles().putAll(group.getRoles());

			if (null != group.getLinkSocket()) {
				group.getLinkSocket().Send(transmit);
				continue;
			}

			// 对于不在线的角色，随机选择一个linkd转发。
			ArrayList<AsyncSocket> readyLinks = new ArrayList<AsyncSocket>();
			for (var link : App.getInstance().Server.getLinks().values()) {
				if (link.isHandshakeDone()) {
					readyLinks.add(link.getSocket());
				}
			}
			if (!readyLinks.isEmpty()) {
				var randLink = readyLinks.get(Zeze.Util.Random.getInstance().nextInt(readyLinks.size()));
				randLink.Send(transmit);
			}
		}
	}

	public final void Transmit(long sender, String actionName, Collection<Long> roleIds) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}

		// 发送协议请求在另外的事务中执行。
		Zeze.Util.Task.Run(App.getInstance().Zeze.NewProcedure(() -> {
				TransmitInProcedure(sender, actionName, roleIds);
				return Procedure.Success;
		}, "Onlines.Transmit", null), null, null);
	}

	public final void TransmitWhileCommit(long sender, String actionName, long roleId) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}
		Transaction.getCurrent().RunWhileCommit(() -> Transmit(sender, actionName, roleId));
	}

	public final void TransmitWhileCommit(long sender, String actionName, Collection<Long> roleIds) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}
		Transaction.getCurrent().RunWhileCommit(() -> Transmit(sender, actionName, roleIds));
	}

	public final void TransmitWhileRollback(long sender, String actionName, long roleId) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}
		Transaction.getCurrent().RunWhileRollback(() -> Transmit(sender, actionName, roleId));
	}

	public final void TransmitWhileRollback(long sender, String actionName, Collection<Long> roleIds) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}
		Transaction.getCurrent().RunWhileRollback(() -> Transmit(sender, actionName, roleIds));
	}

	public static class ConfirmContext extends Service.ManualContext {
		private HashSet<String> LinkNames = new HashSet<String> ();
		public final HashSet<String> getLinkNames() {
			return LinkNames;
		}
		private TaskCompletionSource<Long> Future;
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

		public final int ProcessLinkConfirm(String linkName) {
			synchronized (this) {
				getLinkNames().remove(linkName);
				if (getLinkNames().isEmpty()) {
					App.getInstance().Server.<ConfirmContext>TryRemoveManualContext(getSessionId());
				}
				return Procedure.Success;
			}
		}
	}

	private void Broadcast(int typeId, Binary fullEncodedProtocol, int time, boolean WaitConfirm) {
		TaskCompletionSource<Long> future = null;
		long serialId = 0;
		if (WaitConfirm) {
			future = new TaskCompletionSource<Long>();
			var confirmContext = new ConfirmContext(future);
			for (var link : App.getInstance().Server.getLinks().values()) {
				if (link.getSocket() != null) {
					confirmContext.getLinkNames().add(link.getName());
				}
			}
			serialId = App.getInstance().Server.AddManualContextWithTimeout(confirmContext, 5000);
		}

		var broadcast = new Zezex.Provider.Broadcast();
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
			future.Wait();
		}
	}


	public final void Broadcast(Protocol p, int time) {
		Broadcast(p, time, false);
	}

	public final void Broadcast(Protocol p) {
		Broadcast(p, 60 * 1000, false);
	}

	public final void Broadcast(Protocol p, int time, boolean WaitConfirm) {
		Broadcast(p.getTypeId(), new Binary(p.Encode()), time, WaitConfirm);
	}
}