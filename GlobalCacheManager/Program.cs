using System;
using System.Threading;

namespace GlobalCacheManager
{
    public class Program
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public static void Main(string[] args)
        {
            string ip = null;
            int port = 5555;
            string raftName = null;
            string raftConf = "global.raft.xml";

            int workerThreads, completionPortThreads;
            ThreadPool.GetMinThreads(out workerThreads, out completionPortThreads);

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
                        //ThreadPool.GetMinThreads(out workerThreads, out completionPortThreads);
                        //Console.WriteLine($"workerThreads={workerThreads} completionPortThreads={completionPortThreads}");
                        break;

                }
            }
            if (string.IsNullOrEmpty(raftName))
            {
                System.Net.IPAddress address =
                    string.IsNullOrEmpty(ip)
                    ? System.Net.IPAddress.Any
                    : System.Net.IPAddress.Parse(ip);

                var GlobalServer = Zeze.Services.GlobalCacheManagerServer.Instance;
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
                using var GlobalRaft1 = new Zeze.Services.GlobalCacheManagerWithRaft("127.0.0.1:5556", Zeze.Raft.RaftConfig.Load(raftConf));
                using var GlobalRaft2 = new Zeze.Services.GlobalCacheManagerWithRaft("127.0.0.1:5557", Zeze.Raft.RaftConfig.Load(raftConf));
                using var GlobalRaft3 = new Zeze.Services.GlobalCacheManagerWithRaft("127.0.0.1:5558", Zeze.Raft.RaftConfig.Load(raftConf));
                logger.Info($"Started Raft=RunAllNodes");
                while (true)
                {
                    Thread.Sleep(10000);
                }
            }
            else
            {
                var rconf = Zeze.Raft.RaftConfig.Load(raftConf);
                using var GlobalRaft = new Zeze.Services.GlobalCacheManagerWithRaft(raftName, rconf);
                logger.Info($"Started Raft={raftName}");
                while (true)
                {
                    Thread.Sleep(10000);
                }
            }
        }
    }
}
