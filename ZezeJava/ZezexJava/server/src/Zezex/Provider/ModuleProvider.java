package Zezex.Provider;

import java.util.HashSet;
import java.util.List;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleProvider extends AbstractModule {
	// private static final Logger logger = LogManager.getLogger(ModuleProvider.class);

	public void Start(Game.App app) {
	}

	public void Stop(Game.App app) {
	}

	private void SendKick(AsyncSocket sender, long linkSid, int code, String desc) {
		var p = new Kick();
		p.Argument.setLinksid(linkSid);
		p.Argument.setCode(code);
		p.Argument.setDesc(desc);
		p.Send(sender);
	}

	@Override
	protected long ProcessDispatch(Dispatch p) throws Throwable {
		try {
			var factoryHandle = App.Server.FindProtocolFactoryHandle(p.Argument.getProtocolType());
			if (null == factoryHandle) {
				SendKick(p.getSender(), p.Argument.getLinkSid(), BKick.ErrorProtocolUnkown, "unknown protocol");
				return Procedure.LogicError;
			}
			var p2 = factoryHandle.Factory.create();
			p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.getProtocolData()));
			p2.setSender(p.getSender());

			var session = new Game.Login.Session(p.Argument.getAccount(), p.Argument.getStates(), p.getSender(), p.Argument.getLinkSid());

			p2.setUserState(session);
			if (Transaction.getCurrent() != null) {
				// 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
				Transaction.getCurrent().getTopProcedure().setActionName(p2.getClass().getName());
				Transaction.getCurrent().getTopProcedure().setUserState(p2.getUserState());
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle.handleProtocol(p2), p2, (p3, code) -> {
						p3.setResultCode(code);
						session.SendResponse(p3);
				});
			}

			if (p2.getSender().getService().getZeze() == null || factoryHandle.Level == TransactionLevel.None) {
				// 应用框架不支持事务或者协议配置了"不需要事务”
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle.handleProtocol(p2), p2, (p3, code) -> {
						p3.setResultCode(code);
						session.SendResponse(p3);
				});
			}

			// 创建存储过程并且在当前线程中调用。
			return Zeze.Util.Task.Call(
					p2.getSender().getService().getZeze().NewProcedure(
							() -> factoryHandle.Handle.handleProtocol(p2), p2.getClass().getName(), factoryHandle.Level, p2.getUserState()),
					p2, (p3, code) -> { p3.setResultCode(code); session.SendResponse(p3);
					});
		}
		catch (Throwable ex) {
			SendKick(p.getSender(), p.Argument.getLinkSid(), BKick.ErrorProtocolException, ex.toString());
			throw ex;
		}
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken protocol) throws Throwable {
		// 目前仅需设置online状态。
		if (false == protocol.Argument.getStates().isEmpty()) {
			var roleId = protocol.Argument.getStates().get(0);
			Game.App.getInstance().Game_Login.getOnlines().OnLinkBroken(roleId);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessModuleRedirectRequest(ModuleRedirect rpc) throws Throwable {
		try {
			// replace RootProcedure.ActionName. 为了统计和日志输出。
			Transaction.getCurrent().getTopProcedure().setActionName(rpc.Argument.getMethodFullName());

			rpc.Result.setModuleId(rpc.Argument.getModuleId());
			rpc.Result.setServerId(App.Zeze.getConfig().getServerId());
			var handle = Zezex.ModuleRedirect.Instance.Handles.get(rpc.Argument.getMethodFullName());
			if (null == handle) {
				rpc.SendResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
				return Procedure.LogicError;
			}

			var rp = handle.call(rpc.SessionId, rpc.Argument.getHashCode(), rpc.Argument.getParams(), rpc.Result.getActions());
			rpc.Result.setReturnCode(rp.ReturnCode);
			if (rp.ReturnCode == Procedure.Success) {
				rpc.Result.setParams(rp.EncodedParameters);
			}
			// rpc 成功了，具体handle结果还需要看ReturnCode。
			rpc.SendResultCode(ModuleRedirect.ResultCodeSuccess);
			return rpc.Result.getReturnCode();
		}
		catch (Throwable e) {
			rpc.SendResultCode(ModuleRedirect.ResultCodeHandleException);
			throw e;
		}
	}

	private void SendResultIfSizeExceed(AsyncSocket sender, ModuleRedirectAllResult result) {
		int size = 0;
		for (var hashResult : result.Argument.getHashs().values()) {
			size += hashResult.getParams().size();
			for (var hashActions : hashResult.getActions()) {
				size += hashActions.getParams().size();
			}
		}
		if (size > 2 * 1024 * 1024) { // 2M
			result.Send(sender);
			result.Argument.getHashs().clear();
		}
	}

	@Override
	protected long ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest protocol) throws Throwable {
		var result = new ModuleRedirectAllResult();
		try {
			// replace RootProcedure.ActionName. 为了统计和日志输出。
			Transaction.getCurrent().getTopProcedure().setActionName(protocol.Argument.getMethodFullName());

			// common parameters for result
			result.Argument.setModuleId(protocol.Argument.getModuleId());
			result.Argument.setServerId(App.Zeze.getConfig().getServerId());
			result.Argument.setSourceProvider(protocol.Argument.getSourceProvider());
			result.Argument.setSessionId(protocol.Argument.getSessionId());
			result.Argument.setMethodFullName(protocol.Argument.getMethodFullName());

			var handle = Zezex.ModuleRedirect.Instance.Handles.get(protocol.Argument.getMethodFullName());
			if (null == handle) {
				result.setResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
				// 失败了，需要把hash返回。此时是没有处理结果的。
				for (var hash : protocol.Argument.getHashCodes()) {
					BModuleRedirectAllHash tempVar = new BModuleRedirectAllHash();
					tempVar.setReturnCode(Procedure.NotImplement);
					result.Argument.getHashs().put(hash, tempVar);
				}
				result.Send(protocol.getSender());
				return Procedure.LogicError;
			}
			result.setResultCode(ModuleRedirect.ResultCodeSuccess);

			for (var hash : protocol.Argument.getHashCodes()) {
				// 嵌套存储过程，某个分组处理失败不影响其他分组。
				var hashResult = new BModuleRedirectAllHash();
				final var Params = new Zeze.Util.OutObject<Binary>();
				Params.Value = null;
				hashResult.setReturnCode(App.Zeze.NewProcedure(() -> {
						var rp = handle.call(
								protocol.Argument.getSessionId(), hash, protocol.Argument.getParams(), hashResult.getActions());
						Params.Value = rp.EncodedParameters;
						return rp.ReturnCode;
				}, Transaction.getCurrent().getTopProcedure().getActionName()).Call());

				// 单个分组处理失败继续执行。XXX
				if (hashResult.getReturnCode() == Procedure.Success) {
					hashResult.setParams(Params.Value);
				}
				result.Argument.getHashs().put(hash, hashResult);
				SendResultIfSizeExceed(protocol.getSender(), result);
			}

			// send remain
			if (result.Argument.getHashs().size() > 0) {
				result.Send(protocol.getSender());
			}
			return Procedure.Success;
		}
		catch (Throwable e) {
			result.setResultCode(ModuleRedirect.ResultCodeHandleException);
			result.Send(protocol.getSender());
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
		private Zezex.RedirectAllDoneHandle OnHashEnd;
		public final Zezex.RedirectAllDoneHandle getOnHashEnd() {
			return OnHashEnd;
		}
		public final void setOnHashEnd(Zezex.RedirectAllDoneHandle value) {
			OnHashEnd = value;
		}

		public ModuleRedirectAllContext(int concurrentLevel, String methodFullName) {
			for (int hash = 0; hash < concurrentLevel; ++hash) {
				getHashCodes().add(hash);
			}
			MethodFullName = methodFullName;
		}

		@Override
		public void OnRemoved() throws Throwable {
			synchronized (this) {
				if (OnHashEnd != null) {
					OnHashEnd.handle(this);
				}
				OnHashEnd = null;
			}
		}

		/**
		 调用这个方法处理hash分组结果，真正的处理代码在action中实现。
		 1) 在锁内执行；
		 2) 需要时初始化UserState并传给action；
		 3) 处理完成时删除Context
		*/
		@SuppressWarnings("unchecked")
		public final <T> long ProcessHash(int hash, Zeze.Util.Factory<T> factory, Zeze.Util.Func1<T, Long> action) throws Throwable {
			synchronized (this) {
				try {
					if (null == getUserState()) {
						setUserState(factory.create());
					}
					return action.call((T)getUserState());
				}
				finally {
					HashCodes.remove(hash); // 如果不允许一个hash分组处理措辞，把这个移到开头并判断结果。
					if (HashCodes.isEmpty()) {
						Game.App.getInstance().Server.TryRemoveManualContext(getSessionId());
					}
				}
			}
		}

		// 这里处理真正redirect发生时，从远程返回的结果。
		public final void ProcessResult(ModuleRedirectAllResult result) throws Throwable {
			for (var h : result.Argument.getHashs().entrySet()) {
				// 嵌套存储过程，单个分组的结果处理不影响其他分组。
				// 不判断单个分组的处理结果，错误也继续执行其他分组。XXX
				Game.App.getInstance().Zeze.NewProcedure(() -> ProcessHashResult(
						h.getKey(), h.getValue().getReturnCode(), h.getValue().getParams(), h.getValue().getActions()),
						getMethodFullName()).Call();
			}
		}

		// 生成代码实现。see Zezex.ModuleRedirect.cs
		public long ProcessHashResult(int _hash_, long _returnCode_, Binary _params, List<Zezex.Provider.BActionParam> _actions_) throws Throwable {
			return Procedure.NotImplement;
		}
	}

	@Override
	protected long ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) throws Throwable {
		// replace RootProcedure.ActionName. 为了统计和日志输出。
		Transaction.getCurrent().getTopProcedure().setActionName(protocol.Argument.getMethodFullName());
		var ctx = App.Server.<ModuleRedirectAllContext>TryGetManualContext(protocol.Argument.getSessionId());
		if (ctx != null) {
			ctx.ProcessResult(protocol);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessTransmit(Transmit p) throws Throwable {
		Zeze.Serialize.Serializable parameter = null;
		if (false == p.Argument.getParameterBeanName().isEmpty())
		{
			var factory = App.Game_Login.getOnlines().getTransmitParameterFactorys().get(p.Argument.getParameterBeanName());
			if (null == factory)
				return ErrorCode(ErrorTransmitParameterFactoryNotFound);

			parameter = factory.call(p.Argument.getParameterBeanName());
		}
		App.Game_Login.getOnlines().ProcessTransmit(
				p.Argument.getSender(),
				p.Argument.getActionName(),
				p.Argument.getRoles().keySet(),
				parameter);
		return Procedure.Success;
	}

	@Override
	protected long ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol) throws Throwable {
		var linkSession = (Game.Server.LinkSession)protocol.getSender().getUserState();
		linkSession.Setup(protocol.Argument.getLinkId(), protocol.Argument.getProviderSessionId());
		return Procedure.Success;
	}

	@Override
	protected long ProcessSendConfirm(SendConfirm protocol) throws Throwable {
		var linkSession = (Game.Server.LinkSession)protocol.getSender().getUserState();
		var ctx = App.Server.<Game.Login.Onlines.ConfirmContext>TryGetManualContext(
				protocol.Argument.getConfirmSerialId());
		if (ctx != null) {
			ctx.ProcessLinkConfirm(linkSession.getName());
		}
		// linkName 也可以从 protocol.Sender.Connector.Name 获取。
		return Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleProvider(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
