package Zeze.Arch;

public class Online extends AbstractOnline {
    @Override
    protected long ProcessLoginRequest(Zeze.Builtin.Online.Login r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessLogoutRequest(Zeze.Builtin.Online.Logout r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Online.ReliableNotifyConfirm r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessReLoginRequest(Zeze.Builtin.Online.ReLogin r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
