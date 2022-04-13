package Zeze.Arch;

import Zeze.Beans.ProviderDirect.AnnounceProviderInfo;
import Zeze.Beans.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Beans.ProviderDirect.ModuleRedirect;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Transaction.Bean;

/**
 * Provider之间直连服务模块。
 * 仅包含部分实现，使用的时候需要继承并实现完全。
 * 需要的时候可以重载重新实现默认实现。
 */
public abstract class ProviderDirect extends AbstractProviderDirect {
	private static final Logger logger = LogManager.getLogger(RedirectBase.class);

	public ProviderApp ProviderApp;

	private <A extends Bean, R extends Bean> void SendResultCode(Zeze.Net.Rpc<A, R> rpc, long rc) throws Throwable {
		rpc.setResultCode(rc);
		SendResult(rpc);
	}

	private <A extends Bean, R extends Bean> void SendResult(Zeze.Net.Rpc<A, R> rpc) throws Throwable {
		rpc.setRequest(false);
		Send(rpc.getSender(), rpc);
	}

	private void Send(AsyncSocket sender, Zeze.Net.Protocol rpc) throws Throwable {
		if (sender == null) {
			var service = ProviderApp.ProviderDirectService;
			rpc.Dispatch(service, service.FindProtocolFactoryHandle(rpc.getTypeId()));
		}
		rpc.Send(sender);
	}

	@Override
	protected long ProcessModuleRedirectRequest(ModuleRedirect rpc) throws Throwable {
		rpc.Result.setModuleId(rpc.Argument.getModuleId());
		rpc.Result.setServerId(ProviderApp.Zeze.getConfig().getServerId());
		var handle = ProviderApp.Zeze.Redirect.Handles.get(rpc.Argument.getMethodFullName());
		if (null == handle) {
			SendResultCode(rpc, ModuleRedirect.ResultCodeMethodFullNameNotFound);
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
				SendResultCode(rpc, rc);
			}
			resultBinary = out.Value;
			break;
		default:
			resultBinary = handle.RequestHandle.call(rpc.SessionId, rpc.Argument.getHashCode(), rpc.Argument.getParams());
			break;
		}
		rpc.Result.setParams(resultBinary);
		// rpc 成功了，具体handle结果还需要看ReturnCode。
		SendResultCode(rpc, Procedure.Success);
		return 0L;
	}

	private void SendResultIfSizeExceed(AsyncSocket sender, ModuleRedirectAllResult result) throws Throwable {
		int size = 0;
		for (var hashResult : result.Argument.getHashs().values()) {
			size += hashResult.getParams().size();
		}
		//noinspection PointlessArithmeticExpression
		if (size > 1 * 1024 * 1024) { // 1M
			Send(sender, result);
			result.Argument.getHashs().clear();
		}
	}

	@Override
	protected long ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest p) throws Throwable {
		var result = new ModuleRedirectAllResult();
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
			Send(p.getSender(), result);
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
				var out = new OutObject<>(Binary.Empty);
				hashResult.setReturnCode(ProviderApp.Zeze.NewProcedure(() -> {
					out.Value = handle.RequestHandle.call(p.Argument.getSessionId(), hash, p.Argument.getParams());
					return 0L;
				}, "ProcessModuleRedirectAllRequest").Call());
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
			Send(p.getSender(), result);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) throws Throwable {
		var ctx = ProviderApp.ProviderDirectService.
				<ModuleRedirectAllContext>TryGetManualContext(protocol.Argument.getSessionId());
		if (ctx != null) {
			ctx.ProcessResult(ProviderApp.Zeze, protocol);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessAnnounceProviderInfoRequest(AnnounceProviderInfo r) {
		ProviderApp.ProviderDirectService.SetRelativeServiceReady(
				(ProviderSession)r.getSender().getUserState(),
				r.Argument.getIp(), r.Argument.getPort());
		return 0;
	}
}
