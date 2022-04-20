package Zeze.Game;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import Zeze.Arch.ProviderService;
import Zeze.Arch.ProviderUserSession;
import Zeze.Beans.Game.Online.BAccount;
import Zeze.Beans.Game.Online.BOnline;
import Zeze.Beans.Game.Online.SReliableNotify;
import Zeze.Beans.Provider.Send;
import Zeze.Beans.Provider.SetUserState;
import Zeze.Beans.ProviderDirect.BTransmitContext;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Online extends AbstractOnline {
	protected static final Logger logger = LogManager.getLogger(Online.class);

	private final ProviderService service;

	public Online(ProviderService service) {
		this.service = service;
		RegisterProtocols(service);
		RegisterZezeTables(service.getZeze());
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

	public final void send(long roleId, Protocol<?> p) {
		send(roleId, p, false);
	}

	public final void send(long roleId, Protocol<?> p, boolean WaitConfirm) {
		send(roleId, p.getTypeId(), new Binary(p.Encode()), WaitConfirm);
	}

	private void send(long roleId, long typeId, Binary fullEncodedProtocol, boolean WaitConfirm) {
		var future = WaitConfirm ? new TaskCompletionSource<Long>() : null;

		// 发送协议请求在另外的事务中执行。
		service.getZeze().getTaskOneByOneByKey().Execute(roleId, () ->
				Task.Call(service.getZeze().NewProcedure(() -> {
					sendInProcedure(roleId, typeId, fullEncodedProtocol, future);
					return Procedure.Success;
				}, "Online.send"), null, null));

		if (future != null)
			future.await();
	}

	private void sendInProcedure(Long roleId, long typeId, Binary fullEncodedProtocol, TaskCompletionSource<Long> future) {
		// 发送消息为了用上TaskOneByOne，只能一个一个发送，为了少改代码，先使用旧的GroupByLink接口。
		var groups = GroupByLink(List.of(roleId));
		long serialId = 0;
		if (future != null) {
			var confirmContext = new ConfirmContext(future);
			// 必须在真正发送前全部加入，否则要是发生结果很快返回，
			// 导致异步问题：错误的认为所有 Confirm 都收到。
			for (var group : groups) {
				if (group.linkSocket != null) // skip not online
					confirmContext.linkNames.add(group.linkName);
			}
			serialId = service.AddManualContextWithTimeout(confirmContext, 5000);
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

	private static final class RoleOnLink {
		String linkName = "";
		AsyncSocket linkSocket;
		int providerId = -1;
		long providerSessionId;
		final HashMap<Long, BTransmitContext> roles = new HashMap<>();
	}

	public final Collection<RoleOnLink> GroupByLink(Collection<Long> roleIds) {
		var groups = new HashMap<String, RoleOnLink>();
		var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.linkName, groupNotOnline);

		for (var roleId : roleIds) {
			var online = _tonline.get(roleId);
			if (online == null || online.getState() != BOnline.StateOnline) {
				groupNotOnline.roles.putIfAbsent(roleId, new BTransmitContext());
				continue;
			}

			var connector = service.getLinks().get(online.getLinkName());
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

	private class ConfirmContext extends Service.ManualContext {
		final HashSet<String> linkNames = new HashSet<>();
		final TaskCompletionSource<Long> future;

		public ConfirmContext(TaskCompletionSource<Long> future) {
			this.future = future;
		}

		@Override
		public synchronized void OnRemoved() {
			future.SetResult(super.getSessionId());
		}

		public final long ProcessLinkConfirm(String linkName) {
			synchronized (this) {
				linkNames.remove(linkName);
				if (linkNames.isEmpty())
					service.<ConfirmContext>TryRemoveManualContext(getSessionId());
				return Procedure.Success;
			}
		}
	}

	public void sendAccount(String account, Protocol<?> p) {
		//TODO
	}

	@Override
	protected long ProcessLoginRequest(Zeze.Beans.Game.Online.Login rpc) {
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
		online.setProviderId(service.getZeze().getConfig().getServerId());
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
	protected long ProcessReLoginRequest(Zeze.Beans.Game.Online.ReLogin rpc) {
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

		var syncResultCode = ReliableNotifySync(session, rpc.Argument.getReliableNotifyConfirmCount(), online);
		if (syncResultCode != ResultCodeSuccess)
			return ErrorCode(syncResultCode);

		return Procedure.Success;
	}

	private int ReliableNotifySync(ProviderUserSession session, long ReliableNotifyConfirmCount, BOnline online) {
		return ReliableNotifySync(session, ReliableNotifyConfirmCount, online, true);
	}

	private int ReliableNotifySync(ProviderUserSession session, long ReliableNotifyConfirmCount, BOnline online, boolean sync) {
		if (ReliableNotifyConfirmCount < online.getReliableNotifyConfirmCount()
				|| ReliableNotifyConfirmCount > online.getReliableNotifyTotalCount()
				|| ReliableNotifyConfirmCount - online.getReliableNotifyConfirmCount() > online.getReliableNotifyQueue().size()) {
			return ResultCodeReliableNotifyConfirmCountOutOfRange;
		}

		int confirmCount = (int)(ReliableNotifyConfirmCount - online.getReliableNotifyConfirmCount());

		if (sync) {
			var notify = new SReliableNotify();
			notify.Argument.setReliableNotifyTotalCountStart(ReliableNotifyConfirmCount);
			for (int i = confirmCount; i < online.getReliableNotifyQueue().size(); i++)
				notify.Argument.getNotifies().add(online.getReliableNotifyQueue().get(i));
			session.SendResponseWhileCommit(notify);
		}
		//noinspection ListRemoveInLoop
		for (int ir = 0; ir < confirmCount; ++ir)
			online.getReliableNotifyQueue().remove(0);
		//online.getReliableNotifyQueue().RemoveRange(0, confirmCount);
		online.setReliableNotifyConfirmCount(ReliableNotifyConfirmCount);
		return ResultCodeSuccess;
	}

	@Override
	protected long ProcessLogoutRequest(Zeze.Beans.Game.Online.Logout rpc) {
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
	protected long ProcessReliableNotifyConfirmRequest(Zeze.Beans.Game.Online.ReliableNotifyConfirm rpc) {
		var session = ProviderUserSession.Get(rpc);

		BOnline online = _tonline.get(session.getRoleId());
		if (online == null || online.getState() == BOnline.StateOffline)
			return ErrorCode(ResultCodeOnlineDataNotFound);

		session.SendResponseWhileCommit(rpc); // 同步前提交。

		var syncResultCode = ReliableNotifySync(session, rpc.Argument.getReliableNotifyConfirmCount(), online, false);
		if (syncResultCode != ResultCodeSuccess)
			return ErrorCode(syncResultCode);

		return Procedure.Success;
	}
}
