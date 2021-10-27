using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Services.ServiceManager;
using Zeze.Services;

namespace UnitTest.Zeze.Misc
{
    [TestClass]
    public class TestServiceManager
    {
        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
        }
        ServiceManagerServer ServiceManager;
        [TestCleanup]
        public void TestCleanup()
        {
            ServiceManager?.Dispose();
            ServiceManager = null;
            demo.App.Instance.Stop();
        }
        TaskCompletionSource<int> future;

        [TestMethod]
        public void Test1()
        {
            string ip = "127.0.0.1";
            int port = 7601;

            System.Net.IPAddress address =
                string.IsNullOrEmpty(ip)
                ? System.Net.IPAddress.Any
                : System.Net.IPAddress.Parse(ip);

            // 后面需要手动销毁重建进行重连测试。不用using了，使用TestCleanup关闭最后的实例。
            ServiceManager?.Dispose();
            ServiceManager = new ServiceManagerServer(address, port, global::Zeze.Config.Load(), 0);
            var serviceName = "TestServiceManager";

            future = new TaskCompletionSource<int>();
            // for reconnect
            var clientConfig = demo.App.Instance.Zeze.Config;
            var agentConfig = new global::Zeze.Net.ServiceConf();
            var agentName = "Zeze.Services.ServiceManager.Agent.Test";
            clientConfig.ServiceConfMap.TryAdd(agentName, agentConfig);
            agentConfig.AddConnector(new global::Zeze.Net.Connector(ip, port));
            using var agent = new Agent(demo.App.Instance.Zeze, agentName);
            agent.Client.Start();
            agent.RegisterService(serviceName, "1");
            agent.OnChanged = (state) =>
            {
                Console.WriteLine("OnChanged: " + state.ServiceInfos);
                this.future.SetResult(0);
            };
            agent.SubscribeService(serviceName, SubscribeInfo.SubscribeTypeSimple);
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
            agent.SubscribeService(serviceName, SubscribeInfo.SubscribeTypeReadyCommit);
            future.Task.Wait();

            agent.SubscribeStates.TryGetValue(serviceName, out var state);
            object anyState = this;
            state.SetServiceIdentityReadyState("1", anyState);
            state.SetServiceIdentityReadyState("2", anyState);
            state.SetServiceIdentityReadyState("3", anyState);

            Console.WriteLine("RegisterService 3");
            future = new TaskCompletionSource<int>();
            agent.RegisterService(serviceName, "3");
            future.Task.Wait();

            Console.WriteLine("Test Reconnect");
            ServiceManager.Dispose();
            future = new TaskCompletionSource<int>();
            ServiceManager = new ServiceManagerServer(address, port, global::Zeze.Config.Load(), 0);
            future.Task.Wait();
            ServiceManager?.Dispose();
        }
    }
}
