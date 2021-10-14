package Zezex.Provider;

import static Zeze.Net.Service.*;
import Zeze.Transaction.*;
import Zezex.*;
import java.util.*;

// auto-generated



public final class ModuleProvider extends AbstractModule {
	public static final int ModuleId = 10001;


	private Game.App App;
	public Game.App getApp() {
		return App;
	}

	public ModuleProvider(Game.App app) {
		App = app;
		// register protocol factory and handles
		getApp().getServer().AddFactoryHandle(655473135, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.AnnounceLinkInfo(), Handle = Service.<AnnounceLinkInfo>MakeHandle(this, this.getClass().getMethod("ProcessAnnounceLinkInfo")), NoProcedure = true});
		getApp().getServer().AddFactoryHandle(655479127, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.Bind()});
		getApp().getServer().AddFactoryHandle(655471205, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.Dispatch(), Handle = Service.<Dispatch>MakeHandle(this, this.getClass().getMethod("ProcessDispatch"))});
		getApp().getServer().AddFactoryHandle(655477748, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.LinkBroken(), Handle = Service.<LinkBroken>MakeHandle(this, this.getClass().getMethod("ProcessLinkBroken"))});
		getApp().getServer().AddFactoryHandle(655455850, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.ModuleRedirect(), Handle = Service.<ModuleRedirect>MakeHandle(this, this.getClass().getMethod("ProcessModuleRedirectRequest"))});
		getApp().getServer().AddFactoryHandle(655479394, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.ModuleRedirectAllRequest(), Handle = Service.<ModuleRedirectAllRequest>MakeHandle(this, this.getClass().getMethod("ProcessModuleRedirectAllRequest"))});
		getApp().getServer().AddFactoryHandle(655465353, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.ModuleRedirectAllResult(), Handle = Service.<ModuleRedirectAllResult>MakeHandle(this, this.getClass().getMethod("ProcessModuleRedirectAllResult"))});
		getApp().getServer().AddFactoryHandle(655476045, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.SendConfirm(), Handle = Service.<SendConfirm>MakeHandle(this, this.getClass().getMethod("ProcessSendConfirm"))});
		getApp().getServer().AddFactoryHandle(655453724, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.Transmit(), Handle = Service.<Transmit>MakeHandle(this, this.getClass().getMethod("ProcessTransmit")), NoProcedure = true});
		getApp().getServer().AddFactoryHandle(655436306, new Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.UnBind()});
		// register table
	}

	@Override
	public void UnRegister() {
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__ = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655473135, tempOut__);
	_ = tempOut__.outArgValue;
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__2 = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655479127, tempOut__2);
	_ = tempOut__2.outArgValue;
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__3 = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655471205, tempOut__3);
	_ = tempOut__3.outArgValue;
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__4 = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655477748, tempOut__4);
	_ = tempOut__4.outArgValue;
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__5 = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655455850, tempOut__5);
	_ = tempOut__5.outArgValue;
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__6 = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655479394, tempOut__6);
	_ = tempOut__6.outArgValue;
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__7 = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655465353, tempOut__7);
	_ = tempOut__7.outArgValue;
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__8 = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655476045, tempOut__8);
	_ = tempOut__8.outArgValue;
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__9 = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655453724, tempOut__9);
	_ = tempOut__9.outArgValue;
		TValue _;
		tangible.OutObject<Service.ProtocolFactoryHandle> tempOut__10 = new tangible.OutObject<Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(655436306, tempOut__10);
	_ = tempOut__10.outArgValue;
	}



	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public void Start(Game.App app) {
	}

	public void Stop(Game.App app) {
	}

	private void SendKick(AsyncSocket sender, long linkSid, int code, String desc) {
		var p = new Kick();
		p.getArgument().Linksid = linkSid;
		p.getArgument().Code = code;
		p.getArgument().Desc = desc;
		p.Send(sender);
	}

	@Override
	public int ProcessDispatch(Dispatch p) {
		try {
			var factoryHandle = Game.App.getInstance().getServer().FindProtocolFactoryHandle(p.getArgument().getProtocolType());
			if (null == factoryHandle) {
				SendKick(p.Sender, p.getArgument().getLinkSid(), BKick.ErrorProtocolUnkown, "unknown protocol");
				return Procedure.LogicError;
			}
			var p2 = factoryHandle.Factory();
			p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.getArgument().getProtocolData()));
			p2.Sender = p.Sender;

			var session = new Game.Login.Session(p.getArgument().getAccount(), p.getArgument().getStates(), p.Sender, p.getArgument().getLinkSid());

			p2.UserState = session;
			if (Transaction.Current != null) {
				// 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
				Transaction.Current.TopProcedure.ActionName = p2.getClass().getName();
				Transaction.Current.TopProcedure.UserState = p2.UserState;
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle(p2), p2, (p, code) -> {
						p.ResultCode = code;
						session.SendResponse(p);
				});
			}

			if (p2.Sender.Service.Zeze == null || factoryHandle.NoProcedure) {
				// 应用框架不支持事务或者协议配置了"不需要事务”
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle(p2), p2, (p, code) -> {
						p.ResultCode = code;
						session.SendResponse(p);
				});
			}

			// 创建存储过程并且在当前线程中调用。
			return Zeze.Util.Task.Call(p2.Sender.Service.Zeze.NewProcedure(() -> factoryHandle.Handle(p2), p2.getClass().getName(), p2.UserState), p2, (p, code) -> {
					p.ResultCode = code;
					session.SendResponse(p);
			});
		}
		catch (RuntimeException ex) {
			SendKick(p.Sender, p.getArgument().getLinkSid(), BKick.ErrorProtocolException, ex.toString());
			throw ex;
		}
	}

	@Override
	public int ProcessLinkBroken(LinkBroken protocol) {
		// 目前仅需设置online状态。
		if (protocol.getArgument().getStates().Count > 0) {
			var roleId = protocol.getArgument().getStates().get(0);
			Game.App.getInstance().getGameLogin().getOnlines().OnLinkBroken(roleId);
		}
		return Procedure.Success;
	}

	@Override
	public int ProcessModuleRedirectRequest(ModuleRedirect rpc) {
		try {
			// replace RootProcedure.ActionName. 为了统计和日志输出。
			Transaction.Current.TopProcedure.ActionName = rpc.getArgument().getMethodFullName();

			rpc.getResult().ModuleId = rpc.getArgument().getModuleId();
			rpc.getResult().ServerId = getApp().getZeze().Config.ServerId;
			Object handle;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
			if (false == Game.ModuleRedirect.Instance.Handles.TryGetValue(rpc.getArgument().getMethodFullName(), out handle)) {
				rpc.SendResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
				return Procedure.LogicError;
			}
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
			var(ReturnCode, Params) = handle(rpc.SessionId, rpc.Argument.HashCode, rpc.Argument.Params, rpc.Result.Actions);
			rpc.getResult().ReturnCode = ReturnCode;
			if (ReturnCode == Procedure.Success) {
				rpc.getResult().Params = Params;
			}
			// rpc 成功了，具体handle结果还需要看ReturnCode。
			rpc.SendResultCode(ModuleRedirect.ResultCodeSuccess);
			return rpc.getResult().getReturnCode();
		}
		catch (RuntimeException e) {
			rpc.SendResultCode(ModuleRedirect.ResultCodeHandleException);
			throw e;
		}
	}

	private void SendResultIfSizeExceed(AsyncSocket sender, ModuleRedirectAllResult result) {
		int size = 0;
		for (var hashResult : result.getArgument().getHashs().Values) {
			size += hashResult.Params.Count;
			for (var hashActions : hashResult.Actions) {
				size += hashActions.Params.Count;
			}
		}
		if (size > 2 * 1024 * 1024) { // 2M
			result.Send(sender);
			result.getArgument().getHashs().Clear();
		}
	}

	@Override
	public int ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest protocol) {
		var result = new ModuleRedirectAllResult();
		try {
			// replace RootProcedure.ActionName. 为了统计和日志输出。
			Transaction.Current.TopProcedure.ActionName = protocol.getArgument().getMethodFullName();

			// common parameters for result
			result.getArgument().ModuleId = protocol.getArgument().getModuleId();
			result.getArgument().ServerId = getApp().getZeze().Config.ServerId;
			result.getArgument().SourceProvider = protocol.getArgument().getSourceProvider();
			result.getArgument().SessionId = protocol.getArgument().getSessionId();
			result.getArgument().MethodFullName = protocol.getArgument().getMethodFullName();

			Object handle;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
			if (false == Game.ModuleRedirect.Instance.Handles.TryGetValue(protocol.getArgument().getMethodFullName(), out handle)) {
				result.ResultCode = ModuleRedirect.ResultCodeMethodFullNameNotFound;
				// 失败了，需要把hash返回。此时是没有处理结果的。
				for (var hash : protocol.getArgument().getHashCodes()) {
					BModuleRedirectAllHash tempVar = new BModuleRedirectAllHash();
					tempVar.setReturnCode(Procedure.NotImplement);
					result.getArgument().getHashs().Add(hash, tempVar);
				}
				result.Send(protocol.Sender);
				return Procedure.LogicError;
			}
			result.ResultCode = ModuleRedirect.ResultCodeSuccess;

			for (var hash : protocol.getArgument().getHashCodes()) {
				// 嵌套存储过程，某个分组处理失败不影响其他分组。
				var hashResult = new BModuleRedirectAllHash();
				Binary Params = null;
				hashResult.ReturnCode = getApp().getZeze().NewProcedure(() -> {
						var(_ReturnCode, _Params) = handle(protocol.Argument.SessionId, hash, protocol.Argument.Params, hashResult.Actions);
						Params = _Params;
						return _ReturnCode;
				}, Transaction.Current.TopProcedure.ActionName, null).Call();

				// 单个分组处理失败继续执行。XXX
				if (hashResult.getReturnCode() == Procedure.Success) {
					hashResult.Params = Params;
				}
				result.getArgument().getHashs().Add(hash, hashResult);
				SendResultIfSizeExceed(protocol.Sender, result);
			}

			// send remain
			if (result.getArgument().getHashs().Count > 0) {
				result.Send(protocol.Sender);
			}
			return Procedure.Success;
		}
		catch (RuntimeException e) {
			result.ResultCode = ModuleRedirect.ResultCodeHandleException;
			result.Send(protocol.Sender);
			throw e;
		}
	}

	public static class ModuleRedirectAllContext extends Zeze.Net.Service.ManualContext {
		private String MethodFullName;
		public final String getMethodFullName() {
			return MethodFullName;
		}
		private HashSet<Integer> HashCodes = new HashSet<Integer> ();
		public final HashSet<Integer> getHashCodes() {
			return HashCodes;
		}
		private tangible.Action1Param<ModuleRedirectAllContext> OnHashEnd;
		public final tangible.Action1Param<ModuleRedirectAllContext> getOnHashEnd() {
			return OnHashEnd;
		}
		public final void setOnHashEnd(tangible.Action1Param<ModuleRedirectAllContext> value) {
			OnHashEnd = value;
		}

		public ModuleRedirectAllContext(int concurrentLevel, String methodFullName) {
			for (int hash = 0; hash < concurrentLevel; ++hash) {
				getHashCodes().add(hash);
			}
			MethodFullName = methodFullName;
		}

		@Override
		public void OnRemoved() {
			synchronized (this) {
				if (getOnHashEnd() != null) {
					getOnHashEnd().Invoke(this);
				}
				setOnHashEnd(::null);
			}
		}

		/** 
		 调用这个方法处理hash分组结果，真正的处理代码在action中实现。
		 1) 在锁内执行；
		 2) 需要时初始化UserState并传给action；
		 3) 处理完成时删除Context
		*/
		public final <T> int ProcessHash(int hash, tangible.Func0Param<T> factory, tangible.Func1Param<T, Integer> action) {
			synchronized (this) {
				try {
					if (null == getUserState()) {
						setUserState(factory.invoke());
					}
					return action.invoke((T)getUserState());
				}
				finally {
					getHashCodes().remove((Integer)hash); // 如果不允许一个hash分组处理措辞，把这个移到开头并判断结果。
					if (getHashCodes().isEmpty()) {
						Game.App.getInstance().getServer().<Service.ManualContext>TryRemoveManualContext(getSessionId());
					}
				}
			}
		}

		// 这里处理真正redirect发生时，从远程返回的结果。
		public final void ProcessResult(ModuleRedirectAllResult result) {
			for (var h : result.getArgument().getHashs()) {
				// 嵌套存储过程，单个分组的结果处理不影响其他分组。
				// 不判断单个分组的处理结果，错误也继续执行其他分组。XXX
				Game.App.getInstance().getZeze().NewProcedure(() -> ProcessHashResult(h.Key, h.Value.ReturnCode, h.Value.Params, h.Value.Actions), getMethodFullName(), null).Call();
			}
		}

		// 生成代码实现。see Game.ModuleRedirect.cs
		public int ProcessHashResult(int _hash_, int _returnCode_, Binary _params, List<Zezex.Provider.BActionParam> _actions_) {
			return Procedure.NotImplement;
		}
	}

	@Override
	public int ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) {
		// replace RootProcedure.ActionName. 为了统计和日志输出。
		Transaction.Current.TopProcedure.ActionName = protocol.getArgument().getMethodFullName();
		if (getApp().getServer().<ModuleRedirectAllContext>TryGetManualContext(protocol.getArgument().getSessionId()) != null) {
			getApp().getServer().<ModuleRedirectAllContext>TryGetManualContext(protocol.getArgument().getSessionId()).ProcessResult(protocol);
		}
		return Procedure.Success;
	}

	@Override
	public int ProcessTransmit(Transmit protocol) {
		getApp().getGameLogin().getOnlines().ProcessTransmit(protocol.getArgument().getSender(), protocol.getArgument().getActionName(), protocol.getArgument().getRoles().keySet());
		return Procedure.Success;
	}

	@Override
	public int ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol) {
		var linkSession = protocol.Sender.UserState instanceof Game.Server.LinkSession ? (Game.Server.LinkSession)protocol.Sender.UserState : null;
		linkSession.Setup(protocol.getArgument().getLinkId(), protocol.getArgument().getProviderSessionId());
		return Procedure.Success;
	}

	@Override
	public int ProcessSendConfirm(SendConfirm protocol) {
		var linkSession = protocol.Sender.UserState instanceof Game.Server.LinkSession ? (Game.Server.LinkSession)protocol.Sender.UserState : null;
		if (getApp().getServer().<Game.Login.Onlines.ConfirmContext>TryGetManualContext(protocol.getArgument().getConfirmSerialId()) != null) {
			getApp().getServer().<Game.Login.Onlines.ConfirmContext>TryGetManualContext(protocol.getArgument().getConfirmSerialId()).ProcessLinkConfirm(linkSession.getName());
		}
		// linkName 也可以从 protocol.Sender.Connector.Name 获取。
		return Procedure.Success;
	}
}