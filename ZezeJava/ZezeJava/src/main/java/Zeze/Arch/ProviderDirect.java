package Zeze.Arch;

import Zeze.Beans.ProviderDirect.*;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;

/**
 * Provider之间直连服务模块。
 * 仅包含部分实现，使用的时候需要继承并实现完全。
 * 需要的时候可以重载重新实现默认实现。
 */
public abstract class ProviderDirect extends AbstractProviderDirect {
    public ProviderApp ProviderApp;

    @Override
    protected long ProcessModuleRedirectRequest(ModuleRedirect rpc) throws Throwable {
        try {
            // replace RootProcedure.ActionName. 为了统计和日志输出。
            Transaction.getCurrent().getTopProcedure().setActionName(rpc.Argument.getMethodFullName());

            rpc.Result.setModuleId(rpc.Argument.getModuleId());
            rpc.Result.setServerId(ProviderApp.Zeze.getConfig().getServerId());
            var handle = ProviderApp.Zeze.Redirect.Handles.get(rpc.Argument.getMethodFullName());
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
            result.Argument.setServerId(ProviderApp.Zeze.getConfig().getServerId());
            result.Argument.setSourceProvider(protocol.Argument.getSourceProvider());
            result.Argument.setSessionId(protocol.Argument.getSessionId());
            result.Argument.setMethodFullName(protocol.Argument.getMethodFullName());

            var handle = ProviderApp.Zeze.Redirect.Handles.get(protocol.Argument.getMethodFullName());
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
                hashResult.setReturnCode(ProviderApp.Zeze.NewProcedure(() -> {
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
        var ctx = ProviderApp.ProviderDirectService.<ModuleRedirectAllContext>TryGetManualContext(protocol.Argument.getSessionId());
        if (ctx != null) {
            ctx.ProcessResult(protocol);
        }
        return Procedure.Success;
    }

    @Override
    protected long ProcessAnnounceProviderInfoRequest(AnnounceProviderInfo r) {
        ProviderApp.ProviderDirectService.updateServiceInfos(r.Argument.getIp(), r.Argument.getPort(), r.getSender().getSessionId());
        return 0;
    }
}
