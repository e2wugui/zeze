
namespace demo
{
    public sealed partial class App
    {
        private static void AdjustTableConf(Zeze.Config.TableConf conf)
        {
            if (null != conf)
            {
                if (conf.CacheCapacity < Benchmark.ABasicSimpleAddOneThread.AddCount)
                    conf.CacheCapacity = Benchmark.ABasicSimpleAddOneThread.AddCount;
                if (conf.CacheConcurrencyLevel < Benchmark.CBasicSimpleAddConcurrent.ConcurrentLevel)
                    conf.CacheConcurrencyLevel = Benchmark.CBasicSimpleAddConcurrent.ConcurrentLevel;
            }
        }

        public void Start(Zeze.Config config = null)
        {
            if (config == null)
                config = new Zeze.Config().LoadAndParse();
            //config.WorkerThreads = 240;
            AdjustTableConf(config.DefaultTableConf);
            AdjustTableConf(config.GetTableConf("demo_Module1_Table1"));

            Create(config);
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.StartAsync().Wait(); // 启动数据库
            StartService(); // 启动网络等等。
        }

        public void Stop()
        {
            StopService(); // 关闭网络等等。
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
            Destroy();
        }
    }
}
