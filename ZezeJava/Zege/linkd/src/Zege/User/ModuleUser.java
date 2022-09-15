package Zege.User;

import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.OutLong;

public class ModuleUser extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }


    private  <A extends Bean, R extends Bean> long proxy(int hash, Rpc<A, R> r) throws Throwable {
        var originSender = r.getSender();
        var originSessionId = r.getSessionId();
        var provider = new OutLong();
        if (App.LinkdApp.LinkdProvider.ChoiceHashWithoutBind(r.getModuleId(), hash, provider)) {
            var providerSocket = App.LinkdApp.LinkdProviderService.GetSocket(provider.Value);
            if (null != providerSocket && r.Send(providerSocket, (r_) -> {
                // 这里重用了rpc，直接向server再次发送，当结果返回的时候，sender和sessionId发生了变化。
                // 在发送结果给真正的客户端前，需要恢复原来的sender和sessionId。
                r.setSender(originSender);
                r.setSessionId(originSessionId);
                if (r.isTimeout())
                    r.setResultCode(Zeze.Transaction.Procedure.Timeout);
                else
                    r.SendResult();
                return 0;
            }))
                return 0; // dispatch success
        }
        App.LinkdService.ReportError(r.getSender().getSessionId(), BReportError.FromLink,
                BReportError.CodeNoProvider, "no provider.");
        return 0;
    }

    @Override
    protected long ProcessCreateRequest(Zege.User.Create r) throws Throwable {
        return proxy(ByteBuffer.calc_hashnr(r.Argument.getAccount()), r);
    }

    @Override
    protected long ProcessCreateWithCertRequest(Zege.User.CreateWithCert r) throws Throwable {
        return proxy(ByteBuffer.calc_hashnr(r.Argument.getAccount()), r);
    }

    @Override
    protected long ProcessVerifyChallengeResultRequest(Zege.User.VerifyChallengeResult r) {
        // 这个Rpc不向客户端开放。linkd内部发送给server，用来完成真正的验证。
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessPrepareRequest(Zege.User.Prepare r) throws Throwable {
        return proxy(ByteBuffer.calc_hashnr(r.Argument.getAccount()), r);
    }

    @Override
    protected long ProcessOfflineNotifyRequest(Zege.User.OfflineNotify r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessOfflineRegisterRequest(Zege.User.OfflineRegister r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleUser(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
