using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Services;

namespace UnitTest.Zeze.Misc
{
    [TestClass]
    public class TestServiceManager
    {
        [TestMethod]
        public void Test1()
        {
            string ip = "127.0.0.1";
            int port = 7601;

            var config = global::Zeze.Config.Load();
            System.Net.IPAddress address = string.IsNullOrEmpty(ip)
                ? System.Net.IPAddress.Any : System.Net.IPAddress.Parse(ip);
            using var sm = new ServiceManager(address, port, config);
            var serviceName = "TestServiceManager";
            using var agent = new ServiceManager.Agent(config,
                (agent) =>
                {
                    agent.RegisterService(serviceName, "1");
                    agent.SubscribeService(serviceName, ServiceManager.SubscribeInfo.SubscribeTypeSimple);
                },
                (state) =>
                {
                    Console.WriteLine("OnChanged: " + state.ServiceInfos);
                }
                );
            Console.WriteLine("ConnectNow");
            agent.Client.NewClientSocket(ip, port);
            Thread.Sleep(1000);

            Console.WriteLine("RegisterService 2");
            agent.RegisterService(serviceName, "2");
            Thread.Sleep(1000);

            // 改变订阅类型
            Console.WriteLine("Change Subscribe type");
            agent.UnSubscribeService(serviceName);
            agent.SubscribeService(serviceName, ServiceManager.SubscribeInfo.SubscribeTypeReadyCommit);
            Thread.Sleep(1000);

            agent.SubscribeStates.TryGetValue(serviceName, out var state);
            object anyState = this;
            state.SetServiceReadyState("1", anyState);
            state.SetServiceReadyState("2", anyState);
            state.SetServiceReadyState("3", anyState);

            Console.WriteLine("RegisterService 3");
            agent.RegisterService(serviceName, "3");
            Thread.Sleep(1000);
        }
    }
}
