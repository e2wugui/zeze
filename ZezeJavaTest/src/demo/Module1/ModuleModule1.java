package demo.Module1;

public class ModuleModule1 extends AbstractModuleModule1 {
    public void Start(demo.App app) {
    }

    public void Stop(demo.App app) {
    }

    @Override
    public int ProcessProtocol4(Zeze.Net.Protocol _p) {
        var p = (Protocol4)_p;
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    public int ProcessProtocol3(Zeze.Net.Protocol _p) {
        var p = (Protocol3)_p;
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    public int ProcessRpc4Request(Zeze.Net.Protocol _r) {
        var r = (Rpc4)_r;
        return Zeze.Transaction.Procedure.NotImplement;
    }

}
