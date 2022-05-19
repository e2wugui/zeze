using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Services;
using Zeze.Transaction;

namespace UnitTest.Zeze.Net
{
    [TestClass]
    public class TestHandshake
    {
        [TestMethod]
        public void Test()
        {
            var server = new Server();
            var client = new Client();
            try
            {
                server.AddFactoryHandle(new Hello().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Hello(),
                    Handle = ProcessHelloRequest,
                });
                client.AddFactoryHandle(new Hello().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Hello(),
                });
                server.Start();
                client.Start();

                using var serversocket = server.NewServerSocket(System.Net.IPAddress.Any, 7766, null);
                client.Connect("127.0.0.1", 7766, false);
                client.Config.FindConnector("127.0.0.1:7766").GetReadySocket();
                var clientsocket = client.GetSocket();
                var hello = new Hello();
                hello.Argument.Name = "hello";
                hello.SendAsync(clientsocket).Wait();
                Console.WriteLine("done");
            }
            finally
            {
                client?.Stop();
                server?.Stop();
            }
        }

#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
        private async Task<long> ProcessHelloRequest(Protocol p)
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
        {
            var hello = (Hello)p;
            hello.Result = hello.Argument;
            hello.SendResult();
            return 0;
        }

        public class Param : Bean
        {
            public string Name { get; set; }

            public override void Decode(ByteBuffer bb)
            {
                Name = bb.ReadString();
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteString(Name);
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
            }
        }
        public class Hello : Rpc<Param, Param>
        {
            public const int ModuleId_ = 0;
            public const int ProtocolId_ = 0;
            public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

            public override int ModuleId => ModuleId_;

            public override int ProtocolId => ProtocolId_;
        }
        public class Server : HandshakeServer
        {
            public Server() :base("TestHandshakeServer", (global::Zeze.Config)null)
            {
            }
        }

        public class Client : HandshakeClient
        {
            public Client() : base("TestHandshakeClient", (global::Zeze.Config)null)
            {

            }

            public override async Task OnHandshakeDone(AsyncSocket sender)
            {
                await base.OnHandshakeDone(sender);
            }
        }
    }
}
