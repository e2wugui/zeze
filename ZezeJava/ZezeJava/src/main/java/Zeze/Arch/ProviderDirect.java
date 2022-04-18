package Zeze.Arch;

import Zeze.Beans.ProviderDirect.AnnounceProviderInfo;
import Zeze.Beans.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Beans.ProviderDirect.BModuleRedirectAllRequest;
import Zeze.Beans.ProviderDirect.ModuleRedirect;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		if (rpc.getSender() == null) {
			rpc.setRequest(false);
			var service = ProviderApp.ProviderDirectService;
			rpc.Dispatch(service, service.FindProtocolFactoryHandle(rpc.getTypeId()));
		} else
			rpc.SendResult();
	}

	@Override
	protected long ProcessModuleRedirectRequest(ModuleRedirect rpc) throws Throwable {
		rpc.Result.setModuleId(rpc.Argument.getModuleId());
		rpc.Result.setServerId(ProviderApp.Zeze.getConfig().getServerId());
		var handle = ProviderApp.Zeze.Redirect.Handles.get(rpc.Argument.getMethodFullName());
		if (handle == null) {
			SendResultCode(rpc, ModuleRedirect.ResultCodeMethodFullNameNotFound);
			return Procedure.LogicError;
		}

		Object result;
		switch (handle.RequestTransactionLevel) {
		case Serializable:
		case AllowDirtyWhenAllRead:
			var out = new OutObject<>();
			var rc = ProviderApp.Zeze.NewProcedure(() -> {
				out.Value = handle.RequestHandle.call(rpc.Argument.getHashCode(), rpc.Argument.getParams());
				return Procedure.Success;
			}, "ProcessModuleRedirectRequest").Call();
			if (rc != Procedure.Success) {
				SendResultCode(rpc, rc);
				return rc;
			}
			result = out.Value;
			break;
		default:
			try {
				result = handle.RequestHandle.call(rpc.Argument.getHashCode(), rpc.Argument.getParams());
			} catch (Throwable e) {
				logger.error("call exception:", e);
				SendResultCode(rpc, Procedure.Exception);
				return Procedure.Exception;
			}
			break;
		}
		if (result instanceof RedirectFuture) {
			((RedirectFuture<?>)result).then(r -> {
				rpc.Result.setParams(handle.ResultEncoder.apply(r));
				// rpc 成功了，具体handle结果还需要看ReturnCode。
				SendResultCode(rpc, Procedure.Success);
			});
		} else
			SendResultCode(rpc, Procedure.Success);
		return Procedure.Success;
	}

	private void SendResult(AsyncSocket sender, Zeze.Net.Protocol<?> p) throws Throwable {
		if (sender == null) {
			var service = ProviderApp.ProviderDirectService;
			p.Dispatch(service, service.FindProtocolFactoryHandle(p.getTypeId()));
		}
		p.Send(sender);
	}

	private void SendResultIfSizeExceed(AsyncSocket sender, ModuleRedirectAllResult result) throws Throwable {
		int size = 0;
		for (var hashResult : result.Argument.getHashs().values())
			size += hashResult.getParams().size();
		if (size > 0x10_0000) { // 1MB
			SendResult(sender, result);
			result.Argument.getHashs().clear();
		}
	}

	private void SendResultForAsync(ModuleRedirectAllRequest p, int hash, RedirectResult result) throws Throwable {
		var allResult = new ModuleRedirectAllResult();
		allResult.setResultCode(ModuleRedirect.ResultCodeSuccess);

		var resArg = allResult.Argument;
		resArg.setModuleId(p.Argument.getModuleId());
		resArg.setServerId(ProviderApp.Zeze.getConfig().getServerId());
		resArg.setSourceProvider(p.Argument.getSourceProvider());
		resArg.setSessionId(p.Argument.getSessionId());
		resArg.setMethodFullName(p.Argument.getMethodFullName());

		var handle = ProviderApp.Zeze.Redirect.Handles.get(p.Argument.getMethodFullName());
		var hashResult = new BModuleRedirectAllHash();
		hashResult.setParams(handle.ResultEncoder.apply(result));
		resArg.getHashs().put(hash, hashResult);
		SendResult(p.getSender(), allResult);
	}

	@Override
	protected long ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest p) throws Throwable {
		var pa = p.Argument;
		var result = new ModuleRedirectAllResult();
		var resArg = result.Argument;
		resArg.setModuleId(pa.getModuleId());
		resArg.setServerId(ProviderApp.Zeze.getConfig().getServerId());
		resArg.setSourceProvider(pa.getSourceProvider());
		resArg.setSessionId(pa.getSessionId());
		resArg.setMethodFullName(pa.getMethodFullName());

		var handle = ProviderApp.Zeze.Redirect.Handles.get(pa.getMethodFullName());
		if (handle == null) {
			result.setResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
			// 失败了，需要把hash返回。此时是没有处理结果的。
			for (var hash : pa.getHashCodes())
				resArg.getHashs().put(hash, new BModuleRedirectAllHash());
			SendResult(p.getSender(), result);
			return Procedure.LogicError;
		}
		result.setResultCode(ModuleRedirect.ResultCodeSuccess);

		for (var hash : pa.getHashCodes()) {
			// 嵌套存储过程，某个分组处理失败不影响其他分组。
			var hashResult = new BModuleRedirectAllHash();
			RedirectAllFuture<?> future;
			switch (handle.RequestTransactionLevel) {
			case Serializable:
			case AllowDirtyWhenAllRead:
				var out = new OutObject<>();
				hashResult.setReturnCode(ProviderApp.Zeze.NewProcedure(() -> {
					out.Value = handle.RequestHandle.call(hash, pa.getParams());
					return Procedure.Success;
				}, "ProcessModuleRedirectAllRequest").Call());
				future = (RedirectAllFuture<?>)out.Value;
				break;
			default:
				try {
					future = (RedirectAllFuture<?>)handle.RequestHandle.call(hash, pa.getParams());
				} catch (Throwable e) {
					logger.error("RequestHandle.call exception:", e);
					hashResult.setReturnCode(Procedure.Exception);
					future = null;
				}
				break;
			}
			// 单个分组处理失败继续执行。XXX
			if (future == null)
				resArg.getHashs().put(hash, hashResult);
			else if (future.getClass() == RedirectAllFutureFinished.class) {
				hashResult.setParams(handle.ResultEncoder.apply(((RedirectAllFutureFinished<?>)future).getResult()));
				resArg.getHashs().put(hash, hashResult);
				SendResultIfSizeExceed(p.getSender(), result);
			} else
				((RedirectAllFutureAsync<?>)future).onResult(r -> SendResultForAsync(p, hash, r));
		}

		// send remain
		if (resArg.getHashs().size() > 0)
			SendResult(p.getSender(), result);
		return Procedure.Success;
	}

	@Override
	protected long ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) throws Throwable {
		var ctx = ProviderApp.ProviderDirectService.
				<ModuleRedirectAllContext<?>>TryGetManualContext(protocol.Argument.getSessionId());
		if (ctx != null)
			ctx.ProcessResult(ProviderApp.Zeze, protocol);
		return Procedure.Success;
	}

	@Override
	protected long ProcessAnnounceProviderInfoRequest(AnnounceProviderInfo r) {
		ProviderApp.ProviderDirectService.SetRelativeServiceReady(
				(ProviderSession)r.getSender().getUserState(), r.Argument.getIp(), r.Argument.getPort());
		return 0;
	}
}
