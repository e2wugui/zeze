using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Benchmark
{
    [TestClass]

    public class CBasicSimpleAddConcurrent {
        public const int AddCount = 100_0000;
        public const int ConcurrentLevel = 1_000;
        [TestMethod]
        public void testBenchmark() {
            demo.App.Instance.Start();
            try {
                for (int i = 0; i < ConcurrentLevel; ++i) {
                    long k = i;
                    demo.App.Instance.Zeze.NewProcedure(() => Remove(k), "remove").CallSynchronously();
                }
                var tasks = new List<Task>(AddCount);
                Console.WriteLine("benchmark start...");
                for (int i = 0; i < ConcurrentLevel; ++i)
                {
                    int c = i;
                    tasks.Add(demo.App.Instance.Zeze.NewProcedure(async () => await Add(c), "Add").CallAsync());
                }
                foreach (var task in tasks)
                    task.Wait();
                tasks.Clear();
                var b = new Zeze.Util.Benchmark();
                for (int i = 0; i < AddCount - ConcurrentLevel; ++i)
                {
                    int c = i % ConcurrentLevel;
                    tasks.Add(demo.App.Instance.Zeze.NewProcedure(async () => await Add(c), "Add").CallAsync());
                }
                b.Report(this.GetType().FullName, AddCount);
                foreach (var task in tasks)
                    task.Wait();
                tasks.Clear();
                b.Report(this.GetType().FullName, AddCount);
                demo.App.Instance.Zeze.NewProcedure(Check, "check").CallSynchronously();
                for (long i = 0; i < ConcurrentLevel; ++i) {
                    long k = i;
                    demo.App.Instance.Zeze.NewProcedure(()=>Remove(k), "remove").CallSynchronously();
                }
            }
            finally
            {
                demo.App.Instance.Stop();
            }
        }

        private async Task<long> Check() {
            long sum = 0;
            for (long i = 0; i < ConcurrentLevel; ++i) {
                var r = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(i);
                sum += r.Long2;
            }
            Assert.AreEqual(sum, AddCount);
            return 0;
        }

        private async Task<long> Add(long key) {
            var r = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(key);
            r.Long2 += 1;
            //for (int i = 0; i < 100000; ++i)
            //    r.Int_1 += i;
            //System.out.println("Add=" + key);
            return 0;
        }

        private async Task<long> Remove(long key) {
            await demo.App.Instance.demo_Module1.Table1.RemoveAsync(key);
            return 0;
        }
    }
}