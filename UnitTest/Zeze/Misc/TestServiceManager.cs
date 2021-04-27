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
            ServiceManager.Instance.Start(address, port, config);
            var serviceName = "TestServiceManager";
            ServiceManager.Agent.Instance.Open(config,
                () =>
                {
                    ServiceManager.Agent.Instance.RegisterService(serviceName, "1");
                    ServiceManager.Agent.Instance.SubscribeService(
                        serviceName, ServiceManager.SubscribeInfo.SubscribeTypeSimple);
                },
                (state) =>
                {
                    Console.WriteLine("OnChanged: " + state.ServiceInfos);
                }
                );
            Console.WriteLine("ConnectNow");
            ServiceManager.Agent.Instance.Client.NewClientSocket(ip, port);
            Thread.Sleep(1000);

            Console.WriteLine("RegisterService 2");
            ServiceManager.Agent.Instance.RegisterService(serviceName, "2");
            Thread.Sleep(1000);

            // 改变订阅类型
            Console.WriteLine("Change Subscribe type");
            ServiceManager.Agent.Instance.UnSubscribeService(
                serviceName, ServiceManager.SubscribeInfo.SubscribeTypeSimple);
            ServiceManager.Agent.Instance.SubscribeService(
                serviceName, ServiceManager.SubscribeInfo.SubscribeTypeReadyCommit);
            Thread.Sleep(1000);

            ServiceManager.Agent.Instance.ServiceStates.TryGetValue(serviceName, out var state);
            object anyState = this;
            state.SetServiceReadyState("1", anyState);
            state.SetServiceReadyState("2", anyState);
            state.SetServiceReadyState("3", anyState);

            Console.WriteLine("RegisterService 3");
            ServiceManager.Agent.Instance.RegisterService(serviceName, "3");
            Thread.Sleep(1000);

            ServiceManager.Agent.Instance.Stop();
            ServiceManager.Instance.Stop();
        }
    }
}
