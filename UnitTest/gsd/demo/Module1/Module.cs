
namespace demo.Module1
{
    public sealed partial class Module : AbstractModule
    {
        public void Start(demo.App app)
        {
        }

        public void Stop(demo.App app)
        {
        }

        public override int ProcessProtocol1(Protocol1 protocol)
        {
            protocol.Send(protocol.Sender);
            return Zeze.Transaction.Procedure.Success;
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
            // 如果使用同步发送rpc请求，结果通过wait得到，不会触发这个异步回调
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessRpc1Timeout(Rpc1 rpc)
        {
            // 如果使用同步发送rpc请求，结果通过wait得到，不会触发这个异步回调
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public Table1 Table1 => _Table1;
        public Table2 Table2 => _Table2;
    }
}
