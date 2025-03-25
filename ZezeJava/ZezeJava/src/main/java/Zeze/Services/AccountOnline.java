package Zeze.Services;

public class AccountOnline extends AbstractAccountOnline {
    @Override
    protected long ProcessLoginRequest(Zeze.Builtin.AccountOnline.Login r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessLogoutRequest(Zeze.Builtin.AccountOnline.Logout r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessRegisterRequest(Zeze.Builtin.AccountOnline.Register r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
