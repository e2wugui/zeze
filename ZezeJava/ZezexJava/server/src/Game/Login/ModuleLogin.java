package Game.Login;

import Game.App;
import Game.Server;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;

public final class ModuleLogin extends AbstractModule {
	private Onlines Onlines;

	public void Start(App app) {
		setOnlines(new Onlines(_tonline));
	}

	public void Stop(App app) {
	}

	public Onlines getOnlines() {
		return Onlines;
	}

	private void setOnlines(Onlines value) {
		Onlines = value;
	}

	@Override
	protected long ProcessCreateRoleRequest(CreateRole rpc) {
		var session = ProviderUserSession.Get(rpc);

		BRoleData tempVar = new BRoleData();
		tempVar.setName(rpc.Argument.getName());
		long roleId = _trole.insert(tempVar);

		// duplicate name check
		BRoleId tempVar2 = new BRoleId();
		tempVar2.setId(roleId);
		if (!_trolename.tryAdd(rpc.Argument.getName(), tempVar2)) {
			return ErrorCode(ResultCodeCreateRoleDuplicateRoleName);
		}

		var account = _taccount.getOrAdd(session.getAccount());
		account.getRoles().add(roleId);

		// initialize role data
		App.Game_Bag.GetBag(roleId).SetCapacity(50);

		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetRoleListRequest(GetRoleList rpc) {
		var session = ProviderUserSession.Get(rpc);

		BAccount account = _taccount.get(session.getAccount());
		if (null != account) {
			for (var roleId : account.getRoles()) {
				BRoleData roleData = _trole.get(roleId);
				if (null != roleData) {
					BRole tempVar = new BRole();
					tempVar.setId(roleId);
					tempVar.setName(roleData.getName());
					rpc.Result.getRoleList().add(tempVar);
				}
			}
			rpc.Result.setLastLoginRoleId(account.getLastLoginRoleId());
		}

		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	protected long ProcessLoginRequest(Login rpc) {
		var session = ProviderUserSession.Get(rpc);

		BAccount account = _taccount.get(session.getAccount());
		if (null == account) {
			return ErrorCode(ResultCodeAccountNotExist);
		}

		account.setLastLoginRoleId(rpc.Argument.getRoleId());
		BRoleData role = _trole.get(rpc.Argument.getRoleId());
		if (null == role) {
			return ErrorCode(ResultCodeRoleNotExist);
		}

		BOnline online = _tonline.getOrAdd(rpc.Argument.getRoleId());
		online.setLinkName(session.getLinkName());
		online.setLinkSid(session.getSessionId());
		online.setState(BOnline.StateOnline);

		online.setReliableNotifyConfirmCount(0);
		online.setReliableNotifyTotalCount(0);
		online.getReliableNotifyMark().clear();
		online.getReliableNotifyQueue().clear();

		var linkSession = (Server.LinkSession)session.getLink().getUserState();
		online.setProviderId(App.Zeze.getConfig().getServerId());
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
		App.getLoad().getLoginCount().incrementAndGet();
		return Procedure.Success;
	}

	@Override
	protected long ProcessReLoginRequest(ReLogin rpc) {
		var session = ProviderUserSession.Get(rpc);

		BAccount account = _taccount.get(session.getAccount());
		if (null == account) {
			return ErrorCode(ResultCodeAccountNotExist);
		}

		if (account.getLastLoginRoleId() != rpc.Argument.getRoleId()) {
			return ErrorCode(ResultCodeNotLastLoginRoleId);
		}

		BRoleData role = _trole.get(rpc.Argument.getRoleId());
		if (null == role) {
			return ErrorCode(ResultCodeRoleNotExist);
		}

		BOnline online = _tonline.get(rpc.Argument.getRoleId());
		if (null == online) {
			return ErrorCode(ResultCodeOnlineDataNotFound);
		}

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

		if (syncResultCode != ResultCodeSuccess) {
			return ErrorCode(syncResultCode);
		}

		App.getLoad().getLoginCount().incrementAndGet();
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
			for (int i = confirmCount; i < online.getReliableNotifyQueue().size(); ++i) {
				notify.Argument.getNotifies().add(online.getReliableNotifyQueue().get(i));
			}
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
	protected long ProcessReliableNotifyConfirmRequest(ReliableNotifyConfirm rpc) {
		var session = ProviderUserSession.Get(rpc);

		BOnline online = _tonline.get(session.getRoleId());
		if (null == online || online.getState() == BOnline.StateOffline) {
			return ErrorCode(ResultCodeOnlineDataNotFound);
		}

		session.SendResponseWhileCommit(rpc); // 同步前提交。
		var syncResultCode = ReliableNotifySync(session, rpc.Argument.getReliableNotifyConfirmCount(), online, false);

		if (ResultCodeSuccess != syncResultCode) {
			return ErrorCode(syncResultCode);
		}

		return Procedure.Success;
	}

	@Override
	protected long ProcessLogoutRequest(Logout rpc) {
		var session = ProviderUserSession.Get(rpc);

		if (session.getRoleId() == null) {
			return ErrorCode(ResultCodeNotLogin);
		}

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

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLogin(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
