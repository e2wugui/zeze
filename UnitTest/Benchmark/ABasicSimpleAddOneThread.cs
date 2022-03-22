using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Threading.Tasks;

namespace Benchmark
{
    [TestClass]
    public class ABasicSimpleAddOneThread
    {
        public const int AddCount = 1_000_000;

        [TestMethod]
        public async void testBenchmark()
        {
            demo.App.Instance.Start();
            try
            {
                await demo.App.Instance.Zeze.NewProcedure(Remove, "remove").CallAsync();
                Console.WriteLine("benchmark start...");
                var b = new Zeze.Util.Benchmark();
                var p = demo.App.Instance.Zeze.NewProcedure(Add, "Add");
                for (int i = 0; i < AddCount; ++i)
                {
                    await p.CallAsync();
                }
                b.Report(this.GetType().FullName, AddCount);
                await demo.App.Instance.Zeze.NewProcedure(Check, "check").CallAsync();
                await demo.App.Instance.Zeze.NewProcedure(Remove, "remove").CallAsync();
            }
            finally
            {
                demo.App.Instance.Stop();
            }
        }

        private async Task<long> Check()
        {
            var r = await demo.App.Instance.demo_Module1.Table1.GetOrAdd(1L);
            Assert.AreEqual(r.Long2, AddCount);
            //System.out.println(r.getLong2());
            return 0;
        }

        private long Add()
        {
            var r = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1L);
            r.Long2 += 1;
            return 0;
        }

        private long Remove()
        {
            demo.App.Instance.demo_Module1.Table1.Remove(1L);
            return 0;
        }
    }
}
