package Zeze.Arch;

import Zeze.Builtin.ProviderDirect.AnnounceProviderInfo;
import Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.AsyncSocket;
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

	@Override
	protected long ProcessModuleRedirectRequest(ModuleRedirect rpc) throws Throwable {
		var zeze = ProviderApp.Zeze;
		var rpcArg = rpc.Argument;
		rpc.Result.setModuleId(rpcArg.getModuleId());
		rpc.Result.setServerId(zeze.getConfig().getServerId());
		var handle = zeze.Redirect.Handles.get(rpcArg.getMethodFullName());
		if (handle == null) {
			rpc.SendResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
			return Procedure.LogicError;
		}

		Object result;
		switch (handle.RequestTransactionLevel) {
		case Serializable:
		case AllowDirtyWhenAllRead:
			var out = new OutObject<>();
			var rc = zeze.NewProcedure(() -> {
				out.Value = handle.RequestHandle.call(rpcArg.getHashCode(), rpcArg.getParams());
				return Procedure.Success;
			}, "ProcessModuleRedirectRequest").Call();
			if (rc != Procedure.Success) {
				rpc.SendResultCode(rc);
				return rc;
			}
			result = out.Value;
			break;
		default:
			try {
				result = handle.RequestHandle.call(rpcArg.getHashCode(), rpcArg.getParams());
			} catch (Throwable e) {
				logger.error("call exception:", e);
				rpc.SendResultCode(Procedure.Exception);
				return Procedure.Exception;
			}
			break;
		}
		if (result instanceof RedirectFuture) {
			((RedirectFuture<?>)result).then(r -> {
				rpc.Result.setParams(handle.ResultEncoder.apply(r));
				// rpc 成功了，具体handle结果还需要看ReturnCode。
				rpc.SendResultCode(Procedure.Success);
			});
		} else
			rpc.SendResultCode(Procedure.Success);
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

	private void SendResultForAsync(ModuleRedirectAllRequest p, int hash, RedirectResult result,
									RedirectHandle handle) throws Throwable {
		var pa = p.Argument;
		var res = new ModuleRedirectAllResult();
		var resArg = res.Argument;
		resArg.setModuleId(pa.getModuleId());
		resArg.setServerId(ProviderApp.Zeze.getConfig().getServerId());
		resArg.setSourceProvider(pa.getSourceProvider());
		resArg.setSessionId(pa.getSessionId());
		resArg.setMethodFullName(pa.getMethodFullName());

		var hashResult = new BModuleRedirectAllHash();
		hashResult.setReturnCode(Procedure.Success);
		hashResult.setParams(handle.ResultEncoder.apply(result));
		resArg.getHashs().put(hash, hashResult);
		res.setResultCode(ModuleRedirect.ResultCodeSuccess);
		SendResult(p.getSender(), res);
	}

	@Override
	protected long ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest p) throws Throwable {
		var pa = p.Argument;
		var res = new ModuleRedirectAllResult();
		var resArg = res.Argument;
		resArg.setModuleId(pa.getModuleId());
		resArg.setServerId(ProviderApp.Zeze.getConfig().getServerId());
		resArg.setSourceProvider(pa.getSourceProvider());
		resArg.setSessionId(pa.getSessionId());
		resArg.setMethodFullName(pa.getMethodFullName());

		var handle = ProviderApp.Zeze.Redirect.Handles.get(pa.getMethodFullName());
		if (handle == null) {
			res.setResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
			// 失败了，需要把hash返回。此时是没有处理结果的。
			for (var hash : pa.getHashCodes()) {
				var hashResult = new BModuleRedirectAllHash();
				hashResult.setReturnCode(Procedure.NotImplement);
				resArg.getHashs().put(hash, hashResult);
			}
			SendResult(p.getSender(), res);
			return Procedure.LogicError;
		}
		res.setResultCode(ModuleRedirect.ResultCodeSuccess);

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
					hashResult.setReturnCode(Procedure.Success);
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
				SendResultIfSizeExceed(p.getSender(), res);
			} else
				((RedirectAllFutureAsync<?>)future).onResult(r -> SendResultForAsync(p, hash, r, handle));
		}

		// send remain
		if (resArg.getHashs().size() > 0)
			SendResult(p.getSender(), res);
		return Procedure.Success;
	}

	@Override
	protected long ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) throws Throwable {
		var ctx = ProviderApp.ProviderDirectService.
				<RedirectAllContext<?>>TryGetManualContext(protocol.Argument.getSessionId());
		if (ctx != null)
			ctx.ProcessResult(ProviderApp.Zeze, protocol);
		return Procedure.Success;
	}

	@Override
	protected long ProcessAnnounceProviderInfoRequest(AnnounceProviderInfo r) {
		var ps = (ProviderSession)r.getSender().getUserState();
		ps.ServerId = r.Argument.getServerId();
		ProviderApp.ProviderDirectService.SetRelativeServiceReady(ps, r.Argument.getIp(), r.Argument.getPort());
		return 0;
	}
}
