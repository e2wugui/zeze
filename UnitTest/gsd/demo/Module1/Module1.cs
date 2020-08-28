
namespace demo.Module1
{
    public sealed partial class Module1 : AbstractModule1
    {
        public void Start(demo.App app)
        {
        }

        public void Stop(demo.App app)
        {
        }

        public override int ProcessProtocol1(Protocol1 protocol)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessProtocol3(Protocol3 protocol)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessRpc1Request(Rpc1 rpc)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessRpc1Response(Rpc1 rpc)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessRpc1Timeout(Rpc1 rpc)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public Table1 Table1 => _Table1;
        public Table2 Table2 => _Table2;
    }
}
