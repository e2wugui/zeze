
namespace demo
{
    public sealed partial class App
    {
        public Zeze.Collections.LinkedMap.Module LinkedMapModule;
        public Zeze.Game.Bag.Module BagModule;

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

            CreateZeze(config);
            CreateService();
            CreateModules();
            LinkedMapModule = new(Zeze);
            BagModule = new(Zeze);
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.StartAsync().Wait(); // 启动数据库
            StartService(); // 启动网络等等。
        }

        public void Stop()
        {
            StopService(); // 关闭网络等等。
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
            LinkedMapModule = null;
            BagModule = null;
            DestroyModules();
            DestroyService();
            DestroyZeze();
        }
    }
}
