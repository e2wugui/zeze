using System;
using System.Threading;

namespace ServiceManager
{
    class Program
    {
        static void Run(string[] args)
        {
            string ip = null;
            int port = 5001;

            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-ip": ip = args[++i]; break;
                    case "-port": port = int.Parse(args[++i]); break;

                }
            }

            System.Net.IPAddress address =
                string.IsNullOrEmpty(ip)
                ? System.Net.IPAddress.Any
                : System.Net.IPAddress.Parse(ip);

            var config = new Zeze.Config();
            var smconfig = new Zeze.Services.ServiceManager.Conf();
            config.AddCustomize(smconfig);
            config.LoadAndParse();

            using var sm = new Zeze.Services.ServiceManager(address, port, config);

            while (true)
            {
                Thread.Sleep(1000);
            }
        }

        static void Main(string[] args)
        {
            //*
            var test = new UnitTest.Zeze.Misc.TestServiceManager();
            try
            {
                test.Test1();
            }
            finally
            {
                test.TestCleanup();
            }
            /*/
            Run(args);
            */
        }
    }
}
