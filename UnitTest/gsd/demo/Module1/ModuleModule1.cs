
using System;
using System.Threading.Tasks;
using Zeze.Net;

namespace demo.Module1
{
    public sealed partial class ModuleModule1 : AbstractModule
    {
#pragma warning disable CA1822 // Mark members as static
        public void Start(demo.App _)
#pragma warning restore CA1822 // Mark members as static
        {
        }

#pragma warning disable CA1822 // Mark members as static
        public void Stop(demo.App _)
#pragma warning restore CA1822 // Mark members as static
        {
        }

        protected override async Task<long> ProcessProtocol1(Protocol p)
        {
            var protocol = p as Protocol1;
            protocol.Send(protocol.Sender);
            Console.WriteLine(protocol.Argument);
            await new Rpc2().SendAsync(protocol.Sender); // not wait
            return Zeze.Transaction.Procedure.Success;
        }

#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
        protected override async Task<long> ProcessProtocol3(Protocol p)
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
        protected override async Task<long> ProcessRpc1Request(Protocol p)
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
        {
            var rpc = p as Rpc1;
            rpc.SendResult();
            return Zeze.Transaction.Procedure.Success;
        }

#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
        protected override async Task<long> ProcessProtocolNoProcedure(Protocol p)
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
        {
            throw new NotImplementedException();
        }

        public Table1 Table1 => _Table1;
        public Table2 Table2 => _Table2;
        public TableImportant TableImportant => _TableImportant;
        public tflush Tflush => _tflush;
    }
}
