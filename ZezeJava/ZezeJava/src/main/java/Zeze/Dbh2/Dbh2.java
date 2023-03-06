package Zeze.Dbh2;

public class Dbh2 extends AbstractDbh2 {
    @Override
    protected long ProcessBeginTransactionRequest(Zeze.Builtin.Dbh2.BeginTransaction r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessDeleteRequest(Zeze.Builtin.Dbh2.Delete r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessPutRequest(Zeze.Builtin.Dbh2.Put r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
