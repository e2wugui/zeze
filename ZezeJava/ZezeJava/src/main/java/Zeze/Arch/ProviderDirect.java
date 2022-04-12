package Zeze.Arch;

import Zeze.Beans.ProviderDirect.AnnounceProviderInfo;
import Zeze.Beans.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Beans.ProviderDirect.ModuleRedirect;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutObject;

/**
 * Provider之间直连服务模块。
 * 仅包含部分实现，使用的时候需要继承并实现完全。
 * 需要的时候可以重载重新实现默认实现。
 */
public abstract class ProviderDirect extends AbstractProviderDirect {
	public ProviderApp ProviderApp;

	@Override
	protected long ProcessModuleRedirectRequest(ModuleRedirect rpc) throws Throwable {
		rpc.Result.setModuleId(rpc.Argument.getModuleId());
		rpc.Result.setServerId(ProviderApp.Zeze.getConfig().getServerId());
		var handle = ProviderApp.Zeze.Redirect.Handles.get(rpc.Argument.getMethodFullName());
		if (null == handle) {
			rpc.SendResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
			return Procedure.LogicError;
		}

		Binary resultBinary;
		switch (handle.RequestTransactionLevel) {
		case Serializable:
		case AllowDirtyWhenAllRead:
			var out = new OutObject<Binary>();
			var rc = ProviderApp.Zeze.NewProcedure(() -> {
				out.Value = handle.RequestHandle.call(rpc.SessionId, rpc.Argument.getHashCode(), rpc.Argument.getParams());
				return 0L;
			}, "ProcessModuleRedirectRequest").Call();
			if (0L != rc) {
				rpc.SendResultCode(rc);
			}
			resultBinary = out.Value;
			break;
		default:
			resultBinary = handle.RequestHandle.call(rpc.SessionId, rpc.Argument.getHashCode(), rpc.Argument.getParams());
			break;
		}
		rpc.Result.setParams(resultBinary);
		// rpc 成功了，具体handle结果还需要看ReturnCode。
		rpc.SendResultCode(Procedure.Success);
		return 0L;
	}

	private void SendResultIfSizeExceed(AsyncSocket sender, ModuleRedirectAllResult result) {
		int size = 0;
		for (var hashResult : result.Argument.getHashs().values()) {
			size += hashResult.getParams().size();
		}
		//noinspection PointlessArithmeticExpression
		if (size > 1 * 1024 * 1024) { // 1M
			result.Send(sender);
			result.Argument.getHashs().clear();
		}
	}

	@Override
	protected long ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest p) throws Throwable {
		var result = new ModuleRedirectAllResult();
		try {
			// common parameters for result
			result.Argument.setModuleId(p.Argument.getModuleId());
			result.Argument.setServerId(ProviderApp.Zeze.getConfig().getServerId());
			result.Argument.setSourceProvider(p.Argument.getSourceProvider());
			result.Argument.setSessionId(p.Argument.getSessionId());
			result.Argument.setMethodFullName(p.Argument.getMethodFullName());

			var handle = ProviderApp.Zeze.Redirect.Handles.get(p.Argument.getMethodFullName());
			if (null == handle) {
				result.setResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
				// 失败了，需要把hash返回。此时是没有处理结果的。
				for (var hash : p.Argument.getHashCodes()) {
					BModuleRedirectAllHash tempVar = new BModuleRedirectAllHash();
					result.Argument.getHashs().put(hash, tempVar);
				}
				result.Send(p.getSender());
				return Procedure.LogicError;
			}
			result.setResultCode(ModuleRedirect.ResultCodeSuccess);

			for (var hash : p.Argument.getHashCodes()) {
				// 嵌套存储过程，某个分组处理失败不影响其他分组。
				var hashResult = new BModuleRedirectAllHash();
				Binary params;
				switch (handle.RequestTransactionLevel) {
				case Serializable:
				case AllowDirtyWhenAllRead:
					var out = new OutObject<Binary>();
					ProviderApp.Zeze.NewProcedure(() -> {
						out.Value = handle.RequestHandle.call(p.Argument.getSessionId(), hash, p.Argument.getParams());
						return 0L;
					}, "ProcessModuleRedirectAllRequest").Call();
					params = out.Value;
					break;
				default:
					params = handle.RequestHandle.call(p.Argument.getSessionId(), hash, p.Argument.getParams());
					break;
				}
				// 单个分组处理失败继续执行。XXX
				hashResult.setParams(params);
				result.Argument.getHashs().put(hash, hashResult);
				SendResultIfSizeExceed(p.getSender(), result);
			}

			// send remain
			if (result.Argument.getHashs().size() > 0) {
				result.Send(p.getSender());
			}
			return Procedure.Success;
		} catch (Throwable e) {
			result.setResultCode(ModuleRedirect.ResultCodeHandleException);
			result.Send(p.getSender());
			throw e;
		}
	}

	@Override
	protected long ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) throws Throwable {
		// replace RootProcedure.ActionName. 为了统计和日志输出。
		Transaction txn = Transaction.getCurrent();
		assert txn != null;
		Procedure proc = txn.getTopProcedure();
		assert proc != null;
		proc.setActionName(protocol.Argument.getMethodFullName());
		var ctx = ProviderApp.ProviderDirectService.<ModuleRedirectAllContext>TryGetManualContext(protocol.Argument.getSessionId());
		if (ctx != null) {
			ctx.ProcessResult(ProviderApp.Zeze, protocol);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessAnnounceProviderInfoRequest(AnnounceProviderInfo r) {
		System.out.println("ProcessAnnounceProviderInfoRequest "
				+ r.Argument.getIp() + ":" + r.Argument.getPort()
				+ " serverId=" + ProviderApp.Zeze.getConfig().getServerId()
				+ ProviderApp.ProviderDirectService.ProviderSessions);
		ProviderApp.ProviderDirectService.SetRelativeServiceReady(
				(ProviderSession)r.getSender().getUserState(),
				r.Argument.getIp(), r.Argument.getPort());
		System.out.println("ProcessAnnounceProviderInfoRequest +++++ "
				+ r.Argument.getIp() + ":" + r.Argument.getPort()
				+ " serverId=" + ProviderApp.Zeze.getConfig().getServerId()
				+ ProviderApp.ProviderDirectService.ProviderSessions);
		return 0;
	}
}
