package Zeze.Onz;

public class Onz extends AbstractOnz {
    @Override
    protected long ProcessFuncProcedureRequest(Zeze.Builtin.Onz.FuncProcedure r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessFuncSagaRequest(Zeze.Builtin.Onz.FuncSaga r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessFuncSagaCancelRequest(Zeze.Builtin.Onz.FuncSagaCancel r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
