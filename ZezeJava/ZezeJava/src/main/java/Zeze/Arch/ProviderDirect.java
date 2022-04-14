package Zeze.Arch;

import Zeze.Beans.ProviderDirect.AnnounceProviderInfo;
import Zeze.Beans.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Beans.ProviderDirect.ModuleRedirect;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;

/**
 * Provider之间直连服务模块。
 * 仅包含部分实现，使用的时候需要继承并实现完全。
 * 需要的时候可以重载重新实现默认实现。
 */
public abstract class ProviderDirect extends AbstractProviderDirect {
	// private static final Logger logger = LogManager.getLogger(RedirectBase.class);

	public ProviderApp ProviderApp;

	private <A extends Bean, R extends Bean> void SendResultCode(Zeze.Net.Rpc<A, R> rpc, long rc) throws Throwable {
		rpc.setResultCode(rc);
		SendResult(rpc);
	}

	private <A extends Bean, R extends Bean> void SendResult(Zeze.Net.Rpc<A, R> rpc) throws Throwable {
		rpc.setRequest(false);
		SendResult(rpc.getSender(), rpc);
	}

	private void SendResult(AsyncSocket sender, Zeze.Net.Rpc<?, ?> rpc) throws Throwable {
		if (sender == null) {
			var service = ProviderApp.ProviderDirectService;
			rpc.Dispatch(service, service.FindProtocolFactoryHandle(rpc.getTypeId()));
		}
		rpc.SendResult();
	}

	private void SendResult(AsyncSocket sender, Zeze.Net.Protocol<?> p) throws Throwable {
		if (sender == null) {
			var service = ProviderApp.ProviderDirectService;
			p.Dispatch(service, service.FindProtocolFactoryHandle(p.getTypeId()));
		}
		p.Send(sender);
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
				out.Value = handle.RequestHandle.call(rpc.SessionId, rpc.Argument.getHashCode(), rpc.Argument.getParams(), null);
				return 0L;
			}, "ProcessModuleRedirectRequest").Call();
			if (0L != rc) {
				SendResultCode(rpc, rc);
			}
			resultBinary = out.Value;
			break;
		default:
			resultBinary = handle.RequestHandle.call(rpc.SessionId, rpc.Argument.getHashCode(), rpc.Argument.getParams(), null);
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
			SendResult(sender, result);
			result.Argument.getHashs().clear();
		}
	}

	void SendResultForAsync(ModuleRedirectAllRequest p, RedirectResult result) throws Throwable {
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
		resArg.getHashs().put(result.getHash(), hashResult);
		SendResult(p.getSender(), allResult);
	}

	@Override
	protected long ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest p) throws Throwable {
		var result = new ModuleRedirectAllResult();
		var resArg = result.Argument;
		resArg.setModuleId(p.Argument.getModuleId());
		resArg.setServerId(ProviderApp.Zeze.getConfig().getServerId());
		resArg.setSourceProvider(p.Argument.getSourceProvider());
		resArg.setSessionId(p.Argument.getSessionId());
		resArg.setMethodFullName(p.Argument.getMethodFullName());

		var handle = ProviderApp.Zeze.Redirect.Handles.get(p.Argument.getMethodFullName());
		if (handle == null) {
			result.setResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
			// 失败了，需要把hash返回。此时是没有处理结果的。
			for (var hash : p.Argument.getHashCodes()) {
				BModuleRedirectAllHash tempVar = new BModuleRedirectAllHash();
				resArg.getHashs().put(hash, tempVar);
			}
			SendResult(p.getSender(), result);
			return Procedure.LogicError;
		}
		result.setResultCode(ModuleRedirect.ResultCodeSuccess);
		p.setUserState(this); // for async send result

		for (var hash : p.Argument.getHashCodes()) {
			// 嵌套存储过程，某个分组处理失败不影响其他分组。
			var hashResult = new BModuleRedirectAllHash();
			Binary encodeResult;
			switch (handle.RequestTransactionLevel) {
			case Serializable:
			case AllowDirtyWhenAllRead:
				var out = new OutObject<Binary>();
				hashResult.setReturnCode(ProviderApp.Zeze.NewProcedure(() -> {
					out.Value = handle.RequestHandle.call(p.Argument.getSessionId(), hash, p.Argument.getParams(), p);
					return 0L;
				}, "ProcessModuleRedirectAllRequest").Call());
				encodeResult = out.Value;
				break;
			default:
				try {
					encodeResult = handle.RequestHandle.call(p.Argument.getSessionId(), hash, p.Argument.getParams(), p);
				} catch (Throwable e) {
					hashResult.setReturnCode(Procedure.Exception);
					encodeResult = null;
				}
				break;
			}
			// 单个分组处理失败继续执行。XXX
			if (encodeResult != null)
				hashResult.setParams(encodeResult);
			if (encodeResult != RedirectHandle.ASYNC_RESULT) {
				resArg.getHashs().put(hash, hashResult);
				SendResultIfSizeExceed(p.getSender(), result);
			}
		}

		// send remain
		if (resArg.getHashs().size() > 0) {
			SendResult(p.getSender(), result);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) throws Throwable {
		var ctx = ProviderApp.ProviderDirectService.
				<ModuleRedirectAllContext<?>>TryGetManualContext(protocol.Argument.getSessionId());
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
