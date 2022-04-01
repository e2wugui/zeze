package Zeze.Arch;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Beans.Provider.*;

public abstract class ProviderImplement extends AbstractProviderImplement {
	//private static final Logger logger = LogManager.getLogger(ProviderImplement.class);

	private ProviderServer server;
	public ProviderServer getServer() {
		return server;
	}

	public void setServer(ProviderServer service) {
		server = service;
		RegisterProtocols(service);
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
			var factoryHandle = server.FindProtocolFactoryHandle(p.Argument.getProtocolType());
			if (null == factoryHandle) {
				SendKick(p.getSender(), p.Argument.getLinkSid(), BKick.ErrorProtocolUnkown, "unknown protocol");
				return Procedure.LogicError;
			}
			var p2 = factoryHandle.Factory.create();
			p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.getProtocolData()));
			p2.setSender(p.getSender());

			var session = new ProviderSession(server, p.Argument.getAccount(), p.Argument.getStates(), p.getSender(), p.Argument.getLinkSid());

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
	protected long ProcessModuleRedirectRequest(ModuleRedirect rpc) throws Throwable {
		try {
			// replace RootProcedure.ActionName. 为了统计和日志输出。
			Transaction.getCurrent().getTopProcedure().setActionName(rpc.Argument.getMethodFullName());

			rpc.Result.setModuleId(rpc.Argument.getModuleId());
			rpc.Result.setServerId(server.getZeze().getConfig().getServerId());
			var handle = server.getZeze().getModuleRedirect().Handles.get(rpc.Argument.getMethodFullName());
			if (null == handle) {
				rpc.SendResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
				return Procedure.LogicError;
			}

			var rp = handle.RequestHandle.call(rpc.SessionId, rpc.Argument.getHashCode(),
					rpc.Argument.getParams(), rpc.Result.getActions());
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
			result.Argument.setServerId(server.getZeze().getConfig().getServerId());
			result.Argument.setSourceProvider(protocol.Argument.getSourceProvider());
			result.Argument.setSessionId(protocol.Argument.getSessionId());
			result.Argument.setMethodFullName(protocol.Argument.getMethodFullName());

			var handle = server.getZeze().getModuleRedirect().Handles.get(protocol.Argument.getMethodFullName());
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
				hashResult.setReturnCode(server.getZeze().NewProcedure(() -> {
						var rp = handle.RequestHandle.call(
								protocol.Argument.getSessionId(), hash,
								protocol.Argument.getParams(), hashResult.getActions());
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

	@Override
	protected long ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) throws Throwable {
		// replace RootProcedure.ActionName. 为了统计和日志输出。
		Transaction.getCurrent().getTopProcedure().setActionName(protocol.Argument.getMethodFullName());
		var ctx = server.<ModuleRedirectAllContext>TryGetManualContext(protocol.Argument.getSessionId());
		if (ctx != null) {
			ctx.ProcessResult(protocol);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol) throws Throwable {
		var linkSession = (ProviderServer.LinkSession)protocol.getSender().getUserState();
		linkSession.Setup(protocol.Argument.getLinkId(), protocol.Argument.getProviderSessionId());
		return Procedure.Success;
	}
}
