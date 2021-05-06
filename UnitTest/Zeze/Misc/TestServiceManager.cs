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
        ServiceManager ServiceManager;
        [TestCleanup]
        public void TestCleanup()
        {
            ServiceManager?.Dispose();
        }
        TaskCompletionSource<int> future;

        [TestMethod]
        public void Test1()
        {
            string ip = "127.0.0.1";
            int port = 7601;

            var config = global::Zeze.Config.Load();
            // for reconnect
            var agentConf = new global::Zeze.Net.ServiceConf();
            agentConf.AddConnector(new global::Zeze.Net.Connector(ip, port));
            config.ServiceConfMap.TryAdd("Zeze.Services.ServiceManager.Agent", agentConf);

            System.Net.IPAddress address =
                string.IsNullOrEmpty(ip)
                ? System.Net.IPAddress.Any
                : System.Net.IPAddress.Parse(ip);

            // 后面需要手动销毁再重建测试。不用using了，使用TestCleanup关闭最后的实例。
            ServiceManager = new ServiceManager(address, port, config, 0);
            var serviceName = "TestServiceManager";

            future = new TaskCompletionSource<int>();
            using var agent = new ServiceManager.Agent(config,
                (agent) =>
                {
                    agent.RegisterService(serviceName, "1");
                    agent.SubscribeService(serviceName, ServiceManager.SubscribeInfo.SubscribeTypeSimple);
                },
                (state) =>
                {
                    Console.WriteLine("OnChanged: " + state.ServiceInfos);
                    this.future.SetResult(0);
                }
                );
            Console.WriteLine("ConnectNow");
            future.Task.Wait();

            Console.WriteLine("RegisterService 2");
            future = new TaskCompletionSource<int>();
            agent.RegisterService(serviceName, "2");
            future.Task.Wait();

            // 改变订阅类型
            Console.WriteLine("Change Subscribe type");
            agent.UnSubscribeService(serviceName);
            future = new TaskCompletionSource<int>();
            agent.SubscribeService(serviceName, ServiceManager.SubscribeInfo.SubscribeTypeReadyCommit);
            future.Task.Wait();

            agent.SubscribeStates.TryGetValue(serviceName, out var state);
            object anyState = this;
            state.SetServiceReadyState("1", anyState);
            state.SetServiceReadyState("2", anyState);
            state.SetServiceReadyState("3", anyState);

            Console.WriteLine("RegisterService 3");
            future = new TaskCompletionSource<int>();
            agent.RegisterService(serviceName, "3");
            future.Task.Wait();

            Console.WriteLine("Test Reconnect");
            ServiceManager.Dispose();
            future = new TaskCompletionSource<int>();
            ServiceManager = new ServiceManager(address, port, config, 0);
            future.Task.Wait();
        }
    }
}
