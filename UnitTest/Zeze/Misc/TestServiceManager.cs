using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Services.ServiceManager;
using Zeze.Services;
using Zeze.Net;

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
        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }
        TaskCompletionSource<int> future;

        [TestMethod]
        public void TestBase()
        {
            var infos = new ServiceInfos("TestBase");
            infos.Insert(new ServiceInfo("TestBase", "1", 0));
            infos.Insert(new ServiceInfo("TestBase", "3", 0));
            infos.Insert(new ServiceInfo("TestBase", "2", 0));
            Assert.AreEqual("TestBase=[1,2,3,]", infos.ToString());
        }

        [TestMethod]
        public async Task Test1()
        {
            string ip = "127.0.0.1";
            int port = 5001; // c# 版本的Server已经删除，所以不能动态自建服务器测试，使用java版本的，需要先运行准备好。

            System.Net.IPAddress address =
                string.IsNullOrEmpty(ip)
                ? System.Net.IPAddress.Any
                : System.Net.IPAddress.Parse(ip);

            // 后面需要手动销毁重建进行重连测试。不用using了，使用TestCleanup关闭最后的实例。
            var serviceName = "TestServiceManager";

            future = new TaskCompletionSource<int>(TaskCreationOptions.RunContinuationsAsynchronously);
            // for reconnect
            var clientConfig = demo.App.Instance.Zeze.Config;
            var agentConfig = new ServiceConf();
            var agentName = "Zeze.Services.ServiceManager.Agent.Test";
            clientConfig.ServiceConfMap.TryAdd(agentName, agentConfig);
            agentConfig.AddConnector(new Connector(ip, port));
            using var agent = new Agent(demo.App.Instance.Zeze, agentName);
            agent.Client.Start();
            await agent.RegisterService(serviceName, "1", 0, "127.0.0.1", 1234);
            agent.OnChanged = (state) =>
            {
                Console.WriteLine("OnChanged: " + state);
                this.future.SetResult(0);
            };
            await agent.SubscribeService(serviceName);
            var load = new ServerLoad()
            {
                Ip = "127.0.0.1",
                Port = 1234,
            };
            agent.SetLoad(load);
            Console.WriteLine("ConnectNow");
            future.Task.Wait();

            Console.WriteLine("RegisterService 2");
            future = new TaskCompletionSource<int>(TaskCreationOptions.RunContinuationsAsynchronously);
            await agent.RegisterService(serviceName, "2", 0);
            future.Task.Wait();

            // 改变订阅类型
            Console.WriteLine("Change Subscribe type");
            await agent.UnSubscribeService(serviceName);
            future = new TaskCompletionSource<int>(TaskCreationOptions.RunContinuationsAsynchronously);
            await agent.SubscribeService(serviceName);
            future.Task.Wait();

            agent.SubscribeStates.TryGetValue(serviceName, out var state);
            object anyState = this;
            state.SetIdentityLocalState("1", anyState);
            state.SetIdentityLocalState("2", anyState);
            state.SetIdentityLocalState("3", anyState);

            Console.WriteLine("RegisterService 3");
            future = new TaskCompletionSource<int>(TaskCreationOptions.RunContinuationsAsynchronously);
            await agent.RegisterService(serviceName, "3", 0);
            future.Task.Wait();
        }
    }
}
