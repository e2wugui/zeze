package Zeze.Services;

public class Zoker extends AbstractZoker {
    @Override
    protected long ProcessAppendFileRequest(Zeze.Builtin.Zoker.AppendFile r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessCloseFileRequest(Zeze.Builtin.Zoker.CloseFile r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessListSerivceRequest(Zeze.Builtin.Zoker.ListSerivce r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessOpenFileRequest(Zeze.Builtin.Zoker.OpenFile r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessStartServiceRequest(Zeze.Builtin.Zoker.StartService r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessStopServiceRequest(Zeze.Builtin.Zoker.StopService r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
