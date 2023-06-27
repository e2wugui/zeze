
using Zeze.Net;
using Zeze;
using System.Collections.Concurrent;
using Zege.User;

namespace Zege
{
    public sealed partial class App
    {
        public static Func<string, string, Task> OnError { get; set; }

        private static ConcurrentDictionary<string, App> Apps { get; } = new();

        public static App GetOrAdd(string account)
        {
            var fresh = false;
            var app = Apps.GetOrAdd(account, (key) =>
            {
                fresh = true;
                return new App(account);
            });

            if (fresh)
            {
                //app.Start("127.0.0.1", 11000);
                app.Start("10.12.7.155", 11000);
            }
            return app;
        }

        private static void Remove(App app)
        {
            Apps.TryRemove(KeyValuePair.Create(app.Account, app));
        }

        public string Account { get; private set; }
        public Connector Connector; // 属性不能用 out，就这样了吧。

        public App(string account)
        {
            Account = account;
        }

        public void Start(string ip, int port)
        {
            // 客户端使用默认配置启动，不用配置文件。
            CreateZeze(new Config());

            CreateService();

            if (null != ip && ip.Length > 0 && port != 0)
            {
                // 指定ip,port连接服务器。
                ClientService.Config.TryGetOrAddConnector(ip, port, false, out Connector);
            }
            else
            {
                // 从配置里面找到第一个Connnector。
                ClientService.Config.ForEachConnector((c) =>
                {
                    Connector = c;
                    return false;
                });
            }
            if (null == Connector)
                throw new Exception("miss Connector!");

            CreateModules();
            Zeze.StartAsync().Wait(); // 启动数据库
            StartModules(); // 启动模块，装载配置什么的。
            StartService(); // 启动网络
        }

        public void Stop()
        {
            Remove(this);

            StopService(); // 关闭网络
            StopModules(); // 关闭模块，卸载配置什么的。
            Zeze.Stop(); // 关闭数据库
            DestroyModules();
            DestroyService();
            DestroyZeze();
        }
    }
}
