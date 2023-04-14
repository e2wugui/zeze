using System;
using System.Threading;
using Zeze.Services;
using Zeze.Raft;

namespace GlobalCacheManager
{
    public class Program
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public static void Main(string[] args)
        {
            string ip = null;
            int port = 5002;
            string raftName = null;
            string raftConf = "global.raft.xml";

            ThreadPool.GetMinThreads(out var _, out var completionPortThreads);

            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-ip":
                        ip = args[++i];
                        break;

                    case "-port":
                        port = int.Parse(args[++i]);
                        break;

                    case "-raft":
                        raftName = args[++i];
                        break;

                    case "-raftConf":
                        raftConf = args[++i];
                        break;

                    case "-threads":
                        ThreadPool.SetMinThreads(int.Parse(args[++i]), completionPortThreads);
                        break;

                }
            }
            if (string.IsNullOrEmpty(raftName))
            {
                System.Net.IPAddress address =
                    string.IsNullOrEmpty(ip)
                    ? System.Net.IPAddress.Any
                    : System.Net.IPAddress.Parse(ip);

                var GlobalServer = GlobalCacheManagerServer.Instance;
                GlobalServer.Start(address, port);
                //Console.WriteLine("Ok.");
                logger.Info($"Started. {GlobalServer.ServerSocket.Socket.LocalEndPoint}");
                while (true)
                {
                    Thread.Sleep(10000);
                }
            }
            else if (raftName.Equals("RunAllNodes"))
            {
                using var global1 = new GlobalCacheManagerWithRaft();
                global1.OpenAsync("127.0.0.1:5556", RaftConfig.Load(raftConf)).Wait();
                using var global2 = new GlobalCacheManagerWithRaft();
                global2.OpenAsync("127.0.0.1:5557", RaftConfig.Load(raftConf)).Wait();
                using var global3 = new GlobalCacheManagerWithRaft();
                global3.OpenAsync("127.0.0.1:5558", RaftConfig.Load(raftConf)).Wait();
                logger.Info($"Started Raft=RunAllNodes");
                while (true)
                {
                    Thread.Sleep(10000);
                }
            }
            else
            {
                var rconf = RaftConfig.Load(raftConf);
                using var global1 = new GlobalCacheManagerWithRaft().OpenAsync(raftName, rconf);
                // 等待启动完成，会报告启动错误结果吧。
                global1.Wait();
                logger.Info($"Started Raft={raftName}");
                while (true)
                {
                    Thread.Sleep(10000);
                }
            }
        }
    }
}
