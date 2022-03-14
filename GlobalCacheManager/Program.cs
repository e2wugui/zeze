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

            int workerThreads, completionPortThreads;
            ThreadPool.GetMinThreads(out workerThreads, out completionPortThreads);

            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-ip": ip = args[++i]; break;
                    case "-port": port = int.Parse(args[++i]); break;
                    case "-raft": raftName = args[++i]; break;
                    case "-threads": ThreadPool.SetMinThreads(int.Parse(args[++i]), completionPortThreads); break;

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
            else
            {
                using var GlobalRaft = new Zeze.Services.GlobalCacheManagerWithRaft(raftName);
                logger.Info($"Started Raft={raftName}");
                while (true)
                {
                    Thread.Sleep(10000);
                }
            }
        }
    }
}
