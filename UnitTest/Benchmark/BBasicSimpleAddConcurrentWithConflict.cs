using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Benchmark
{
    [TestClass]
    public class BBasicSimpleAddConcurrentWithConflict
    {
        public const int AddCount = 1_000_000;
        [TestMethod]
        public void testBenchmark() {
            demo.App.Instance.Start();
            try {
                demo.App.Instance.Zeze.NewProcedure(Remove, "remove").Call();
                var tasks = new List<Task>(AddCount);
                Console.WriteLine("benchmark start...");
                var b = new Zeze.Util.Benchmark();
                for (int i = 0; i < AddCount; ++i) {
                    tasks.Add(Zeze.Util.Mission.Run(demo.App.Instance.Zeze.NewProcedure(Add, "Add")));
                }
                b.Report(this.GetType().FullName, AddCount);
                foreach (var task in tasks) {
                    task.Wait();
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

        private long Check() {
            var r = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1L);
            Assert.AreEqual(r.Long2, AddCount);
            //System.out.println(r.getLong2());
            return 0;
        }

        private long Add() {
            var r = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1L);
            r.Long2 += 1;
            return 0;
        }

        private long Remove() {
            demo.App.Instance.demo_Module1.Table1.Remove(1L);
            return 0;
        }
    }
}