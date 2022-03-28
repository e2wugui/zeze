using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Collections.Generic;

namespace Infinite
{
    [TestClass]
    public class Simulate
    {
        static readonly List<App> Apps = new();

        [TestInitialize]
        public void Before()
        {
            for (int serverId = 0; serverId < 10; ++serverId)
                Apps.Add(new App(serverId));
            foreach (var app in Apps)
                app.Start();
        }

        [TestCleanup]
        public void After()
        {
            foreach (var app in Apps)
                app.Stop();
        }

        public static App randApp()
        {
            return randApp(Apps.Count);
        }

        public static App randApp(int max)
        {
            if (max > Apps.Count)
                max = Apps.Count;
            return Apps[Zeze.Util.Random.Instance.Next(max)];
        }

        public const int BatchTaskCount = 10000;
        public const int CacheCapacity = 1000;
        public const int AccessKeyBound = (int)(CacheCapacity * 1.20f);

        public bool Infinite = false; // 当使用本目录的Main独立启动时，会设置为true。
        public static long BatchNumber = 0;

        [TestMethod]
        public void testMain()
        {
            Tasks.Prepare();
            while (true)
            {
                ++BatchNumber;
                for (int i = 0; i < BatchTaskCount; ++i)
                    Tasks.RandCreateTask().Run();
                foreach (var app in Apps)
                    app.WaitAllRunningTasksAndClear();
                Tasks.VerifyBatch();
                if (!Infinite)
                    break;
            }
            App.logger.Fatal("Simulate Done.");
        }
    }
}
