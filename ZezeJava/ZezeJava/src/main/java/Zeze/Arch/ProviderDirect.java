package Zeze.Arch;

import Zeze.Builtin.ProviderDirect.AnnounceProviderInfo;
import Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Builtin.ProviderDirect.Transmit;
import Zeze.Builtin.ProviderDirect.TransmitAccount;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider之间直连服务模块。
 * 仅包含部分实现，使用的时候需要继承并实现完全。
 * 需要的时候可以重载重新实现默认实现。
 */
public class ProviderDirect extends AbstractProviderDirect {
	private static final Logger logger = LogManager.getLogger(ProviderDirect.class);

	protected ProviderApp providerApp;

	@Override
	protected long ProcessModuleRedirectRequest(ModuleRedirect rpc) throws Exception {
		var zeze = providerApp.zeze;
		var rpcArg = rpc.Argument;
		rpc.Result.setModuleId(rpcArg.getModuleId());
		rpc.Result.setServerId(zeze.getConfig().getServerId());
		var handle = zeze.redirect.handles.get(rpcArg.getMethodFullName());
		if (handle == null) {
			rpc.SendResultCode(ModuleRedirect.ResultCodeMethodFullNameNotFound);
			return Procedure.LogicError;
		}
		if (handle.version != rpcArg.getVersion()) {
			rpc.SendResultCode(ModuleRedirect.ResultCodeHandleVersion);
			return Procedure.LogicError;
		}

		Object result;
		switch (handle.requestTransactionLevel) {
		case Serializable:
		case AllowDirtyWhenAllRead:
			var out = new OutObject<>();
			var rc = zeze.newProcedure(() -> {
				out.value = handle.requestHandle.call(rpcArg.getHashCode(), rpcArg.getParams());
				return Procedure.Success;
			}, "ProcessModuleRedirectRequest").call();
			if (rc != Procedure.Success) {
				rpc.SendResultCode(rc);
				return rc;
			}
			result = out.value;
			break;
		default:
			try {
				result = handle.requestHandle.call(rpcArg.getHashCode(), rpcArg.getParams());
			} catch (Exception e) {
				logger.error("call exception:", e);
				rpc.SendResultCode(Procedure.Exception);
				return Procedure.Exception;
			}
			break;
		}
		if (result instanceof RedirectFuture) {
			((RedirectFuture<?>)result).onSuccess(r -> {
				if (r instanceof Long)
					rpc.SendResultCode((Long)r);
				else if (r instanceof Binary) {
					rpc.Result.setParams((Binary)r);
					rpc.SendResultCode(Procedure.Success);
				} else if (r instanceof String) {
					rpc.Result.setParams(new Binary((String)r));
					rpc.SendResultCode(Procedure.Success);
				} else {
					var re = handle.resultEncoder;
					if (re != null)
						rpc.Result.setParams(re.apply(r));
					// rpc 成功了，具体handle结果还需要看ReturnCode。
					rpc.SendResultCode(Procedure.Success);
				}
			}).onFail(e -> {
				logger.error("call failed:", e);
				rpc.SendResultCode(Procedure.Exception);
			});
		} else
			rpc.SendResultCode(Procedure.Success);
		return Procedure.Success;
	}

	private void sendResult(@Nullable AsyncSocket sender, @NotNull Protocol<?> p) throws Exception {
		if (sender == null) {
			var service = providerApp.providerDirectService;
			service.dispatchProtocol(p);
			return;
		}
		p.Send(sender);
	}

	private void sendResultIfSizeExceed(@Nullable AsyncSocket sender, @NotNull ModuleRedirectAllResult result)
			throws Exception {
		int size = 0;
		for (var hashResult : result.Argument.getHashs().values())
			size += hashResult.getParams().size();
		if (size > 0x10_0000) { // 1MB
			sendResult(sender, result);
			result.Argument.getHashs().clear();
		}
	}

