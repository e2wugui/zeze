using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Benchmark
{
    [TestClass]

    public class CBasicSimpleAddConcurrent {
        public const int AddCount = 1_000_000;
        public const int ConcurrentLevel = 1_000;
        [TestMethod]
        public void testBenchmark() {
            demo.App.Instance.Start();
            try {
                for (int i = 0; i < ConcurrentLevel; ++i) {
                    long k = i;
                    demo.App.Instance.Zeze.NewProcedure(() => Remove(k), "remove").Call();
                }
                var tasks = new List<Task>(AddCount);
                for (int i = 0; i < AddCount; ++i) {
                    int c = i % ConcurrentLevel;
                    tasks.Add(Zeze.Util.Task.Create(demo.App.Instance.Zeze.NewProcedure(()=>Add(c), "Add")));
                    //tasks.add(Zeze.Util.Task.Create(App.Instance.Zeze.NewProcedure(this::Add, "Add"), null, null));
                }
                Console.WriteLine("benchmark start...");
                var b = new Zeze.Util.Benchmark();
                foreach (var task in tasks) {
                    Zeze.Util.Task.Run(task);
                }
                //b.Report(this.getClass().getName(), AddCount);
                foreach (var task in tasks) {
                    task.Wait();
                }
                b.Report(this.GetType().FullName, AddCount);
                demo.App.Instance.Zeze.NewProcedure(Check, "check").Call();
                for (long i = 0; i < ConcurrentLevel; ++i) {
                    long k = i;
                    demo.App.Instance.Zeze.NewProcedure(()=>Remove(k), "remove").Call();
                }
            }
            finally
            {
                demo.App.Instance.Stop();
            }
        }

        private int Check() {
            long sum = 0;
            for (long i = 0; i < ConcurrentLevel; ++i) {
                var r = demo.App.Instance.demo_Module1.Table1.GetOrAdd(i);
                sum += r.Long2;
            }
            Assert.AreEqual(sum, AddCount);
            return 0;
        }

        private int Add(long key) {
            var r = demo.App.Instance.demo_Module1.Table1.GetOrAdd(key);
            r.Long2 += 1;
            //System.out.println("Add=" + key);
            return 0;
        }

        private int Remove(long key) {
            demo.App.Instance.demo_Module1.Table1.Remove(key);
            return 0;
        }
    }
}