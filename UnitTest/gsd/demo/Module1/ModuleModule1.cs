
using System;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Util;

namespace demo.Module1
{
    public sealed partial class ModuleModule1 : AbstractModule
    {
#pragma warning disable CA1822 // Mark members as static
        public void Start(demo.App _)
#pragma warning restore CA1822 // Mark members as static
        {
        }

        public void Stop(demo.App _)
        {
        }

        protected override async Task<long> ProcessProtocol1(Protocol p)
        {
            var protocol = p as Protocol1;
            protocol.Send(protocol.Sender);
            Console.WriteLine(protocol.Argument);
            await new Rpc2().SendAsync(protocol.Sender); // not wait
            return ResultCode.Success;
        }

        protected override Task<long> ProcessProtocol3(Protocol p)
        {
            return Task.FromResult(ResultCode.NotImplement);
        }

        protected override Task<long> ProcessRpc1Request(Protocol p)
        {
            var rpc = p as Rpc1;
            rpc.SendResult();
            return Task.FromResult(ResultCode.Success);
        }

        protected override Task<long> ProcessProtocolNoProcedure(Protocol p)
        {
            throw new NotImplementedException();
        }

        public Table1 Table1 => _Table1;
        public Table2 Table2 => _Table2;
        public TableImportant TableImportant => _TableImportant;
        public tFlush Tflush => _tFlush;
    }
}
