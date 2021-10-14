package Zezex.Linkd;

import Zezex.*;

// auto-generated


public final class ModuleLinkd extends AbstractModule {
	public static final int ModuleId = 10000;


	private App App;
	public App getApp() {
		return App;
	}

	public ModuleLinkd(App app) {
		App = app;
		// register protocol factory and handles
		getApp().getLinkdService().AddFactoryHandle(655394483, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Linkd.Auth(), Handle = Zeze.Net.Service.<Auth>MakeHandle(this, this.getClass().getMethod("ProcessAuthRequest"))});
		getApp().getLinkdService().AddFactoryHandle(655406763, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Linkd.KeepAlive(), Handle = Zeze.Net.Service.<KeepAlive>MakeHandle(this, this.getClass().getMethod("ProcessKeepAlive"))});
		// register table
	}

	@Override
	public void UnRegister() {
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__ = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getLinkdService().getFactorys().TryRemove(655394483, tempOut__);
	_ = tempOut__.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__2 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getLinkdService().getFactorys().TryRemove(655406763, tempOut__2);
	_ = tempOut__2.outArgValue;
	}



	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	@Override
	public int ProcessAuthRequest(Auth rpc) {
		/*
		BAccount account = _taccount.Get(protocol.Argument.Account);
		if (null == account || false == account.Token.Equals(protocol.Argument.Token))
		{
		    result.Send(protocol.Sender);
		    return Zeze.Transaction.Procedure.LogicError;
		}

		Game.App.Instance.LinkdService.GetSocket(account.SocketSessionId)?.Dispose(); // kick, 最好发个协议再踢。如果允许多个连接，去掉这行。
		account.SocketSessionId = protocol.Sender.SessionId;
		*/
		Object tempVar = rpc.getSender().UserState;
		var linkSession = tempVar instanceof LinkSession ? (LinkSession)tempVar : null;
		linkSession.setAccount(rpc.getArgument().getAccount());
		rpc.SendResultCode(Auth.Success);

		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessKeepAlive(KeepAlive protocol) {
		var linkSession = protocol.Sender.UserState instanceof LinkSession ? (LinkSession)protocol.Sender.UserState : null;
		if (null == linkSession) {
			// handshake 完成之前不可能回收得到 keepalive，先这样处理吧。
			protocol.Sender.Close(null);
			return Zeze.Transaction.Procedure.LogicError;
		}
		linkSession.KeepAlive();
		protocol.Sender.Send(protocol); // send back;
		return Zeze.Transaction.Procedure.Success;
	}
}