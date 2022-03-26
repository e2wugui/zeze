using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Services;
using Zeze.Beans.TestRocks;

namespace UnitTest.Zeze.RocksRaft
{
    [TestClass]
    public class TestLichenghua
    {
        [TestMethod]
        public void TestGo()
        {
            using var test = new TestRocks();
            var leader = test.GetLeader(null);
            Assert.AreEqual(0, leader.NewProcedure(() =>
            {
                var table = leader.GetTableTemplate("tRocks").OpenTable<int, Value>(0);
                table.Remove(1);
                return 0;
            }).CallSynchronously());

            Assert.AreEqual(0, leader.NewProcedure(() =>
            {
                var table = leader.GetTableTemplate("tRocks").OpenTable<int, Value>(0);
                var value = table.GetOrAdd(1);
                value.Int = 1;
                leader.AtomicLongIncrementAndGet(0);
                leader.AtomicLongIncrementAndGet(0);
                return 0;
            }).CallSynchronously());

            Assert.AreEqual(0, leader.NewProcedure(() =>
            {
                var table = leader.GetTableTemplate("tRocks").OpenTable<int, Value>(0);
                var value = table.GetOrAdd(1);
                Assert.AreEqual(1, value.Int);
                Assert.AreEqual(2, leader.AtomicLongGet(0));
                return 0;
            }).CallSynchronously());

            leader.Raft.Server.Stop(); // 停止Leader的网络
            leader = test.GetLeader(leader); // 得到新的Leader，传入参数是确保得到新的。

            Assert.AreEqual(0, leader.NewProcedure(() =>
            {
                var table = leader.GetTableTemplate("tRocks").OpenTable<int, Value>(0);
                var value = table.GetOrAdd(1);
                Assert.AreEqual(1, value.Int);
                Assert.AreEqual(2, leader.AtomicLongGet(0));
                return 0;
            }).CallSynchronously());
        }
    }
}
