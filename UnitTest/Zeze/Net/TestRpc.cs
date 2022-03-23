using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Net;
using System.Net;
using Zeze.Transaction;
using System.Threading;
using System.Threading.Tasks;

namespace UnitTest.Zeze.Net
{
    [TestClass]
    public class TestRpc
    {
        [TestMethod]
        public void TestRpcSimple()
        {
            Service server = new Service("TestRpc.Server");

            FirstRpc forid = new FirstRpc();
            server.AddFactoryHandle(forid.TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new FirstRpc(),
                Handle = ProcessFirstRpcRequest,
            });

            AsyncSocket servetrSocket = server.NewServerSocket(IPAddress.Any, 5000, null);
            Client client = new Client(this);
            client.AddFactoryHandle(forid.TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new FirstRpc(),
            });

            AsyncSocket clientSocket = client.NewClientSocket("127.0.0.1", 5000, null, null);
            connected.WaitOne();

            FirstRpc first = new FirstRpc();
            first.Argument.Int1 = 1234;
            //Console.WriteLine("SendFirstRpcRequest");
            first.SendForWait(clientSocket).Task.Wait();
            //Console.WriteLine("FirstRpc Wait End");
            Assert.AreEqual(first.Argument.Int1, first.Result.Int1);
        }

        ManualResetEvent connected = new ManualResetEvent(false);

        public async Task<long> ProcessFirstRpcRequest(Protocol p)
        {
            var rpc = p as FirstRpc;
            rpc.Result.Assign(rpc.Argument);
            rpc.SendResult();
            Console.WriteLine("ProcessFirstRpcRequest result.Int1=" +  rpc.Result.Int1);
            return Procedure.Success;
        }

        public class FirstRpc : Rpc<demo.Module1.Value, demo.Module1.Value>
        {
            public override int ModuleId => 1;

            public override int ProtocolId => 1;
        }
        public class Client : Service
        {
            TestRpc test;
            public Client(TestRpc test) : base("TestRpc.Client")
            {
                this.test = test;
            }
            public override void OnSocketConnected(AsyncSocket so)
            {
                base.OnSocketConnected(so);
                test.connected.Set();
            }
        }
    }
}
