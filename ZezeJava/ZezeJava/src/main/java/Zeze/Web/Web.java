package Zeze.Web;

public class Web extends AbstractWeb {
    @Override
    protected long ProcessAuthJsonRequest(Zeze.Builtin.Web.AuthJson r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessAuthOkRequest(Zeze.Builtin.Web.AuthOk r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessAuthQueryRequest(Zeze.Builtin.Web.AuthQuery r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessRequestJsonRequest(Zeze.Builtin.Web.RequestJson r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessRequestQueryRequest(Zeze.Builtin.Web.RequestQuery r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
