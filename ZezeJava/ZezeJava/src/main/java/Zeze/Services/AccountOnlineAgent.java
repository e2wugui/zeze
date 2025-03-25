package Zeze.Services;

public class AccountOnlineAgent extends AbstractAccountOnlineAgent {
    @Override
    protected long ProcessKickRequest(Zeze.Builtin.AccountOnline.Kick r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
