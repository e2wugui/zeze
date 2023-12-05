using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Component;

namespace Benchmark
{
    [TestClass]
    public class BBasicSimpleAddConcurrentWithConflict
    {
        public const int AddCount = 100_0000;
        public const int ConcurrentLevel = 1_000;
        [TestMethod]
        public void testBenchmark() {
            demo.App.Instance.Start();
            try {
                demo.App.Instance.Zeze.NewProcedure(Remove, "remove").CallSynchronously();
                var tasks = new List<Task>(AddCount);
                for (int i = 1; i < ConcurrentLevel; ++i)
                {
                    int c = i;
                    tasks.Add(demo.App.Instance.Zeze.NewProcedure(async () =>
                    {
                        await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(c);
                        return 0;
                    }, "Add").CallAsync());
                }
                foreach (var task in tasks)
                    task.Wait();
                tasks.Clear();
                Console.WriteLine("benchmark start...");
                var b = new Zeze.Util.Benchmark();
                for (int i = 0; i < AddCount; ++i) {
                    var c = i % 1000;
                    tasks.Add(demo.App.Instance.Zeze.NewProcedure(async () => await Add(c), "Add").CallAsync());
                }
                b.Report(this.GetType().FullName, AddCount);
                foreach (var task in tasks) {
                    task.Wait();
                }
                b.Report(this.GetType().FullName, AddCount);
                demo.App.Instance.Zeze.NewProcedure(Check, "check").CallSynchronously();
                demo.App.Instance.Zeze.NewProcedure(Remove, "remove").CallSynchronously();
            }
            finally
            {
                demo.App.Instance.Stop();
            }
        }

        private async Task<long> Check() {
            var r = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(0L);
            Assert.AreEqual(r.Long2, AddCount);
            //System.out.println(r.getLong2());
            return 0;
        }

        private async Task<long> Add(long key) {
            var r = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(0L);
            r.Long2 += 1;
            //for (int i = 0; i < 100000; ++i)
            //    r.Int_1 += i;
            return 0;
        }

        private async Task<long> Remove() {
            await demo.App.Instance.demo_Module1.Table1.RemoveAsync(0L);
            return 0;
        }
    }
}