package Zezex.Provider;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleProvider extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleProvider.class);

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
	public int ProcessDispatch(Protocol _p) {
		var p = (Dispatch)_p;
		try {
			var factoryHandle = App.Server.FindProtocolFactoryHandle(p.Argument.getProtocolType());
			if (null == factoryHandle) {
				SendKick(p.getSender(), p.Argument.getLinkSid(), BKick.ErrorProtocolUnkown, "unknown protocol");
				return Procedure.LogicError;
			}
			var p2 = factoryHandle.Factory.create();
			p2.Service = p.Service;
			p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.getProtocolData()));
			p2.setSender(p.getSender());

			var session = new Game.Login.Session(p.Argument.getAccount(), p.Argument.getStates(), p.getSender(), p.Argument.getLinkSid());

			p2.setUserState(session);
			if (Transaction.getCurrent() != null) {
				// 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
				Transaction.getCurrent().getTopProcedure().setActionName(p2.getClass().getName());
				Transaction.getCurrent().getTopProcedure().setUserState(p2.getUserState());
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p2), p2, (p3, code) -> {
						p3.setResultCode(code);
						session.SendResponse(p3);
				});
			}

			if (p2.getSender().getService().getZeze() == null || factoryHandle.NoProcedure) {
				// 应用框架不支持事务或者协议配置了"不需要事务”
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p2), p2, (p3, code) -> {
						p3.setResultCode(code);
						session.SendResponse(p3);
				});
			}

			// 创建存储过程并且在当前线程中调用。
			return Zeze.Util.Task.Call(
					p2.getSender().getService().getZeze().NewProcedure(
							() -> factoryHandle.Handle.handle(p2), p2.getClass().getName(), p2.getUserState()),
					p2, (p3, code) -> { p3.setResultCode(code); session.SendResponse(p3);
					});
		}
		catch (RuntimeException ex) {
			SendKick(p.getSender(), p.Argument.getLinkSid(), BKick.ErrorProtocolException, ex.toString());
			throw ex;
		}
	}

	@Override
	public int ProcessLinkBroken(Protocol _protocol) {
		var protocol = (LinkBroken)_protocol;
		// 目前仅需设置online状态。
		if (false == protocol.Argument.getStates().isEmpty()) {
			var roleId = protocol.Argument.getStates().get(0);
			Game.App.getInstance().Game_Login.getOnlines().OnLinkBroken(roleId);
		}
		return Procedure.Success;
	}

	@Override
	public int ProcessModuleRedirectRequest(Protocol _rpc) {
		var rpc = (ModuleRedirect)_rpc;
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
		catch (RuntimeException e) {
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
	public int ProcessModuleRedirectAllRequest(Protocol _protocol) {
		var protocol = (ModuleRedirectAllRequest)_protocol;
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
				}, Transaction.getCurrent().getTopProcedure().getActionName(), null).Call());

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
		catch (RuntimeException e) {
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
		public void OnRemoved() {
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
		public final <T> int ProcessHash(int hash, Zeze.Util.Factory<T> factory, Zeze.Util.Func1<T, Integer> action) {
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
		public final void ProcessResult(ModuleRedirectAllResult result) {
			for (var h : result.Argument.getHashs().entrySet()) {
				// 嵌套存储过程，单个分组的结果处理不影响其他分组。
				// 不判断单个分组的处理结果，错误也继续执行其他分组。XXX
				Game.App.getInstance().Zeze.NewProcedure(() -> ProcessHashResult(
						h.getKey(), h.getValue().getReturnCode(), h.getValue().getParams(), h.getValue().getActions()),
						getMethodFullName(), null).Call();
			}
		}

		// 生成代码实现。see Zezex.ModuleRedirect.cs
		public int ProcessHashResult(int _hash_, int _returnCode_, Binary _params, List<Zezex.Provider.BActionParam> _actions_) {
			return Procedure.NotImplement;
		}
	}

	@Override
	public int ProcessModuleRedirectAllResult(Protocol _protocol) {
		var protocol = (ModuleRedirectAllResult)_protocol;
		// replace RootProcedure.ActionName. 为了统计和日志输出。
		Transaction.getCurrent().getTopProcedure().setActionName(protocol.Argument.getMethodFullName());
		var ctx = App.Server.<ModuleRedirectAllContext>TryGetManualContext(protocol.Argument.getSessionId());
		if (ctx != null) {
			ctx.ProcessResult(protocol);
		}
		return Procedure.Success;
	}

	@Override
	public int ProcessTransmit(Protocol _protocol) {
		var protocol = (Transmit)_protocol;
		App.Game_Login.getOnlines().ProcessTransmit(
				protocol.Argument.getSender(),
				protocol.Argument.getActionName(),
				protocol.Argument.getRoles().keySet());
		return Procedure.Success;
	}

	@Override
	public int ProcessAnnounceLinkInfo(Protocol _protocol) {
		var protocol = (AnnounceLinkInfo)_protocol;
		var linkSession = (Game.Server.LinkSession)protocol.getSender().getUserState();
		linkSession.Setup(protocol.Argument.getLinkId(), protocol.Argument.getProviderSessionId());
		return Procedure.Success;
	}

	@Override
	public int ProcessSendConfirm(Protocol _protocol) {
		var protocol = (SendConfirm)_protocol;
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
    public static final int ModuleId = 10001;


    public Game.App App;

    public ModuleProvider(Game.App app) {
        App = app;
        // register protocol factory and handles
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.AnnounceLinkInfo();
            factoryHandle.Handle = (_p) -> ProcessAnnounceLinkInfo(_p);
            factoryHandle.NoProcedure = true;
            App.Server.AddFactoryHandle(42956386019790L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.Bind();
            App.Server.AddFactoryHandle(42956370435684L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.Dispatch();
            factoryHandle.Handle = (_p) -> ProcessDispatch(_p);
            App.Server.AddFactoryHandle(42956978782483L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.LinkBroken();
            factoryHandle.Handle = (_p) -> ProcessLinkBroken(_p);
            App.Server.AddFactoryHandle(42958025275938L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.ModuleRedirect();
            factoryHandle.Handle = (_p) -> ProcessModuleRedirectRequest(_p);
            App.Server.AddFactoryHandle(42957157812299L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.ModuleRedirectAllRequest();
            factoryHandle.Handle = (_p) -> ProcessModuleRedirectAllRequest(_p);
            App.Server.AddFactoryHandle(42954995920679L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.ModuleRedirectAllResult();
            factoryHandle.Handle = (_p) -> ProcessModuleRedirectAllResult(_p);
            App.Server.AddFactoryHandle(42956729630485L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.SendConfirm();
            factoryHandle.Handle = (_p) -> ProcessSendConfirm(_p);
            App.Server.AddFactoryHandle(42954102918470L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.Transmit();
            factoryHandle.Handle = (_p) -> ProcessTransmit(_p);
            factoryHandle.NoProcedure = true;
            App.Server.AddFactoryHandle(42954614917260L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.UnBind();
            App.Server.AddFactoryHandle(42955764678922L, factoryHandle);
        }
        // register table
    }

    public void UnRegister() {
        App.Server.getFactorys().remove(42956386019790L);
        App.Server.getFactorys().remove(42956370435684L);
        App.Server.getFactorys().remove(42956978782483L);
        App.Server.getFactorys().remove(42958025275938L);
        App.Server.getFactorys().remove(42957157812299L);
        App.Server.getFactorys().remove(42954995920679L);
        App.Server.getFactorys().remove(42956729630485L);
        App.Server.getFactorys().remove(42954102918470L);
        App.Server.getFactorys().remove(42954614917260L);
        App.Server.getFactorys().remove(42955764678922L);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
