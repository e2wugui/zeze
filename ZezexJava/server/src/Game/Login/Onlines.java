package Game.Login;

import Zeze.Net.*;
import Zeze.Transaction.*;
import Game.*;
import java.util.*;

public class Onlines {
	private tonline table;

	public Onlines(tonline table) {
		this.table = table;
	}

	public final void OnLinkBroken(long roleId) {
		var online = table.Get(roleId);
		if (null != online) {
			online.State = BOnline.StateNetBroken;
		}

		Zeze.Util.Scheduler.Instance.Schedule((ThisTask) -> {
				App.getInstance().getZeze().NewProcedure(() -> {
					// 网络断开后延迟删除在线状态。这里简单判断一下是否StateNetBroken。
					// 由于CLogin,CReLogin的时候没有取消Timeout，所以有可能再次登录断线后，会被上一次断线的Timeout删除。
					// 造成延迟时间不准确。管理Timeout有点烦，先这样吧。
					var online = table.Get(roleId);
					if (null != online && online.getState() == BOnline.StateNetBroken) {
						table.Remove(roleId);
					}
					App.getInstance().getLoad().getLogoutCount().IncrementAndGet();

					return Procedure.Success;
				}, "Onlines.OnLinkBroken", null).Call();
		}, 10 * 60 * 1000, -1); // 10 minuts for relogin
	}

	public final void AddReliableNotifyMark(long roleId, String listenerName) {
		var online = table.Get(roleId);
		if (null == online || online.getState() != BOnline.StateOnline) {
			throw new RuntimeException("Not Online. AddReliableNotifyMark: " + listenerName);
		}
		online.getReliableNotifyMark().Add(listenerName);
	}

