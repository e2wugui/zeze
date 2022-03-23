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
        public void testBenchmark()
        {
            demo.App.Instance.Start();
            try
            {
                demo.App.Instance.Zeze.NewProcedure(Remove, "remove").Call();
                Console.WriteLine("benchmark start...");
                var b = new Zeze.Util.Benchmark();
                var p = demo.App.Instance.Zeze.NewProcedure(Add, "Add");
                for (int i = 0; i < AddCount; ++i)
                {
                    p.Call();
                }
                b.Report(this.GetType().FullName, AddCount);
                demo.App.Instance.Zeze.NewProcedure(Check, "check").Call();
                demo.App.Instance.Zeze.NewProcedure(Remove, "remove").Call();
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

        private async Task<long> Add()
        {
            var r = await demo.App.Instance.demo_Module1.Table1.GetOrAdd(1L);
            r.Long2 += 1;
            return 0;
        }

        private async Task<long> Remove()
        {
            await demo.App.Instance.demo_Module1.Table1.Remove(1L);
            return 0;
        }
    }
}