	private void sendResultForAsync(@NotNull ModuleRedirectAllRequest p, int hash, @NotNull RedirectResult result,
									@NotNull RedirectHandle handle) throws Exception {
		var pa = p.Argument;
		var res = new ModuleRedirectAllResult();
		var resArg = res.Argument;
		resArg.setModuleId(pa.getModuleId());
		resArg.setServerId(providerApp.zeze.getConfig().getServerId());
		resArg.setSourceProvider(pa.getSourceProvider());
		resArg.setSessionId(pa.getSessionId());
		resArg.setMethodFullName(pa.getMethodFullName());

		var hashResult = new BModuleRedirectAllHash.Data(Procedure.Success, null);
		var re = handle.resultEncoder;
		if (re != null)
			hashResult.setParams(re.apply(result));
		resArg.getHashs().put(hash, hashResult);
		res.setResultCode(ModuleRedirect.ResultCodeSuccess);
		sendResult(p.getSender(), res);
	}

	@Override
	protected long ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest p) throws Exception {
		var pa = p.Argument;
		var res = new ModuleRedirectAllResult();
		var resArg = res.Argument;
		resArg.setModuleId(pa.getModuleId());
		resArg.setServerId(providerApp.zeze.getConfig().getServerId());
		resArg.setSourceProvider(pa.getSourceProvider());
		resArg.setSessionId(pa.getSessionId());
		resArg.setMethodFullName(pa.getMethodFullName());

		var handle = providerApp.zeze.redirect.handles.get(pa.getMethodFullName());
		int rc = handle == null ? ModuleRedirect.ResultCodeMethodFullNameNotFound
				: handle.version != pa.getVersion() ? ModuleRedirect.ResultCodeHandleVersion
				: ModuleRedirect.ResultCodeSuccess;
		res.setResultCode(rc);
		if (rc != ModuleRedirect.ResultCodeSuccess) {
			// 失败了，需要把hash返回。此时是没有处理结果的。
			for (var hash : pa.getHashCodes())
				resArg.getHashs().put(hash, new BModuleRedirectAllHash.Data(Procedure.NotImplement, null));
			sendResult(p.getSender(), res);
			return Procedure.LogicError;
		}

		for (var hash : pa.getHashCodes()) {
			// 嵌套存储过程，某个分组处理失败不影响其他分组。
			var hashResult = new BModuleRedirectAllHash.Data();
			RedirectAllFuture<?> future;
			switch (handle.requestTransactionLevel) {
			case Serializable:
			case AllowDirtyWhenAllRead:
				var out = new OutObject<>();
				hashResult.setReturnCode(providerApp.zeze.newProcedure(() -> {
					out.value = handle.requestHandle.call(hash, pa.getParams());
					return Procedure.Success;
				}, "ProcessModuleRedirectAllRequest").call());
				future = (RedirectAllFuture<?>)out.value;
				break;
			default:
				try {
					future = (RedirectAllFuture<?>)handle.requestHandle.call(hash, pa.getParams());
					hashResult.setReturnCode(Procedure.Success);
				} catch (Exception e) {
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
				var re = handle.resultEncoder;
				if (re != null)
					hashResult.setParams(re.apply(((RedirectAllFutureFinished<?>)future).getResult()));
				resArg.getHashs().put(hash, hashResult);
				sendResultIfSizeExceed(p.getSender(), res);
			} else
				((RedirectAllFutureAsync<?>)future).onResult(r -> sendResultForAsync(p, hash, r, handle));
		}

		// send remain
		if (!resArg.getHashs().isEmpty())
			sendResult(p.getSender(), res);
		return Procedure.Success;
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	protected long ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) throws Exception {
		var ctx = providerApp.providerDirectService.
				<RedirectAllContext<?>>tryGetManualContext(protocol.Argument.getSessionId());
		if (ctx != null)
			ctx.processResult(protocol);
		return Procedure.Success;
	}

	@Override
	protected long ProcessTransmit(Transmit p) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessTransmitAccount(TransmitAccount p) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessAnnounceProviderInfoRequest(AnnounceProviderInfo r) {
		var ps = (ProviderSession)r.getSender().getUserState();
		ps.serverId = r.Argument.getServerId();
		providerApp.providerDirectService.setRelativeServiceReady(ps, r.Argument.getIp(), r.Argument.getPort());
		return 0;
	}
}