	public final void RemoveReliableNotifyMark(long roleId, String listenerName) {
		// 移除尽量通过，不做任何判断。
		if (table.Get(roleId) != null) {
			table.Get(roleId).getReliableNotifyMark().Remove(listenerName);
		}
	}


	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol p) {
		SendReliableNotifyWhileCommit(roleId, listenerName, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotifyWhileCommit(long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, Protocol p, boolean WaitConfirm) {
		Transaction.Current.RunWhileCommit(() -> SendReliableNotify(roleId, listenerName, p, WaitConfirm));
	}


	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		SendReliableNotifyWhileCommit(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotifyWhileCommit(long roleId, string listenerName, int typeId, Binary fullEncodedProtocol, bool WaitConfirm = false)
	public final void SendReliableNotifyWhileCommit(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		Transaction.Current.RunWhileCommit(() -> SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm));
	}


	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol p) {
		SendReliableNotifyWhileRollback(roleId, listenerName, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotifyWhileRollback(long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, Protocol p, boolean WaitConfirm) {
		Transaction.Current.RunWhileRollback(() -> SendReliableNotify(roleId, listenerName, p, WaitConfirm));
	}


	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		SendReliableNotifyWhileRollback(roleId, listenerName, typeId, fullEncodedProtocol, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotifyWhileRollback(long roleId, string listenerName, int typeId, Binary fullEncodedProtocol, bool WaitConfirm = false)
	public final void SendReliableNotifyWhileRollback(long roleId, String listenerName, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		Transaction.Current.RunWhileRollback(() -> SendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol, WaitConfirm));
	}


	public final void SendReliableNotify(long roleId, String listenerName, Protocol p) {
		SendReliableNotify(roleId, listenerName, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReliableNotify(long roleId, string listenerName, Protocol p, bool WaitConfirm = false)
	public final void SendReliableNotify(long roleId, String listenerName, Protocol p, boolean WaitConfirm) {
		SendReliableNotify(roleId, listenerName, p.TypeId, new Binary(p.Encode()), WaitConfirm);
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
		TaskCompletionSource<Long> future = null;

		if (WaitConfirm) {
			future = new TaskCompletionSource<Long>();
		}

		App.getInstance().getZeze().TaskOneByOneByKey.Execute(listenerName, App.getInstance().getZeze().NewProcedure(() -> {
					BOnline online = table.Get(roleId);
					if (null == online || online.getState() == BOnline.StateOffline) {
						return Procedure.Success;
					}
					if (false == online.getReliableNotifyMark().Contains(listenerName)) {
						return Procedure.Success; // 相关数据装载的时候要同步设置这个。
					}

					// 先保存在再发送，然后客户端还会确认。
					// see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
					online.getReliableNotifyQueue().Add(fullEncodedProtocol);
					if (online.getState() == BOnline.StateOnline) {
						var notify = new SReliableNotify(); // 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
						notify.getArgument().ReliableNotifyTotalCountStart = online.getReliableNotifyTotalCount();
						notify.getArgument().getNotifies().Add(fullEncodedProtocol);

						SendInProcedure(new ArrayList<Long>(Arrays.asList(roleId)), notify.TypeId, new Binary(notify.Encode()), future);
					}
					online.ReliableNotifyTotalCount += 1; // 后加，start 是 Queue.Add 之前的。
					return Procedure.Success;
		}, "SendReliableNotify." + listenerName, null), null);

		if (future != null) {
			future.Task.Wait();
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
				groupNotOnline.getRoles().TryAdd(roleId, new Zezex.Provider.BTransmitContext());
				continue;
			}

			TValue connector;
			tangible.OutObject<TValue> tempOut_connector = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (false == App.getInstance().getServer().getLinks().TryGetValue(online.getLinkName(), tempOut_connector)) {
			connector = tempOut_connector.outArgValue;
				groupNotOnline.getRoles().TryAdd(roleId, new Zezex.Provider.BTransmitContext());
				continue;
			}
		else {
			connector = tempOut_connector.outArgValue;
		}

			if (false == connector.IsHandshakeDone) {
				groupNotOnline.getRoles().TryAdd(roleId, new Zezex.Provider.BTransmitContext());
				continue;
			}
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			TValue group;
			if (false == (groups.containsKey(online.getLinkName()) && (group = groups.get(online.getLinkName())) == group)) {
				group = new RoleOnLink();
				group.setLinkName(online.getLinkName());
				group.setLinkSocket(connector.Socket);
				group.setProviderId(online.getProviderId());
				group.setProviderSessionId(online.getProviderSessionId());
				groups.put(group.LinkName, group);
			}
			Zezex.Provider.BTransmitContext tempVar = new Zezex.Provider.BTransmitContext(); // 使用 TryAdd，忽略重复的 roleId。
			tempVar.setLinkSid(online.getLinkSid());
			tempVar.setProviderId(online.getProviderId());
			tempVar.setProviderSessionId(online.getProviderSessionId());
			group.Roles.TryAdd(roleId, tempVar);
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
			serialId = App.getInstance().getServer().AddManualContextWithTimeout(confrmContext, 5000);
		}

		for (var group : groups) {
			if (group.getLinkSocket() == null) {
				continue; // skip not online
			}

			var send = new Zezex.Provider.Send();
			send.getArgument().ProtocolType = typeId;
			send.getArgument().ProtocolWholeData = fullEncodedProtocol;
			send.getArgument().ConfirmSerialId = serialId;

			for (var ctx : group.getRoles().values()) {
				send.getArgument().getLinkSids().Add(ctx.LinkSid);
			}
			group.getLinkSocket().Send(send);
		}
	}

	private void Send(Collection<Long> roleIds, int typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		TaskCompletionSource<Long> future = null;

		if (WaitConfirm) {
			future = new TaskCompletionSource<Long>();
		}

		// 发送协议请求在另外的事务中执行。
		Zeze.Util.Task.Run(App.getInstance().getZeze().NewProcedure(() -> {
				SendInProcedure(roleIds, typeId, fullEncodedProtocol, future);
				return Procedure.Success;
		}, "Onlines.Send", null), null, null);

		if (future != null) {
			future.Task.Wait();
		}
	}


	public final void Send(long roleId, Protocol p) {
		Send(roleId, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Send(long roleId, Protocol p, bool WaitConfirm = false)
	public final void Send(long roleId, Protocol p, boolean WaitConfirm) {
		Send(new ArrayList<Long>(Arrays.asList(roleId)), p.TypeId, new Binary(p.Encode()), WaitConfirm);
	}


	public final void Send(java.util.Collection<Long> roleIds, Protocol p) {
		Send(roleIds, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Send(ICollection<long> roleIds, Protocol p, bool WaitConfirm = false)
	public final void Send(Collection<Long> roleIds, Protocol p, boolean WaitConfirm) {
		Send(roleIds, p.TypeId, new Binary(p.Encode()), WaitConfirm);
	}


	public final void SendWhileCommit(long roleId, Protocol p) {
		SendWhileCommit(roleId, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendWhileCommit(long roleId, Protocol p, bool WaitConfirm = false)
	public final void SendWhileCommit(long roleId, Protocol p, boolean WaitConfirm) {
		Transaction.Current.RunWhileCommit(() -> Send(roleId, p, WaitConfirm));
	}


	public final void SendWhileCommit(java.util.Collection<Long> roleIds, Protocol p) {
		SendWhileCommit(roleIds, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendWhileCommit(ICollection<long> roleIds, Protocol p, bool WaitConfirm = false)
	public final void SendWhileCommit(Collection<Long> roleIds, Protocol p, boolean WaitConfirm) {
		Transaction.Current.RunWhileCommit(() -> Send(roleIds, p, WaitConfirm));
	}


	public final void SendWhileRollback(long roleId, Protocol p) {
		SendWhileRollback(roleId, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendWhileRollback(long roleId, Protocol p, bool WaitConfirm = false)
	public final void SendWhileRollback(long roleId, Protocol p, boolean WaitConfirm) {
		Transaction.Current.RunWhileRollback(() -> Send(roleId, p, WaitConfirm));
	}


	public final void SendWhileRollback(java.util.Collection<Long> roleIds, Protocol p) {
		SendWhileRollback(roleIds, p, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendWhileRollback(ICollection<long> roleIds, Protocol p, bool WaitConfirm = false)
	public final void SendWhileRollback(Collection<Long> roleIds, Protocol p, boolean WaitConfirm) {
		Transaction.Current.RunWhileRollback(() -> Send(roleIds, p, WaitConfirm));
	}

	/** 
	 Func<sender, target, result>
	 sender: 查询发起者，结果发送给他。
	 target: 查询目标角色。
	 result: 返回值，int，按普通事务处理过程返回值处理。
	*/
	private java.util.concurrent.ConcurrentHashMap<String, tangible.Func2Param<Long, Long, Integer>> TransmitActions = new java.util.concurrent.ConcurrentHashMap<String, tangible.Func2Param<Long, Long, Integer>> ();
	public final java.util.concurrent.ConcurrentHashMap<String, tangible.Func2Param<Long, Long, Integer>> getTransmitActions() {
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
		TValue handle;
		tangible.OutObject<TValue> tempOut_handle = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (getTransmitActions().TryGetValue(actionName, tempOut_handle)) {
		handle = tempOut_handle.outArgValue;
			for (var target : roleIds) {
				Zeze.Util.Task.Run(App.getInstance().getZeze().NewProcedure(() -> handle(sender, target), "Game.Online.Transmit:" + actionName, null), null, null);
			}
		}
	else {
		handle = tempOut_handle.outArgValue;
	}
	}

	private void TransmitInProcedure(long sender, String actionName, Collection<Long> roleIds) {
		if (App.getInstance().getZeze().Config.GlobalCacheManagerHostNameOrAddress.length() == 0) {
			// 没有启用cache-sync，马上触发本地任务。
			ProcessTransmit(sender, actionName, roleIds);
			return;
		}

		var groups = GroupByLink(roleIds);
		for (var group : groups) {
			if (group.getProviderId() == App.getInstance().getZeze().Config.ServerId) {
				// loopback 就是当前gs.
				ProcessTransmit(sender, actionName, group.getRoles().keySet());
				continue;
			}
			var transmit = new Zezex.Provider.Transmit();
			transmit.getArgument().ActionName = actionName;
			transmit.getArgument().Sender = sender;
			transmit.getArgument().ServiceNamePrefix = App.ServerServiceNamePrefix;
			transmit.getArgument().getRoles().AddRange(group.getRoles());

			if (null != group.getLinkSocket()) {
				group.getLinkSocket().Send(transmit);
				continue;
			}

			// 对于不在线的角色，随机选择一个linkd转发。
			ArrayList<AsyncSocket> readyLinks = new ArrayList<AsyncSocket>();
			for (var link : App.getInstance().getServer().getLinks().values()) {
				if (link.IsHandshakeDone) {
					readyLinks.add(link.Socket);
				}
			}
			if (!readyLinks.isEmpty()) {
				var randLink = readyLinks.get(Zeze.Util.Random.Instance.nextInt(readyLinks.size()));
				randLink.Send(transmit);
			}
		}
	}

	public final void Transmit(long sender, String actionName, Collection<Long> roleIds) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}

		// 发送协议请求在另外的事务中执行。
		Zeze.Util.Task.Run(App.getInstance().getZeze().NewProcedure(() -> {
				TransmitInProcedure(sender, actionName, roleIds);
				return Procedure.Success;
		}, "Onlines.Transmit", null), null, null);
	}

	public final void TransmitWhileCommit(long sender, String actionName, long roleId) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}
		Transaction.Current.RunWhileCommit(() -> Transmit(sender, actionName, roleId));
	}

	public final void TransmitWhileCommit(long sender, String actionName, Collection<Long> roleIds) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}
		Transaction.Current.RunWhileCommit(() -> Transmit(sender, actionName, roleIds));
	}

	public final void TransmitWhileRollback(long sender, String actionName, long roleId) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}
		Transaction.Current.RunWhileRollback(() -> Transmit(sender, actionName, roleId));
	}

	public final void TransmitWhileRollback(long sender, String actionName, Collection<Long> roleIds) {
		if (false == getTransmitActions().containsKey(actionName)) {
			throw new RuntimeException("Unkown Action Name: " + actionName);
		}
		Transaction.Current.RunWhileRollback(() -> Transmit(sender, actionName, roleIds));
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
					App.getInstance().getServer().<ConfirmContext>TryRemoveManualContext(getSessionId());
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
			for (var link : App.getInstance().getServer().getLinks().values()) {
				if (link.Socket != null) {
					confirmContext.getLinkNames().add(link.Name);
				}
			}
			serialId = App.getInstance().getServer().AddManualContextWithTimeout(confirmContext, 5000);
		}

		var broadcast = new Zezex.Provider.Broadcast();
		broadcast.getArgument().ProtocolType = typeId;
		broadcast.getArgument().ProtocolWholeData = fullEncodedProtocol;
		broadcast.getArgument().ConfirmSerialId = serialId;
		broadcast.getArgument().Time = time;

		for (var link : App.getInstance().getServer().getLinks().values()) {
			if (link.Socket != null) {
				link.Socket.Send(broadcast);
			}
		}

		if (future != null) {
			future.Task.Wait();
		}
	}


	public final void Broadcast(Protocol p, int time) {
		Broadcast(p, time, false);
	}

	public final void Broadcast(Protocol p) {
		Broadcast(p, 60 * 1000, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Broadcast(Protocol p, int time = 60 * 1000, bool WaitConfirm = false)
	public final void Broadcast(Protocol p, int time, boolean WaitConfirm) {
		Broadcast(p.TypeId, new Binary(p.Encode()), time, WaitConfirm);
	}
}