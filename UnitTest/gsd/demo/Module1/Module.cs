
using System;

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
            Console.WriteLine(protocol.Argument);
            new Rpc2().Send(protocol.Sender, (Protocol) => 0);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessProtocol3(Protocol3 protocol)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessRpc1Request(Rpc1 rpc)
        {
            rpc.SendResult();
            return Zeze.Transaction.Procedure.Success;
        }

        public Table1 Table1 => _Table1;
        public Table2 Table2 => _Table2;
    }
}
