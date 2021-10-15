package Game.Login;

import Zeze.Transaction.*;
import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleLogin extends AbstractModule {
	public void Start(App app) {
		setOnlines(new Onlines(_tonline));
	}

	public void Stop(App app) {
	}

	private Onlines Onlines;
	public Onlines getOnlines() {
		return Onlines;
	}
	private void setOnlines(Onlines value) {
		Onlines = value;
	}

	@Override
	public int ProcessCreateRoleRequest(CreateRole rpc) {
		Session session = Session.Get(rpc);

		BRoleData tempVar = new BRoleData();
		tempVar.setName(rpc.getArgument().getName());
		long roleid = _trole.Insert(tempVar);

		// duplicate name check
		BRoleId tempVar2 = new BRoleId();
		tempVar2.setId(roleid);
		if (false == _trolename.TryAdd(rpc.getArgument().getName(), tempVar2)) {
			return ReturnCode(ResultCodeCreateRoleDuplicateRoleName);
		}

		var account = _taccount.GetOrAdd(session.getAccount());
		account.getRoles().Add(roleid);

		// initialize role data
		App.getInstance().getGameBag().GetBag(roleid).SetCapacity(50);

		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	public int ProcessGetRoleListRequest(GetRoleList rpc) {
		Session session = Session.Get(rpc);

		BAccount account = _taccount.Get(session.getAccount());
		if (null != account) {
			for (var roleId : account.getRoles()) {
				BRoleData roleData = _trole.Get(roleId);
				if (null != roleData) {
					BRole tempVar = new BRole();
					tempVar.setId(roleId);
					tempVar.setName(roleData.getName());
					rpc.getResult().getRoleList().Add(tempVar);
				}
			}
			rpc.getResult().LastLoginRoleId = account.getLastLoginRoleId();
		}

		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	public int ProcessLoginRequest(Login rpc) {
		Session session = Session.Get(rpc);

		BAccount account = _taccount.Get(session.getAccount());
		if (null == account) {
			return ReturnCode(ResultCodeAccountNotExist);
		}

		account.LastLoginRoleId = rpc.getArgument().getRoleId();
		BRoleData role = _trole.Get(rpc.getArgument().getRoleId());
		if (null == role) {
			return ReturnCode(ResultCodeRoleNotExist);
		}

		BOnline online = _tonline.GetOrAdd(rpc.getArgument().getRoleId());
		online.LinkName = session.getLinkName();
		online.LinkSid = session.getSessionId();
		online.State = BOnline.StateOnline;

		online.ReliableNotifyConfirmCount = 0;
		online.ReliableNotifyTotalCount = 0;
		online.getReliableNotifyMark().Clear();
		online.getReliableNotifyQueue().Clear();

		var linkSession = session.getLink().UserState instanceof Server.LinkSession ? (Server.LinkSession)session.getLink().UserState : null;
		online.ProviderId = getApp().getZeze().Config.ServerId;
		online.ProviderSessionId = linkSession.getProviderSessionId();

		// 先提交结果再设置状态。
		// see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
		session.SendResponseWhileCommit(rpc);
		Transaction.Current.RunWhileCommit(() -> {
				var setUserState = new Zezex.Provider.SetUserState();
				setUserState.getArgument().LinkSid = session.getSessionId();
				setUserState.getArgument().getStates().Add(rpc.getArgument().getRoleId());
				rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		getApp().getLoad().getLoginCount().IncrementAndGet();
		return Procedure.Success;
	}

	@Override
	public int ProcessReLoginRequest(ReLogin rpc) {
		Session session = Session.Get(rpc);

		BAccount account = _taccount.Get(session.getAccount());
		if (null == account) {
			return ReturnCode(ResultCodeAccountNotExist);
		}

		if (account.getLastLoginRoleId() != rpc.getArgument().getRoleId()) {
			return ReturnCode(ResultCodeNotLastLoginRoleId);
		}

		BRoleData role = _trole.Get(rpc.getArgument().getRoleId());
		if (null == role) {
			return ReturnCode(ResultCodeRoleNotExist);
		}

		BOnline online = _tonline.Get(rpc.getArgument().getRoleId());
		if (null == online) {
			return ReturnCode(ResultCodeOnlineDataNotFound);
		}

		online.LinkName = session.getLinkName();
		online.LinkSid = session.getSessionId();
		online.State = BOnline.StateOnline;

		// 先发结果，再发送同步数据（ReliableNotifySync）。
		// 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
		session.SendResponseWhileCommit(rpc);
		Transaction.Current.RunWhileCommit(() -> {
				var setUserState = new Zezex.Provider.SetUserState();
				setUserState.getArgument().LinkSid = session.getSessionId();
				setUserState.getArgument().getStates().Add(rpc.getArgument().getRoleId());
				rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		var syncResultCode = ReliableNotifySync(session, rpc.getArgument().getReliableNotifyConfirmCount(), online);

		if (syncResultCode != ResultCodeSuccess) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return ReturnCode((ushort)syncResultCode);
			return ReturnCode((short)syncResultCode);
		}

		getApp().getLoad().getLoginCount().IncrementAndGet();
		return Procedure.Success;
	}


	private int ReliableNotifySync(Session session, long ReliableNotifyConfirmCount, BOnline online) {
		return ReliableNotifySync(session, ReliableNotifyConfirmCount, online, true);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: private int ReliableNotifySync(Session session, long ReliableNotifyConfirmCount, BOnline online, bool sync = true)
	private int ReliableNotifySync(Session session, long ReliableNotifyConfirmCount, BOnline online, boolean sync) {
		if (ReliableNotifyConfirmCount < online.getReliableNotifyConfirmCount() || ReliableNotifyConfirmCount > online.getReliableNotifyTotalCount() || ReliableNotifyConfirmCount - online.getReliableNotifyConfirmCount() > online.getReliableNotifyQueue().Count) {
			return ResultCodeReliableNotifyConfirmCountOutOfRange;
		}

		int confirmCount = (int)(ReliableNotifyConfirmCount - online.getReliableNotifyConfirmCount());

		if (sync) {
			var notify = new SReliableNotify();
			notify.getArgument().ReliableNotifyTotalCountStart = ReliableNotifyConfirmCount;
			for (int i = confirmCount; i < online.getReliableNotifyQueue().Count; ++i) {
				notify.getArgument().getNotifies().Add(online.getReliableNotifyQueue().get(i));
			}
			session.SendResponseWhileCommit(notify);
		}
		online.getReliableNotifyQueue().RemoveRange(0, confirmCount);
		online.ReliableNotifyConfirmCount = ReliableNotifyConfirmCount;
		return ResultCodeSuccess;
	}

	@Override
	public int ProcessReliableNotifyConfirmRequest(ReliableNotifyConfirm rpc) {
		Session session = Session.Get(rpc);

		BOnline online = _tonline.Get(session.getRoleId().longValue());
		if (null == online || online.getState() == BOnline.StateOffline) {
			return ReturnCode(ResultCodeOnlineDataNotFound);
		}

		session.SendResponseWhileCommit(rpc); // 同步前提交。
		var syncResultCode = ReliableNotifySync(session, rpc.getArgument().getReliableNotifyConfirmCount(), online, false);

		if (ResultCodeSuccess != syncResultCode) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return ReturnCode((ushort)syncResultCode);
			return ReturnCode((short)syncResultCode);
		}

		return Procedure.Success;
	}

	@Override
	public int ProcessLogoutRequest(Logout rpc) {
		Session session = Session.Get(rpc);

		if (session.getRoleId() == null) {
			return ReturnCode(ResultCodeNotLogin);
		}

		_tonline.Remove(session.getRoleId().longValue());

		// 先设置状态，再发送Logout结果。
		Transaction.Current.RunWhileCommit(() -> {
				var setUserState = new Zezex.Provider.SetUserState();
				setUserState.getArgument().LinkSid = session.getSessionId();
				rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		session.SendResponseWhileCommit(rpc);
		// 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
		// App.Load.LogoutCount.IncrementAndGet();
		return Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
	public static final int ModuleId = 1;

	private taccount _taccount = new taccount();
	private tonline _tonline = new tonline();
	private trole _trole = new trole();
	private trolename _trolename = new trolename();

	private App App;
	public App getApp() {
		return App;
	}

	public ModuleLogin(App app) {
		App = app;
		// register protocol factory and handles
		getApp().getServer().AddFactoryHandle(108094, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Login.CreateRole(), Handle = Zeze.Net.Service.<CreateRole>MakeHandle(this, this.getClass().getMethod("ProcessCreateRoleRequest"))});
		getApp().getServer().AddFactoryHandle(91931, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Login.GetRoleList(), Handle = Zeze.Net.Service.<GetRoleList>MakeHandle(this, this.getClass().getMethod("ProcessGetRoleListRequest"))});
		getApp().getServer().AddFactoryHandle(83324, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Login.Login(), Handle = Zeze.Net.Service.<Login>MakeHandle(this, this.getClass().getMethod("ProcessLoginRequest"))});
		getApp().getServer().AddFactoryHandle(113969, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Login.Logout(), Handle = Zeze.Net.Service.<Logout>MakeHandle(this, this.getClass().getMethod("ProcessLogoutRequest"))});
		getApp().getServer().AddFactoryHandle(80485, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Login.ReliableNotifyConfirm(), Handle = Zeze.Net.Service.<ReliableNotifyConfirm>MakeHandle(this, this.getClass().getMethod("ProcessReliableNotifyConfirmRequest"))});
		getApp().getServer().AddFactoryHandle(108643, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Login.ReLogin(), Handle = Zeze.Net.Service.<ReLogin>MakeHandle(this, this.getClass().getMethod("ProcessReLoginRequest"))});
		// register table
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_taccount.Name).DatabaseName, _taccount);
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_tonline.Name).DatabaseName, _tonline);
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_trole.Name).DatabaseName, _trole);
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_trolename.Name).DatabaseName, _trolename);
	}

	@Override
	public void UnRegister() {
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__ = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(108094, tempOut__);
	_ = tempOut__.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__2 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(91931, tempOut__2);
	_ = tempOut__2.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__3 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(83324, tempOut__3);
	_ = tempOut__3.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__4 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(113969, tempOut__4);
	_ = tempOut__4.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__5 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(80485, tempOut__5);
	_ = tempOut__5.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__6 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(108643, tempOut__6);
	_ = tempOut__6.outArgValue;
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_taccount.Name).DatabaseName, _taccount);
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_tonline.Name).DatabaseName, _tonline);
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_trole.Name).DatabaseName, _trole);
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_trolename.Name).DatabaseName, _trolename);
	}
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}