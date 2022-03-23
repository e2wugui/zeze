
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Threading.Tasks;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestTransactionLevelSerialiable
    {
        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }

        private volatile bool InTest = true;

        [TestMethod]
        public void Test2()
        {
            demo.App.Instance.Zeze.NewProcedure(init, "test_init").CallAsync().Wait();
            Task.Run(Verify_task);
            try
            {
                Task[] tasks = new Task[200000];
                for (int i = 0; i < tasks.Length; ++i)
                {
                    tasks[i] = demo.App.Instance.Zeze.NewProcedure(trade, "test_trade").CallAsync();
                }
                Task.WaitAll(tasks);
            }
            finally
            {
                InTest = false;
            }
        }

        private void Verify_task()
        {
            while (InTest)
            {
                demo.App.Instance.Zeze.NewProcedure(verify, "test_verify").CallAsync().Wait();
            }
        }

        private async Task<long> verify()
        {
            var v1 = await demo.App.Instance.demo_Module1.Table1.GetOrAdd(1L);
            var v2 = await demo.App.Instance.demo_Module1.Table1.GetOrAdd(2L);
            var total = v1.Int1 + v2.Int1;
            // 必须在事务成功时verify，执行过程中是可能失败的。
            Transaction.Current.RunWhileCommit(() => { Assert.AreEqual(100_000, total); });
            return 0;
        }

        private async Task<long> init()
        {
            var v1 = await demo.App.Instance.demo_Module1.Table1.GetOrAdd(1L);
            var v2 = await demo.App.Instance.demo_Module1.Table1.GetOrAdd(2L);
            v1.Int1 = 100_000;
            v2.Int1 = 0;
            return 0;
        }

        private async Task<long> trade()
        {
            var v1 = await demo.App.Instance.demo_Module1.Table1.GetOrAdd(1L);
            var v2 = await demo.App.Instance.demo_Module1.Table1.GetOrAdd(2L);
            var money = global::Zeze.Util.Random.Instance.Next(1000);
            if (global::Zeze.Util.Random.Instance.Next(100) < 50)
            {
                // random swap
                var tmp = v1;
                v1 = v2;
                v2 = tmp;
            }
            v1.Int1 -= money;
            v2.Int1 += money;
            return 0;
        }
    }
}

