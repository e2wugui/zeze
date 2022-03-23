using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Util;

namespace UnitTest.Zeze.Util
{
    [TestClass]
    public class TestPersistentAtomicLong
    {
        [TestMethod]
        public void TestConcurrent()
        {
            var p1 = PersistentAtomicLong.GetOrAdd("TestPersistentAtomicLong", 10);
            var p2 = PersistentAtomicLong.GetOrAdd("TestPersistentAtomicLong", 10);
            var jobs = new Task[2];
            jobs[0] = Task.Run(() => Alloc(p1));
            jobs[1] = Task.Run(() => Alloc(p2));
            Task.WaitAll(jobs);
        }

        ConcurrentDictionary<long, long> allocs = new ConcurrentDictionary<long, long>();

        private void Alloc(PersistentAtomicLong p)
        {
            try
            {
                for (int i = 0; i < 1000; ++i)
                {
                    var n = p.Next();
                    Assert.IsTrue(allocs.TryAdd(n, n));
                }
            }
            catch (Exception ex)
            {
                Assert.Fail(ex.ToString());
            }
        }
    }
}
