
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using Zeze.Net;

namespace Zezex
{
    [TestClass]
    public sealed partial class App
    {
        [TestMethod]
        public void MainTest()
        {
            App.Instance.Start();
            App.Instance.Stop();
        }

        public void Start()
        {
            CreateZeze();
            CreateService();
            CreateModules();
            Zeze.StartAsync().Wait(); // 启动数据库
            StartModules(); // 启动模块，装载配置什么的。
            var connector = new Connector(false, "ws://127.0.0.1:22000/websocket");
            ClientService.Config.AddConnector(connector);
            StartService(); // 启动网络
            connector.GetReadySocket();
            Zezex_Linkd.sendCs();
            Console.WriteLine("Press Enter To Exit.");
            Console.In.ReadLine();
        }

        public void Stop()
        {
            StopService(); // 关闭网络
            StopModules(); // 关闭模块，卸载配置什么的。
            Zeze.Stop(); // 关闭数据库
            DestroyModules();
            DestroyService();
            DestroyZeze();
        }
    }
}
