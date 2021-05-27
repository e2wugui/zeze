using System;
using System.Text;
using System.Threading;

namespace ServiceManager
{
    public class Program
    {
        public static void Main(string[] args)
        {
            //Run(args);
            new Zeze.Raft.Test().Run();
            //TestRocksDb();
            //TestServiceManager();
        }

        public static void Run(string[] args)
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

        public static void TestServiceManager()
        {
            var test = new UnitTest.Zeze.Misc.TestServiceManager();
            try
            {
                test.Test1();
            }
            finally
            {
                test.TestCleanup();
            }
        }

        public static void TestRocksDb()
        {
            string temp = System.IO.Path.GetTempPath();
            var options = new RocksDbSharp.DbOptions().SetCreateIfMissing(true);
            using (var db = RocksDbSharp.RocksDb.Open(options,
                System.IO.Path.Combine(temp, "rocksdb_example")))
            {
                {
                    var iter = db.NewIterator();
                    iter.SeekToLast();
                    if (iter.Valid())
                    {
                        Console.WriteLine($"last is({BitConverter.ToString(iter.Key())},{BitConverter.ToString(iter.Value())})");
                    }
                    else
                    {
                        Console.WriteLine("last not found.");
                    }
                }
                var key1 = Encoding.UTF8.GetBytes("key1");
                var value1 = Encoding.UTF8.GetBytes("value1");
                db.Put(key1, value1);
                Console.WriteLine(BitConverter.ToString(db.Get(key1)));
                var value1get = db.Get(key1);
                Console.WriteLine(null == value1get ? "null" : BitConverter.ToString(value1get));
                var key2 = Encoding.UTF8.GetBytes("key2");
                var value2 = Encoding.UTF8.GetBytes("value2");
                db.Put(key2, value2);
                {
                    var iter = db.NewIterator();
                    iter.SeekToLast();
                    if (iter.Valid())
                    {
                        Console.WriteLine($"last is({BitConverter.ToString(iter.Key())},{BitConverter.ToString(iter.Value())})");
                    }
                    else
                    {
                        Console.WriteLine("last not found.");
                    }
                }
                db.Remove(key1);
                db.Remove(key2);
            }
        }
    }

}
