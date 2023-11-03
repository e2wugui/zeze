package Zeze.Services;

public class ZokerAgent extends AbstractZokerAgent {
    @Override
    protected long ProcessRegisterRequest(Zeze.Builtin.Zoker.Register r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
