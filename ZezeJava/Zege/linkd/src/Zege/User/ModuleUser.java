package Zege.User;

public class ModuleUser extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessAuthRequest(Zege.Linkd.Auth r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessCreateRequest(Zege.User.Create r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessCreateWithCertRequest(Zege.User.CreateWithCert r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessVerifyChallengeResultRequest(Zege.User.VerifyChallengeResult r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleUser(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
